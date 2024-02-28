package com.thomasR.helen.repository

import androidx.compose.ui.input.key.Key.Companion.U
import com.thomasR.helen.profile.uart.data.UARTCmd
import com.thomasR.helen.profile.uart.data.UARTEvent
import com.thomasR.helen.profile.uart.data.UARTEventHandled
import com.thomasR.helen.profile.uart.data.UARTReceive
import com.thomasR.helen.profile.uart.data.UARTSend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.replay
import kotlinx.coroutines.flow.shareIn
import no.nordicsemi.android.common.core.simpleSharedFlow

class UartRepository {
    private val _events = MutableSharedFlow<UARTEvent>(25)
    internal val events = _events.asSharedFlow()

    init {
        _events.tryEmit(UARTEventHandled)
    }

    private val _command = simpleSharedFlow<UARTCmd>()
    internal val command = _command.asSharedFlow()

    fun onUartMessageReceived(msg: String) {
        _events.tryEmit(UARTReceive(msg))
    }

    fun clearEvent() {
        _events.tryEmit(UARTEventHandled)
    }

    fun sendMessage(msg: String) {
        _command.tryEmit(UARTSend(msg))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _events.resetReplayCache()
    }
}