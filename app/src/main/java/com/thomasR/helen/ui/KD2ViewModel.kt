package com.thomasR.helen.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thomasR.helen.profile.kd2.data.KD2ChannelSetup
import com.thomasR.helen.profile.kd2.data.KD2ComPinConfig
import com.thomasR.helen.repository.HelenRepository
import com.thomasR.helen.repository.KD2Repository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KD2ViewModel(private val repository: HelenRepository) : ViewModel() {
    internal val data = repository.kd2.data
    internal val deviceData = repository.data
    internal val response = repository.kd2.controlPointCommonResponse

    fun clearControlPointResponse() {
        repository.kd2.clearControlPointResponse()
    }

    fun updateChannelConfig(channel: Int, config: KD2ChannelSetup) {
        repository.kd2.updateChannelConfig(channel, config)
    }

    fun reloadChannelConfig(channel: Int) {
        repository.kd2.requestChannelConfig(channel)
    }

    fun uploadChannelConfig(channel: Int, config: KD2ChannelSetup) {
        repository.kd2.setChannelConfig(channel, config)
    }

    fun updateComPinUsage(newUsage: KD2ComPinConfig) {
        repository.kd2.updateComPinConfig(newUsage)
    }

    fun reloadComPinUsage() {
        repository.kd2.requestComPinConfig()
    }

    fun uploadComPinUsage(config: KD2ComPinConfig) {
        repository.kd2.setComPinConfig(config)
    }

    fun startImuCalibration() {
        repository.kd2.startImuCalibration()
    }

    fun setIgnoreWrongSetup(ignore: Boolean) {
        repository.setIgnoreSetup(ignore)
    }

    companion object {
        class Factory(private val repository: HelenRepository): ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return KD2ViewModel(repository) as T
            }
        }
    }
}