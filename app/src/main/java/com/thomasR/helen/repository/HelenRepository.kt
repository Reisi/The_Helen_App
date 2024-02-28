package com.thomasR.helen.repository

import android.content.Context
import android.util.Log
import com.thomasR.helen.profile.helenProject.data.ControlPointEvent
import com.thomasR.helen.profile.helenProject.data.HPSEventHandled
import com.thomasR.helen.profile.helenProject.data.HPSCmd
import com.thomasR.helen.profile.helenProject.data.HPSControlPointIndication
import com.thomasR.helen.profile.helenProject.data.HPSEvent
import com.thomasR.helen.profile.helenProject.data.HPSFeatureData
import com.thomasR.helen.profile.helenProject.data.HPSMeasurementData
import com.thomasR.helen.profile.helenProject.data.HPSModeConfig
import com.thomasR.helen.data.HelenData
import com.thomasR.helen.profile.deviceInformation.data.DeviceInformationData
import com.thomasR.helen.profile.genericAccess.data.GACmd
import com.thomasR.helen.profile.genericAccess.data.GAEvent
import com.thomasR.helen.profile.genericAccess.data.GAEventHandled
import com.thomasR.helen.profile.genericAccess.data.GANameChangeResponse
import com.thomasR.helen.profile.genericAccess.data.GANameChangeResponseCode
import com.thomasR.helen.profile.genericAccess.data.GASetName
import com.thomasR.helen.profile.helenProject.data.FactoryReset
import com.thomasR.helen.profile.helenProject.data.HPSData
import com.thomasR.helen.profile.helenProject.data.ReadModes
import com.thomasR.helen.profile.helenProject.data.RequestMode
import com.thomasR.helen.profile.helenProject.data.RequestSearch
import com.thomasR.helen.profile.helenProject.data.SetMode
import com.thomasR.helen.profile.helenProject.data.WriteModes
import com.thomasR.helen.profile.kd2.data.KD2Data
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.common.core.simpleSharedFlow
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.core.MockServerDevice
import no.nordicsemi.android.kotlin.ble.core.ServerDevice
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState

class HelenRepository(
    private val address: String,
    private val name: String? = null,
    internal val dis: DISRepository = DISRepository(),
    internal val hps: HPSRepository = HPSRepository(),
    internal val dfu: DfuRepository = DfuRepository(address),
    internal val kd2: KD2Repository = KD2Repository(),
    internal val uart: UartRepository = UartRepository()
) {
    private val _data = MutableStateFlow(HelenData())
    internal val data = _data.asStateFlow()

    private val _events = MutableStateFlow<GAEvent>(GAEventHandled)
    internal val events = _events.asStateFlow()

    private val _nameChange = simpleSharedFlow<GACmd>()
    internal val nameChange = _nameChange.asSharedFlow()

    init {
        _data.value = _data.value.copy(name = name, address = address)
    }

    // constructor for adding devices from persistent memory
    constructor(
        address: String,
        name: String?,
        model: String?,
        setupProfile: Int?,
        ignoreWrongSetup: Boolean,
    ) : this(
        address = address,
        name = name,
        dis = DISRepository(DeviceInformationData(model = model))
    ) {
        _data.value = _data.value.copy(
            connectionState = GattConnectionState.STATE_DISCONNECTED,
            isHelenProjectSupported = true,
            setupProfile = setupProfile,
            ignoreWrongSetup = ignoreWrongSetup
        )
    }

    // constructor for dummy devices
    constructor(
        device: HelenData,
        disData: DeviceInformationData,
        hpsData: HPSData,
        kd2Data: KD2Data = KD2Data()
    ) : this(
        address = device.address!!,
        name = device.name,
        dis = DISRepository(disData),
        hps = HPSRepository(hpsData),
        kd2 = KD2Repository(kd2Data)
    ) {
        _data.value = device
    }

    fun onDeviceNameReceived(name: String) {
        _data.value = _data.value.copy(name = name)
    }

    fun onDeviceNameChanged(responseCode: GANameChangeResponseCode) {
        _events.tryEmit(GANameChangeResponse(responseCode))
    }

    fun clearEvent() {
        _events.tryEmit(GAEventHandled)
    }

    fun changeName(name: String) {
        _nameChange.tryEmit(GASetName(name))
        _data.value = _data.value.copy(name = name)
    }

    /*fun clearClient() {
        _data.value = _data.value.copy(client = null)
    }*/

    fun setClient(client: ClientBleGatt, scope: CoroutineScope) {
        _data.value = _data.value.copy(client = client)
        client.connectionStateWithStatus
            .filterNotNull()
            .onEach {
                // update connection state on change
                _data.value = _data.value.copy(connectionState = it.state)
                Log.d("HelenRepository", "connectionState ${it.state}")
                // and clear client when disconnected
                if (it.state == GattConnectionState.STATE_DISCONNECTED)
                    _data.value = _data.value.copy(client = null)
            }
            .launchIn(scope)
    }

    fun clearConnectionState() {
        _data.update { it.copy(connectionState = null) }
        Log.d("HelenRepository", "connectionState null")
    }

    fun setConnectionState(state: GattConnectionState) {
        _data.update { it.copy(connectionState = state) }
        Log.d("HelenRepository", "connectionState $state")
    }

    fun setDfuSupported(supported: Boolean) {
        _data.value = _data.value.copy(isDfuSupported = supported)
    }

    fun setHelenProjectSupported(supported: Boolean) {
        _data.value = _data.value.copy(isHelenProjectSupported = supported)
    }

    fun setKd2Supported(supported: Boolean) {
        _data.value = _data.value.copy(isKd2Supported = supported)
    }

    fun setUartSupported(supported: Boolean) {
        _data.value = _data.value.copy(isUartSupported = supported)
    }

    fun setNameChangeSupported(supported: Boolean) {
        _data.value = _data.value.copy(nameChangeSupported = supported)
    }

    fun selectSetupProfile(profile: Int?) {
        _data.value = _data.value.copy(setupProfile = profile, ignoreWrongSetup = false)
    }

    fun setIgnoreSetup(ignore: Boolean) {
        _data.value = _data.value.copy(ignoreWrongSetup = ignore)
    }
}