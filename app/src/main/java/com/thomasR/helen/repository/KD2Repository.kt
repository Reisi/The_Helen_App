package com.thomasR.helen.repository

import android.util.Log
import com.thomasR.helen.profile.kd2.data.KD2ChannelSetup
import com.thomasR.helen.profile.kd2.data.KD2ComPinConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointCommand
import com.thomasR.helen.profile.kd2.data.KD2ControlPointCommonResponse
import com.thomasR.helen.profile.kd2.data.KD2ControlPointIndication
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestChannelConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestComPinConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestExternalComp
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestInternalComp
import com.thomasR.helen.profile.kd2.data.KD2ControlPointResponseHandled
import com.thomasR.helen.profile.kd2.data.KD2ControlPointSetChannelConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointSetComPinConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointSetExternalComp
import com.thomasR.helen.profile.kd2.data.KD2ControlPointSetInternalComp
import com.thomasR.helen.profile.kd2.data.KD2Data
import com.thomasR.helen.profile.kd2.data.KD2ExternalComp
import com.thomasR.helen.profile.kd2.data.KD2Feature
import com.thomasR.helen.profile.kd2.data.KD2InternalComp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.common.core.simpleSharedFlow

class KD2Repository(init: KD2Data = KD2Data()) {

    private val _data = MutableStateFlow(init)
    internal val data = _data.asStateFlow()

    private val _expertMode = MutableStateFlow(false)
    internal val expertMode = _expertMode.asStateFlow()

    private val _command = simpleSharedFlow<KD2ControlPointCommand>()
    internal val command = _command.asSharedFlow()

    private val _controlPointCommonResponse = MutableStateFlow<KD2ControlPointIndication>(
        KD2ControlPointResponseHandled
    )
    internal val controlPointCommonResponse = _controlPointCommonResponse.asStateFlow()

    fun clearControlPointResponse() {
        _controlPointCommonResponse.value = KD2ControlPointResponseHandled
    }

    fun onControlPointCommonResponse(response: KD2ControlPointCommonResponse) {
        _controlPointCommonResponse.value = response
        Log.d("KD2Rep", "Control Point Response oc: ${response.opCode}, value: ${response.responseValue}")
    }

    fun onFeatureRead(feature: KD2Feature) {
        _data.update{ it.copy(feature = feature) }
    }

    fun updateChannelConfig(channel: Int, config: KD2ChannelSetup) {
        _data.update {
            val channelConfigs = it.controlPointData.channelConfigs.toMutableList()
            if (channel < channelConfigs.size)      // channel already exist -> override
                channelConfigs[channel] = config
            else {                                  // channel does not exist
                // add "empty" channel configurations for the case that higher channel
                // configurations are received first
                while (channelConfigs.size < channel) {
                    channelConfigs.add(KD2ChannelSetup())
                }
                channelConfigs.add(config)
            }
            val controlPointData = it.controlPointData.copy(channelConfigs = channelConfigs)
            it.copy(controlPointData = controlPointData)
        }
    }

    fun updateComPinConfig(config: KD2ComPinConfig) {
        _data.update {
            val controlPointData = it.controlPointData.copy(comPinConfig = config)
            it.copy(controlPointData = controlPointData)
        }
    }

    fun updateInternalComp(comp: KD2InternalComp) {
        _data.update {
            val controlPointData = it.controlPointData.copy(internalComp = comp)
            it.copy(controlPointData = controlPointData)
        }
    }

    fun updateExternalComp(comp: KD2ExternalComp) {
        _data.update {
            val controlPointData = it.controlPointData.copy(externalComp = comp)
            it.copy(controlPointData = controlPointData)
        }
    }

    fun requestChannelConfig(channel: Int) {
        _command.tryEmit(KD2ControlPointRequestChannelConfig(channel))
    }

    fun setChannelConfig(channel: Int, config: KD2ChannelSetup) {
        _command.tryEmit(KD2ControlPointSetChannelConfig(channel, config))
    }

    fun requestComPinConfig() {
        _command.tryEmit(KD2ControlPointRequestComPinConfig)
    }

    fun setComPinConfig(config: KD2ComPinConfig) {
        _command.tryEmit(KD2ControlPointSetComPinConfig(config))
    }

    fun requestInternalComp() {
        _command.tryEmit(KD2ControlPointRequestInternalComp)
    }

    fun setInternalComp(comp: KD2InternalComp) {
        _command.tryEmit(KD2ControlPointSetInternalComp(comp))
    }

    fun requestExternalComp() {
        _command.tryEmit(KD2ControlPointRequestExternalComp)
    }

    fun retExternalComp(comp: KD2ExternalComp) {
        _command.tryEmit(KD2ControlPointSetExternalComp(comp))
    }

    fun clear() {
        _data.update { KD2Data() }
    }
}