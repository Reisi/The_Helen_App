package com.thomasR.helen.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Device(
    @PrimaryKey val address: String,
    val name: String? = null,
    val model: String? = null,
    val setupProfile: Int? = 0,//null,
    val ignoreWrongSetup: Boolean = false
)
