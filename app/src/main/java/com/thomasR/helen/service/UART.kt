package com.thomasR.helen.service

import android.annotation.SuppressLint
import android.util.Log
import com.thomasR.helen.profile.uart.data.UARTSend
import com.thomasR.helen.repository.UartRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattServices
import no.nordicsemi.android.kotlin.ble.core.data.util.DataByteArray
import no.nordicsemi.android.kotlin.ble.core.errors.DeviceDisconnectedException
import java.util.UUID

private val UART_SERVICE_UUID: UUID = UUID.fromString("6e400001-B5A3-F393-E0A9-E50E24DCCA9E")
private val UART_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-B5A3-F393-E0A9-E50E24DCCA9E")
private val UART_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-B5A3-F393-E0A9-E50E24DCCA9E")

@SuppressLint("MissingPermission")
class UART(
    private val repository: UartRepository,
    private val services: ClientBleGattServices,
    private val scope: CoroutineScope
) {
    suspend fun configureGatt() {
        val uartService = services.findService(UART_SERVICE_UUID)
        val rx = uartService?.findCharacteristic(UART_RX_CHARACTERISTIC_UUID)
        val tx = uartService?.findCharacteristic(UART_TX_CHARACTERISTIC_UUID)

        if (rx == null || tx == null) {
            throw Exception("uart service not available")
        }

        repository.command
            .onEach {
                if (it is UARTSend) {
                    // TODO split message into several messages if longer than mtu
                    rx.write(DataByteArray(it.msg.encodeToByteArray()))
                }
            }
            .launchIn(scope)

        try {
            tx.getNotifications()
                .mapNotNull { String(it.value) }
                .onEach {
                    repository.onUartMessageReceived(it)
                    Log.d("service", it)
                }
                .launchIn(scope)
        } catch (e: DeviceDisconnectedException) {
            Log.e("UART", e.message)
        }
    }
}