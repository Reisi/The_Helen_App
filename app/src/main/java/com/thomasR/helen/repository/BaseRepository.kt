package com.thomasR.helen.repository

import com.thomasR.helen.data.BaseData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BaseRepository {
    private val _data = MutableStateFlow(BaseData())
    internal val data = _data.asStateFlow()

    init {
        // TODO: load from storage and apply
    }

    fun changeTheme(newTheme: Boolean?) {
        _data.value = _data.value.copy(useDarkTheme = newTheme)
    }
}