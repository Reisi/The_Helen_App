package com.thomasR.helen.data

import com.thomasR.helen.repository.HelenRepository

data class MainData(
    val devices: List<HelenRepository> = emptyList()
)
