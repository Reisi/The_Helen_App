package com.thomasR.helen.permission

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class LocationState(
    private val context: Context
) {
    val isLocationAvailable: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION)

    val isLocationEnabled:  Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val lm = context.getSystemService(LocationManager::class.java)
            LocationManagerCompat.isLocationEnabled(lm)
        } else false

    fun locationEnabledState() = callbackFlow<Boolean> {
        trySend(isLocationEnabled)

        val onStateChange = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                trySend(isLocationEnabled)
            }
        }
        val filter = IntentFilter().apply {
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(context, onStateChange, filter, ContextCompat.RECEIVER_EXPORTED)
        awaitClose {
            context.unregisterReceiver(onStateChange)
        }
    }
}