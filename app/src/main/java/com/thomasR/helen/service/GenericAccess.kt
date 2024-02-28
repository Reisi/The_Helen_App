package com.thomasR.helen.service

import android.annotation.SuppressLint
import com.thomasR.helen.profile.genericAccess.data.GANameChangeResponseCode
import com.thomasR.helen.profile.genericAccess.data.GASetName
import com.thomasR.helen.repository.HelenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.common.core.DataByteArray
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattServices
import no.nordicsemi.android.kotlin.ble.core.data.BleGattProperty
import java.util.UUID

val GA_SERVICE_UUID: UUID = UUID.fromString("00001800-0000-1000-8000-00805F9B34FB")
private val GA_DEVICENAME_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A00-0000-1000-8000-00805F9B34FB")
val GA_DEVICE_NAME_MAX_LENGTH = 31

@SuppressLint("MissingPermission")
class GenericAccess(
    private val repository: HelenRepository,
    private val services: ClientBleGattServices,
    private val scope: CoroutineScope
) {
    suspend fun configureGatt() {
        val gaService = services.findService(GA_SERVICE_UUID)!!
        val deviceName = gaService.findCharacteristic(GA_DEVICENAME_CHARACTERISTIC_UUID)!!

        if (deviceName.properties.contains(BleGattProperty.PROPERTY_WRITE))
            repository.setNameChangeSupported(true)

        repository.onDeviceNameReceived(String(deviceName.read().value))

        repository.nameChange
            .onEach {
                when(it) {
                    is GASetName -> {
                        if (repository.data.value.nameChangeSupported != true)
                            repository.onDeviceNameChanged(GANameChangeResponseCode.NOT_SUPPORTED)
                        else if (it.name.length > GA_DEVICE_NAME_MAX_LENGTH)
                            repository.onDeviceNameChanged(GANameChangeResponseCode.INVALID_LENGHT)
                        else {
                            try {
                                deviceName.write(DataByteArray(it.name.encodeToByteArray()))
                                repository.onDeviceNameChanged(GANameChangeResponseCode.SUCCESS)
                            } catch (_: Exception) {
                                repository.onDeviceNameChanged(GANameChangeResponseCode.FAILED)
                            }
                        }
                    }
                }
            }
            .launchIn(scope)
    }
}