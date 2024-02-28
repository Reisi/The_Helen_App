package com.thomasR.helen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thomasR.helen.repository.UartRepository

class UartViewModel(private val repository: UartRepository) : ViewModel() {
    internal val receivedMessage = repository.events

    fun messageHandled() {
        repository.clearEvent()
    }

    fun sendMessage(message: String) {
        repository.sendMessage(message)
    }

    companion object {
        class Factory(private val repository: UartRepository): ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UartViewModel(repository) as T
            }
        }
    }
}