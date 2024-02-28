package com.thomasR.helen.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thomasR.helen.data.PermissionAlertData
import com.thomasR.helen.permission.BluetoothPermission
import com.thomasR.helen.permission.BluetoothState
import com.thomasR.helen.permission.LocationPermission
import com.thomasR.helen.permission.LocationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class PermissionViewModel(application: Application) : AndroidViewModel(application) {
    val bluetoothState = BluetoothState(application)
    val bluetoothEnabledState = bluetoothState.bleEnabledState().stateIn(
        viewModelScope, SharingStarted.Lazily, bluetoothState.isBleEnabled
    )

    val locationState = LocationState(application)
    val locationEnabledState = locationState.locationEnabledState().stateIn(
        viewModelScope, SharingStarted.Lazily, locationState.isLocationEnabled
    )

    val bluetoothPermission = BluetoothPermission()
    val locationPermission = LocationPermission()

    private val _permissionAlertState = MutableStateFlow(PermissionAlertData())
    val permissionAlertState: StateFlow<PermissionAlertData> = _permissionAlertState.asStateFlow()

    fun setAlertData(
        alertText: String,
        confirmText: String,
        onConfirmAction: () -> Unit
    ) {
        _permissionAlertState.value = _permissionAlertState.value.copy(
            showAlert = true, alertText = alertText,
            confirmText = confirmText, onConfirmAction = onConfirmAction)
    }

    fun clearAlert() {
        _permissionAlertState.value = PermissionAlertData()
    }
}