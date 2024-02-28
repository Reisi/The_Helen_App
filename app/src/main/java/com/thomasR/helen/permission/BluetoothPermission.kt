package com.thomasR.helen.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BluetoothPermission {
    val isRequired: Boolean
        get() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    fun isScanGranted(context: Context) : Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isConnectGranted(context: Context) : Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun arePermissionsGranted(context: Context): Boolean {
        return isScanGranted(context) && isConnectGranted(context)
    }

    fun shouldShowRational(context: Context): Boolean {
        return isRequired &&
                context.findActivity()
                    .shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN)
    }

    private fun Context.findActivity(): Activity {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        throw IllegalStateException("no activity")
    }
}