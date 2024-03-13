package com.thomasR.helen.repository

import android.os.CountDownTimer
import com.thomasR.helen.profile.helenProject.data.ControlPointEvent
import com.thomasR.helen.profile.helenProject.data.EnableControlPoint
import com.thomasR.helen.profile.helenProject.data.FactoryReset
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSEventHandled
import com.thomasR.helen.profile.helenProject.data.HPSCmd
import com.thomasR.helen.profile.helenProject.data.HPSControlPointIndication
import com.thomasR.helen.profile.helenProject.data.HPSData
import com.thomasR.helen.profile.helenProject.data.HPSEvent
import com.thomasR.helen.profile.helenProject.data.HPSFeatureData
import com.thomasR.helen.profile.helenProject.data.HPSMeasurementData
import com.thomasR.helen.profile.helenProject.data.HPSModeConfig
import com.thomasR.helen.profile.helenProject.data.OverrideMode
import com.thomasR.helen.profile.helenProject.data.ReadModes
import com.thomasR.helen.profile.helenProject.data.RequestMode
import com.thomasR.helen.profile.helenProject.data.RequestSearch
import com.thomasR.helen.profile.helenProject.data.SetMode
import com.thomasR.helen.profile.helenProject.data.WriteModes
import com.thomasR.helen.profile.helenProject.data.WriteModesResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.common.core.simpleSharedFlow
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule

class HPSRepository(init: HPSData = HPSData()) {

    private val _data = MutableStateFlow(init)
    internal val data = _data.asStateFlow()

    private val _events = MutableStateFlow<HPSEvent>(HPSEventHandled)
    internal val events = _events.asStateFlow()

    private val _command = simpleSharedFlow<HPSCmd>()
    internal val command = _command.asSharedFlow()

    /*private val measurementResetTimer = object : CountDownTimer(5000, 5000) {
        override fun onTick(millisUntilFinished: Long) { }
        override fun onFinish() { _data.update { it.copy(measurement = null) }  }
    }*/

    private var resetTimerTask: TimerTask? = null

    fun onHPSMeasurementDataReceived(measData: HPSMeasurementData?) {
        _data.update { it.copy(measurement = measData) }
        resetTimerTask?.cancel()
        resetTimerTask = Timer().schedule(5000) {
            resetTimerTask = null
            _data.update { it.copy(measurement = null) }
        }
        //measurementResetTimer.start()
    }

    fun onHPSFeatureRead(featureData: HPSFeatureData?) {
        _data.update { it.copy(feature = featureData) }
    }

    fun onHPSModeConfigRead(modeConfig: List<List<HPSModeConfig>>?) {
        _data.update { it.copy(modes = modeConfig) }
    }

    fun onControlPointEnableChanged(enabled: Boolean) {
        _data.update { it.copy(isControlPointEnabled = enabled) }
    }

    fun onHPSControlPointIndicationReceived(indication: HPSControlPointIndication?) {
        if (indication != null) {
            _events.value = ControlPointEvent(indication.requestOpCode, indication.responseValue)
        }
    }

    fun onWriteConfigResponseReceived(success: Boolean) {
        _events.value = WriteModesResponse(success)
    }

    fun clearEvent() {
        _events.value = HPSEventHandled
    }

    fun readModes() {
        _command.tryEmit(ReadModes)
    }

    fun writeModes(modes: List<List<HPSModeConfig>>) {
        _command.tryEmit(WriteModes(modes))
    }

    fun enableControlPoint(enable: Boolean) {
        _command.tryEmit(EnableControlPoint(enable))
    }

    fun requestCurrentMode() {
        _command.tryEmit(RequestMode)
    }

    fun setMode(modeNo: Int) {
        _command.tryEmit(SetMode(modeNo))
    }

    fun requestSearch() {
        _command.tryEmit(RequestSearch)
    }

    fun factoryReset() {
        _command.tryEmit((FactoryReset))
    }

    fun overrideMode(channelConfig: List<HPSChannelConfig>) {
        _command.tryEmit(OverrideMode(channelConfig))
    }

    fun changeMode(mode: Int, config: HPSModeConfig) {
        if (_data.value.modes == null) return

        _data.update {
            val old = it.modes!!
            val new: MutableList<MutableList<HPSModeConfig>> = emptyList<MutableList<HPSModeConfig>>().toMutableList()
            var modeNo: Int? = mode

            // make a copy of the old list and replace the selected mode with the given configuration
            for (groupIndex in old.indices) {
                new.add(old[groupIndex].toMutableList())
                // running through all modes in this group, clearing flags if necessary and replace
                // mode if it is in this group
                for (modeIndex in new.last().indices) {
                    var newHelenConfig = new.last()[modeIndex].helen.copy()
                    // clear preferred flag if changed mode is new preferred one
                    if (config.helen.preferred)
                        newHelenConfig = newHelenConfig.copy(preferred = false)
                    // clear temporary flag if changed mode is new temporary mode
                    if (config.helen.temporary)
                        newHelenConfig = newHelenConfig.copy(temporary = false)
                    // clear off mode flag if changed mode is new off mode
                    if (config.helen.off)
                        newHelenConfig = newHelenConfig.copy(off = false)

                    // replace mode if it is the changed one
                    if (modeNo == modeIndex) {
                        new.last()[modeIndex] = config
                        modeNo = null   // to prevent falsely updating mode in next group
                    }
                    // otherwise copy old one with modified helen config
                    else {
                        new.last()[modeIndex] = new.last()[modeIndex].copy(helen = newHelenConfig)
                    }
                }
                if (modeNo != null)
                    modeNo -= old[groupIndex].size      // mode in higher group, reduce by group size

                /*if (modeNo != null) {                       // mode already updated
                    if (modeNo < old[groupIndex].size) {    // mode is in this group
                        new[groupIndex][modeNo] = config    // replace
                        modeNo = null                       // clear modeNo to not replace any other modes
                    } else
                        modeNo = modeNo - old[groupIndex].size      // mode in higher group, reduce by group size
                }*/
            }

            it.copy(modes = new)
        }
    }

    fun changeGroups(newGroups: List<List<HPSModeConfig>>) {
        if (_data.value.modes == null) return
        _data.update {it.copy(modes = newGroups) }
    }

    fun clear() {
        _data.update { HPSData() }
    }
}