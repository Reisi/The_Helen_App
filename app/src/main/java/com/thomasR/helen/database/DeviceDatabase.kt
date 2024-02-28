package com.thomasR.helen.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Device::class], version = 2)
abstract class DeviceDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile private var INSTANCE: DeviceDatabase? = null

        fun getInstance(context: Context): DeviceDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                DeviceDatabase::class.java, "DeviceDB.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}