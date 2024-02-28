package com.thomasR.helen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thomasR.helen.repository.BaseRepository
import com.thomasR.helen.repository.Theme
import com.thomasR.helen.repository.UserPreferences
import com.thomasR.helen.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainBaseViewModel(
    private val repository: UserPreferencesRepository
): ViewModel() {

    private  val _data = MutableStateFlow<UserPreferences?>(null)
    internal val data = _data.asStateFlow()

    init {
        viewModelScope.launch {
            _data.value = repository.fetchInitialPreferences()
        }

        repository.userPreferencesFlow
            .onEach { _data.value = it }
            .launchIn(viewModelScope)
    }

    fun changeTheme(newTheme: Theme) {
        viewModelScope.launch { repository.setTheme(newTheme) }
    }

    companion object {
        class Factory(private val repository: UserPreferencesRepository): ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return MainBaseViewModel(repository) as T
            }
        }
    }
}