package com.thomasR.helen.ui

import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thomasR.helen.repository.MainRepository
import com.thomasR.helen.service.HPS_SERVICE_UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import no.nordicsemi.android.kotlin.ble.core.RealServerDevice
import no.nordicsemi.android.kotlin.ble.core.ServerDevice
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScanMode
import no.nordicsemi.android.kotlin.ble.core.scanner.BleScannerSettings
import no.nordicsemi.android.kotlin.ble.scanner.BleScanner
import java.util.UUID

//@HiltViewModel
@SuppressLint("MissingPermission")
class SearchViewModel(
    private val application: Application,
    private var repository: MainRepository,
    private val connect: (String, String?) -> Unit,
    private val disconnect: (String) -> Unit
) : ViewModel(), DefaultLifecycleObserver {

    val devices = repository.devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private var currentJob: Job? = null

    fun connectDevice(address: String, name: String?) {
        connect(address, name)
    }

    fun disconnectDevice(address: String) {
        disconnect(address)
    }

    fun isDeviceBonded(address: String) : Boolean {
        return repository.isDeviceBonded(address)
    }

    fun removeDevice(address: String, keepBond: Boolean) {
        repository.removeDevice(address, keepBond)
    }

    fun startScanning() {
        //val HPS_SERVICE_UUID: ParcelUuid = ParcelUuid(UUID.fromString("4F770201-ED7D-11E4-840E-0002A5D5C51B"))
        //val HPS_SERVICE_UUID: UUID = UUID.fromString("4F770201-ED7D-11E4-840E-0002A5D5C51B")

        /*val btManger = application.getSystemService(Service.BLUETOOTH_SERVICE) as BluetoothManager
        val connectedDevices = btManger.getConnectedDevices(BluetoothProfile.GATT)
        for (device in connectedDevices) {
            connectDevice(RealServerDevice(device))
        }

        return*/

        Log.d("SearchViewModel", "mainRepository hash ${repository.hashCode()}")

        currentJob?.cancel()

        @SuppressLint("MissingPermission")
        currentJob = BleScanner(application).scan(settings = BleScannerSettings(BleScanMode.SCAN_MODE_LOW_LATENCY))
            .filter {
                it.data?.scanRecord?.serviceUuids?.contains(ParcelUuid(HPS_SERVICE_UUID)) == true
                //it.data?.scanRecord?.deviceName == "Helen"
            }
            .map {connect(it.device.address, it.device.name) }
            .onStart { _isScanning.value = true }
            .onCompletion { _isScanning.value = false }
            .launchIn(viewModelScope)

    }

    fun stopScanning() {
        currentJob?.cancel()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopScanning()
    }

    companion object {
        class Factory(
            private val repository: MainRepository,
            private val connect: (String, String?) -> Unit,
            private val disconnect: (String) -> Unit
        ): ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                return SearchViewModel(application, repository, connect, disconnect) as T
            }
        }
    }
}