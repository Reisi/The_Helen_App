package com.thomasR.helen.data

import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState

data class HelenData(
    val name: String? = null,
    val address: String? = null,
    val client: ClientBleGatt? = null,
    val connectionState: GattConnectionState? = null,   // TODO necessary, is already present in client
    val isDfuSupported: Boolean? = null,
    val isHelenProjectSupported: Boolean? = null,
    val isKd2Supported: Boolean? = null,
    val isUartSupported: Boolean? = null,
    val nameChangeSupported: Boolean? = null,

    val setupProfile: Int? = null,
    val ignoreWrongSetup: Boolean = false
)