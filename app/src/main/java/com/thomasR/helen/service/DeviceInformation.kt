package com.thomasR.helen.service

import android.annotation.SuppressLint
import com.thomasR.helen.repository.DISRepository
import com.thomasR.helen.repository.HelenRepository
import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattServices
import java.util.UUID

val DEVICE_INFORMATION_SERVICE_UUID: UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")
private val DIS_MODEL_NUMBER_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB")
private val DIS_HARDWARE_REV_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB")
private val DIS_FIRMWARE_REV_CHARACTERISTIC_UUID: UUID = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB")

@SuppressLint("MissingPermission")
class DeviceInformation(
    private val repository: DISRepository,
    private val services: ClientBleGattServices,
    private val scope: CoroutineScope
)  {
    suspend fun configureGatt() {
        val service = services.findService(DEVICE_INFORMATION_SERVICE_UUID)
            ?: throw Exception("device information service not found")
        val modelNo = service.findCharacteristic(DIS_MODEL_NUMBER_CHARACTERISTIC_UUID)
        val hardwareRev = service.findCharacteristic(DIS_HARDWARE_REV_CHARACTERISTIC_UUID)
        val firmwareRev = service.findCharacteristic(DIS_FIRMWARE_REV_CHARACTERISTIC_UUID)

        if (modelNo != null)
            repository.onDeviceModelReceived(String(modelNo.read().value))

        if (hardwareRev != null)
            repository.onDeviceHardwareReceived(String(hardwareRev.read().value))

        if (firmwareRev != null)
            repository.onDeviceFirmwareReceived(String(firmwareRev.read().value))
    }
}