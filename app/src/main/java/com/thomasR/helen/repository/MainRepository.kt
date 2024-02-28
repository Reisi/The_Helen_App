package com.thomasR.helen.repository

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_NONE
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.things.bluetooth.BluetoothConnectionManager
import com.thomasR.helen.data.HelenData
import com.thomasR.helen.database.Device
import com.thomasR.helen.database.DeviceDao
import com.thomasR.helen.database.DeviceDatabase
import com.thomasR.helen.permission.BluetoothPermission
import com.thomasR.helen.profile.deviceInformation.data.DeviceInformationData
import com.thomasR.helen.profile.helenProject.data.HPSData
import com.thomasR.helen.profile.kd2.data.KD2Data
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState

class MainRepository(
    private val context: Context
    //private val scope: CoroutineScope
) {
    private val _devices = mutableStateListOf<HelenRepository>()
    internal val devices: List<HelenRepository> = this._devices

    private var deviceDao: DeviceDao

    init {
        /* TODO load and store devices */
        deviceDao = DeviceDatabase.getInstance(context).deviceDao()

        CoroutineScope(Job()).launch {
            deviceDao.loadAll().collect { storedDevices ->
                for (stored in storedDevices) {
                    // check if there is already a repository for this device (possible if a device
                    // is already connected at startup)
                    if (getDeviceIndex(stored.address) != null)
                        continue
                    // add device to repository
                    val repository = HelenRepository(
                        address = stored.address,
                        name = stored.name,
                        model = stored.model,
                        setupProfile = stored.setupProfile,
                        ignoreWrongSetup = stored.ignoreWrongSetup
                    )
                    _devices.add(repository)
                    onAddDevice(repository)
                }
                cancel()    // read only once, then cancel
            }
        }
    }

    private fun onAddDevice(repository: HelenRepository) {
        repository.data
            .onEach {
                // update the list if device data changes
                val copy = _devices.toList()
                _devices.clear()
                _devices.addAll(copy)

                // store device in persistent memory when disconnected
                if (/*it.client != null &&*/ it.connectionState == GattConnectionState.STATE_DISCONNECTED)
                    deviceDao.insert(
                        Device(
                            it.address!!,
                            it.name,
                            repository.dis.data.value.model,
                            it.setupProfile,
                            it.ignoreWrongSetup
                        )
                    )
            }
            .onCompletion { Log.d("MainRep", "no longer listening to repository changes") }
            .launchIn(CoroutineScope(Job()))
    }

    //private val _data = MutableSharedFlow<MainData>(1)
    //val data = _data.asSharedFlow()

    fun addDevice (address: String, name: String? = null, model: String? = null /*, scope: CoroutineScope*/) {
        if (getDeviceIndex(address) != null)
            throw Exception("device already added")

        val repository = HelenRepository(address, name)
        _devices.add(repository)
        onAddDevice(repository)
    }

    fun addDevice(device: HelenData, info: DeviceInformationData, hpsData: HPSData, kd2Data: KD2Data) {
        val repository = HelenRepository(device, info, hpsData, kd2Data)
        _devices.add(repository)
    }

    @SuppressLint("MissingPermission")
    fun isDeviceBonded(address: String) : Boolean {
        if (!BluetoothAdapter.checkBluetoothAddress(address))
            return false

        val btPermission = BluetoothPermission()
        if (btPermission.isRequired && !btPermission.isConnectGranted(context))
            throw Exception("no permission")

        val btManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        return (btManager.adapter.getRemoteDevice(address).bondState != BOND_NONE)
    }

    fun removeDevice(address: String, keepBond: Boolean) {
        for (i in _devices.indices) {
            if (_devices[i].data.value.address == address) {
                _devices.removeAt(i)
                Thread {
                    deviceDao.delete(Device(address))
                }.start()
                break  // there should not be more than one device with this address
            }
        }

        if (!keepBond && BluetoothAdapter.checkBluetoothAddress(address)) {
            val btPermission = BluetoothPermission()
            if (btPermission.isRequired && !btPermission.isConnectGranted(context))
                throw Exception("no permission")

            val btManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            btManager.adapter.getRemoteDevice(address).removeBond()
        }
    }

    private fun getDeviceIndex(address: String) : Int? {
        for (i in _devices.indices) {
            if (_devices[i].data.value.address == address)
                return i
        }
        return null
    }

    fun getDeviceRepository(address: String): HelenRepository? {
        val index = getDeviceIndex(address)
        return if (index == null) null else _devices[index]
    }

    private fun BluetoothDevice.removeBond() {
        try {
            javaClass.getMethod("removeBond").invoke(this)
        } catch (e: Exception) {
            Log.e("BluetoothDevice", "Removing bond has been failed. ${e.message}")
        }
        /*try {
            val method = this.javaClass.getMethod("removeBond")
            method.invoke(this) as Any
        } catch (e: Exception) {
            e.message?.let { Log.e("BluetoothDevice", it) }
        }*/
    }
}