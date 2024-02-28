package com.thomasR.helen.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(device: Device)

    @Update
    fun update(device: Device)

    @Delete
    fun delete(device: Device)

    @Query("SELECT * FROM device")
    fun loadAll(): Flow<List<Device>>
}