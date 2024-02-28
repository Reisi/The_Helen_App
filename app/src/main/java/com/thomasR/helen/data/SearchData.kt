package com.thomasR.helen.data

data class SearchData(
    val isSearchActive: Boolean = false,
    val connectedDevices: List<HelenData> = emptyList()
)
