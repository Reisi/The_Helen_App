package com.thomasR.helen.permission

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class BluetoothState(
    private val context: Context
) {
    val isBleAvailable: Boolean
        get() = context.packageManager.hasSystemFeature((PackageManager.FEATURE_BLUETOOTH_LE))

    val isBleEnabled: Boolean
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled

    fun bleEnabledState() = callbackFlow<Boolean> {
        trySend(isBleEnabled)

        val onStateChange = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                trySend(isBleEnabled)
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, onStateChange, filter, ContextCompat.RECEIVER_EXPORTED)
        awaitClose {
            context.unregisterReceiver(onStateChange)
        }
    }
}