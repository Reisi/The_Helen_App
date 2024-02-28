package com.thomasR.helen.ui

import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.thomasR.helen.R
import com.thomasR.helen.service.DfuService
import no.nordicsemi.android.dfu.DfuServiceController
import no.nordicsemi.android.dfu.DfuServiceInitiator

@Composable
fun DfuScreen(
    modifier: Modifier = Modifier,
    address: String,
) {
    val context = LocalContext.current
    Button(onClick = { startDfu(context, address) }) {
        Text("start")
    }
}

private fun startDfu(context: Context, address: String): DfuServiceController {
    val starter = DfuServiceInitiator(address).apply {

    }
    starter.setZip(R.raw.kd2_10)
    return starter.start(context, DfuService::class.java)
}