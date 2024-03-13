package com.thomasR.helen.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thomasR.helen.profile.dfu.data.Idle
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSModeConfig
import com.thomasR.helen.repository.HelenRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HelenViewModel(
    private val application: Application,
    private val repository: HelenRepository
) : ViewModel() {
    val data = repository.data
    val events = repository.events
    val disData = repository.dis.data
    val hpsData = repository.hps.data
    val hpsEvents = repository.hps.events
    val kd2data = repository.kd2.data
    val dfuProgress = repository.dfu.progress(application)
        .stateIn(viewModelScope, SharingStarted.Lazily, Idle)


    //var enabled = false

    fun reloadModes() {
        repository.hps.readModes()
    }

    fun writeModes(modes: List<List<HPSModeConfig>>) {
        repository.hps.writeModes(modes)
    }

    fun updateMode(mode: Int, config: HPSModeConfig) {
        repository.hps.changeMode(mode, config)
    }

    fun updateGroups(newGroups: List<List<HPSModeConfig>>) {
        repository.hps.changeGroups(newGroups)
    }

    fun enableControlPoint(enable: Boolean) {
        repository.hps.enableControlPoint(enable)
    }

    fun selectMode(mode: Int) {
        repository.hps.setMode(mode)
    }

    fun requestSearch() {
        repository.hps.requestSearch()
    }

    fun factoryReset() {
        repository.hps.factoryReset()
    }

    fun overrideMode(channelConfig: List<HPSChannelConfig>?) {
        if (channelConfig == null)
            repository.hps.setMode(255)
        else
            repository.hps.overrideMode(channelConfig)
    }

    fun clearProjectEvent() {
        repository.hps.clearEvent()
    }

    fun clearEvent() {
        repository.clearEvent()
    }

    fun changeName(newName: String) {
        repository.changeName(newName)
    }

    fun selectSetupProfile(profile: Int?) {
        repository.selectSetupProfile(profile)
    }

    fun isFirmwareUpdateAvailable() : String? {
        return if (disData.value.model == null || disData.value.hardwareRevision == null || disData.value.firmwareRevision == null)
            null
        else repository.dfu.isUpdateAvailable(
            context = application,
            model = disData.value.model!!,
            hardwareRev = disData.value.hardwareRevision!!,
            currentRev = disData.value.firmwareRevision!!
        )
    }

    fun getChangelog() : String? {
        return if (disData.value.model == null || disData.value.hardwareRevision == null)
            null
        else
            repository.dfu.getChangelog(
                context = application,
                model = disData.value.model!!,
                hardwareRev = disData.value.hardwareRevision!!
            )
    }

    fun startFirmwareUpdate() {
        repository.dfu.startUpdate(application, disData.value.model!!, disData.value.hardwareRevision!!)
    }

    companion object {
        class Factory(private val repository: HelenRepository): ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return HelenViewModel(application, repository) as T
            }
        }
    }
}