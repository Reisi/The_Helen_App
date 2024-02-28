package com.thomasR.helen.repository

import com.thomasR.helen.profile.deviceInformation.data.DeviceInformationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DISRepository(init: DeviceInformationData = DeviceInformationData()) {

    private val _data = MutableStateFlow(init)
    internal val data = _data.asStateFlow()

    fun onDeviceModelReceived(modelNo: String) {
        _data.update { it.copy(model = modelNo) }
    }

    fun onDeviceHardwareReceived(rev: String) {
        _data.update { it.copy(hardwareRevision = rev) }
    }

    fun onDeviceFirmwareReceived(rev: String) {
        _data.update { it.copy(firmwareRevision = rev) }
    }

    fun clear() {
        _data.update { DeviceInformationData(model = it.model) }
    }
}