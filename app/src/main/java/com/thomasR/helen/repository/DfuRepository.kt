package com.thomasR.helen.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.RawRes
import com.thomasR.helen.R
import com.thomasR.helen.profile.dfu.data.Completed
import com.thomasR.helen.profile.dfu.data.DfuProgress
import com.thomasR.helen.profile.dfu.data.Idle
import com.thomasR.helen.profile.dfu.data.InProgress
import com.thomasR.helen.profile.dfu.data.Starting
import com.thomasR.helen.service.DfuService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceController
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper

class DfuRepository (
    private val address: String
){
    private var dfuServiceController: DfuServiceController? = null

    fun progress(
        @ApplicationContext context: Context
    ) = callbackFlow {
        trySend(Idle)

        val dfuProgressListener = object : DfuProgressListenerAdapter() {
            override fun onEnablingDfuMode(deviceAddress: String) {
                if (isMyDevice(deviceAddress)) trySend(Starting)
            }

            override fun onProgressChanged(
                deviceAddress: String,
                percent: Int,
                speed: Float,
                avgSpeed: Float,
                currentPart: Int,
                partsTotal: Int
            ) {
                if (isMyDevice(deviceAddress)) trySend(InProgress(percent))
            }

            private val resetToIdleTimer = object : CountDownTimer(5000, 5000) {
                override fun onTick(millisUntilFinished: Long) { }
                override fun onFinish() { trySend(Idle)  }
            }

            override fun onDfuCompleted(deviceAddress: String) {
                if (isMyDevice(deviceAddress)) {
                    trySend(Completed)
                    resetToIdleTimer.start()
                }
            }
        }

        DfuServiceListenerHelper.registerProgressListener(context, dfuProgressListener)

        awaitClose{
            DfuServiceListenerHelper.unregisterProgressListener(context, dfuProgressListener)
        }
    }

    private fun Resources.getRawTextFile(@RawRes id: Int) =
        openRawResource(id).bufferedReader().use {it.readText() }

    @SuppressLint("DiscouragedApi")
    fun getChangelog(
        @ApplicationContext context: Context,
        model: String,
        hardwareRev: String,
    ) : String? {
        val resString = model.lowercase() + "_" + hardwareRev.replace(".", "")
        val changelog = context.resources.getIdentifier(resString + "_changelog", "raw", context.packageName)

        return if (changelog == 0)
            null
        else
            context.resources.getRawTextFile(changelog)
    }

    @SuppressLint("DiscouragedApi")
    fun isUpdateAvailable(
        @ApplicationContext context: Context,
        model: String,
        hardwareRev: String,
        currentRev: String
    ) : String? {
        // try to get the resource identifiers (the expected format is "{model.lowercase()_hardwareRev.withoutPoints()})
        val resString = model.lowercase() + "_" + hardwareRev.replace(".", "")
        val zip = context.resources.getIdentifier(resString, "raw", context.packageName)
        val changelog = context.resources.getIdentifier(resString + "_changelog", "raw", context.packageName)

        if (zip == 0 || changelog == 0) return null

        // extract the first firmware version string out of the changelog
        var changelogText = context.resources.getRawTextFile(changelog)
        changelogText = changelogText.drop(changelogText.indexOf("## [") + "## [".length)
        changelogText = changelogText.dropLast(changelogText.length - changelogText.indexOf("] -"))

        // reduce the firmware revision strings to digits and points
        val current = currentRev.filter { (it.isDigit() || it == '.') }.split(".").toMutableList()
        val update = changelogText.filter { (it.isDigit() || it == '.') }.split(".")

        // the update determines the valid number of subversion's, so add "0" to current uf necessary
        while (current.size < update.size) {current.add("0")}

        // finally compare the versions going from top down
        for (i in update.indices) {
            if (current[i].toInt() < update[i].toInt()) return changelogText
        }

        return null
    }

    fun startUpdate(
        @ApplicationContext context: Context,
        model: String,
        hardwareRev: String,
    ) {
        // try to get the resource identifiers (the expected format is "{model.lowercase()_hardwareRev.withoutPoints()})
        val resString = model.lowercase() + "_" + hardwareRev.replace(".", "")
        val zip = context.resources.getIdentifier(resString, "raw", context.packageName)
        if (zip == 0) throw Exception("no zip file for this model found")
        // TODO check if already running
        val starter = DfuServiceInitiator(address).apply {
            setZip(zip)
        }
        dfuServiceController = starter.start(context, DfuService::class.java)
    }

    fun abortUpdate() {
        dfuServiceController?.abort()
    }

    fun clear() {
        dfuServiceController = null;
    }

    private fun isMyDevice(address: String): Boolean {
        val myAddress = this.address.replace(":", "").toLongOrNull(16)
        val messageAddress = address.replace(":", "").toLongOrNull(16)

        if (myAddress == null || messageAddress == null) {
            Log.e("DfuRepository", "address string is not a mac address")
            return false
            //throw Exception("address is not a mac address")
        }

        return (myAddress == messageAddress) || ((myAddress + 1) == messageAddress)
    }
}