package com.thomasR.helen.data

data class PermissionAlertData(
    val showAlert: Boolean = false,
    val alertText: String = "",
    val confirmText: String = "",
    val onConfirmAction: () -> Unit = {}
)
