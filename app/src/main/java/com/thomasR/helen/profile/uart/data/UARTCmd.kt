package com.thomasR.helen.profile.uart.data

sealed class UARTCmd

data class UARTSend(val msg: String) : UARTCmd()
