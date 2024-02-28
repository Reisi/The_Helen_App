package com.thomasR.helen.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class LocationPermission {
    val isRequired: Boolean
        get() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)

    fun isGranted(context: Context) : Boolean {
        return !isRequired ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowRational(context: Context): Boolean {
        return isRequired &&
                context.findActivity()
                    .shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
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