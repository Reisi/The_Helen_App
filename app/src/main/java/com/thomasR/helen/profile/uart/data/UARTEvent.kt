package com.thomasR.helen.profile.uart.data

sealed class UARTEvent

object UARTEventHandled : UARTEvent()

data class UARTReceive(val msg: String) : UARTEvent()
