package com.thomasR.helen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.thomasR.helen.R
import com.thomasR.helen.profile.dfu.data.InProgress
import com.thomasR.helen.profile.helenProject.data.HPSMeasurementData
import com.thomasR.helen.profile.helenProject.data.HPSModeConfig
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    helenViewModel: HelenViewModel
) {
    val projectData by helenViewModel.hpsData.collectAsState()
    val info by helenViewModel.disData.collectAsState()
    val dfu by helenViewModel.dfuProgress.collectAsState()

    var selectMode: ((Int) -> Unit)? = { helenViewModel.selectMode(it) }
    if (projectData.isControlPointEnabled != true ||
        projectData.feature?.feature?.modeSetSupported != true)
        selectMode = null

    Column (modifier = modifier) {
        DeviceInformation(
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.padding_small)),
            model = info.model ?: stringResource(R.string.not_available),
            hardwareRev = info.hardwareRevision ?: stringResource(R.string.not_available),
            firmwareRev = info.firmwareRevision ?: stringResource(R.string.not_available),
            availableFirmwareRev = helenViewModel.isFirmwareUpdateAvailable(),
            changelog = helenViewModel.getChangelog(),
            firmwareUpdateProgress = (dfu as? InProgress)?.progress,
            updateFirmwareClicked = { helenViewModel.startFirmwareUpdate() }
        )
        if (projectData.feature?.modeCount != null && projectData.modes != null) {
            Modes(
                modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.padding_small)),
                modes = projectData.modes!!,
                numOfModes = projectData.feature!!.modeCount,
                currentMode = projectData.measurement?.mode,
                selectMode = selectMode
            )
        }
        if (projectData.measurement != null) {
            StatusGrid(measurement = projectData.measurement!!)
        }
    }
}

@Composable
private fun DeviceInformation(
    modifier: Modifier = Modifier,
    model: String,
    hardwareRev: String,
    firmwareRev: String,
    availableFirmwareRev: String? = null,
    changelog: String? = null,
    firmwareUpdateProgress: Int?,
    updateFirmwareClicked: () -> Unit,
) {
    var showUpdate by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row {
            Column {
                Text(text = stringResource(R.string.device_model))
                Text(text = stringResource(R.string.device_hardware_rev))
                Text(text = stringResource(R.string.device_firmware_rev))
            }
            Column {
                Text(text = model)
                Text(text = hardwareRev)
                if (firmwareUpdateProgress != null) {   // text if update is in progress
                    Text(text = stringResource(id = R.string.firmware_updating))
                } else if (availableFirmwareRev != null) {  // text if update is available
                    val firmware = buildAnnotatedString {
                        //withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                            append("$firmwareRev ")
                        //}
                        withStyle(style = SpanStyle(
                            //color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append("($availableFirmwareRev ${stringResource(R.string.available)})")
                        }
                    }
                    /*Text(
                        text = firmware,
                        modifier = Modifier.clickable { showUpdate = true }
                    )*/
                    ClickableText(    // doesn't use correct color and size
                        text = firmware,
                        onClick = { if (it > "$firmwareRev ".length) showUpdate = true },
                        style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                } else {                                // text if no update is available
                    Text(text = firmwareRev)
                }
            }
            /*TextButton(onClick = updateFirmwareClicked) {
                Text(text = stringResource(id = R.string.firmware_update))
            }*/
        }
        if (firmwareUpdateProgress != null) {
            LinearProgressIndicator(
                progress = { firmwareUpdateProgress.toFloat() / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showUpdate) {
        AlertDialog(
            onDismissRequest = { showUpdate = false },
            confirmButton = {
                TextButton(onClick = {showUpdate = false; updateFirmwareClicked() }) {
                    Text(stringResource(R.string.update))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.changelog),
                textAlign = TextAlign.Center
            ) },
            text = {
                MarkdownText(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    markdown = changelog ?: stringResource(R.string.not_available),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)// MaterialTheme.typography.bodySmall
                )

                /*Text(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    text = changelog ?: buildAnnotatedString {append(stringResource(R.string.not_available)) },
                    style = MaterialTheme.typography.bodySmall
                )*/
            }
        )
    }
}

private fun getModeLabel(
    modeNo: Int,
    mode: HPSModeConfig
) : String {
    var label = ""
    if (mode.helen.preferred) label += "P"
    if (mode.helen.temporary) label += "T"
    if (mode.helen.off) label += "O"
    return if (label == "") (modeNo + 1).toString() else label
}

@Composable
private fun Modes(
    modifier: Modifier = Modifier,
    numOfModes: Int,
    modes: List<List<HPSModeConfig>>,
    currentMode: Int?,
    selectMode: ((Int) -> Unit)?
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        val paddingSmall = dimensionResource(id = R.dimen.padding_small)
        Column {
            Text (
                text = stringResource(id = if (selectMode == null) R.string.current_mode else R.string.select_mode),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            BoxWithConstraints (
                modifier = Modifier.padding(paddingSmall)
            ) {
                val requiredWidth = numOfModes * (32.dp + (2 * paddingSmall)) - paddingSmall
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = if (requiredWidth <= maxWidth)
                        Arrangement.SpaceBetween
                    else
                        Arrangement.spacedBy(paddingSmall)
                ) {
                    var modeNo = 0
                    for (group in modes) {
                        for (mode in group) {
                            val thisMode = modeNo
                            OutlinedButton(
                                contentPadding = PaddingValues.Absolute(0.dp),
                                modifier = Modifier.size(32.dp),
                                onClick = {
                                    if (selectMode != null) {
                                        selectMode(thisMode)
                                    }
                                },
                                colors = if (thisMode == currentMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = getModeLabel(thisMode, mode),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                            modeNo++
                        }
                    }
                }
            }
                /*val requiredWidth = numOfModes * 48 - 8
                if (requiredWidth.dp <= maxWidth) {
                    Row (
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        var modeNo = 0
                        for (group in modes) {
                            for (mode in group) {
                                var label = ""
                                if (mode.helen.preferred) label += "P"
                                if (mode.helen.temporary) label += "T"
                                if (mode.helen.off) label += "O"
                                if (label == "") label += (modeNo + 1).toString()
                                val thisMode = modeNo
                                OutlinedButton (
                                    contentPadding = PaddingValues.Absolute(0.dp),
                                    modifier = Modifier.size(32.dp),
                                    onClick = { if (selectMode != null) { selectMode(thisMode) } },
                                    colors = if (thisMode == currentMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                modeNo++
                            }
                        }*/
                        /*for (mode in 0 until numOfModes) {
                            OutlinedButton (
                                contentPadding = PaddingValues.Absolute(0.dp),
                                modifier = Modifier.size(32.dp),
                                onClick = { if (selectMode != null) { selectMode(mode) } },
                                colors = if (mode == currentMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = (mode + 1).toString(),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }*/
                    /*}
                } else {
                    LazyRow (
                        horizontalArrangement = Arrangement.spacedBy(paddingSmall)
                    )  {
                        var modeNo = 0
                        for (group in modes) {
                            for (mode in group) {
                                var label = ""
                                if (mode.helen.preferred) label += "P"
                                if (mode.helen.temporary) label += "T"
                                if (mode.helen.off) label += "O"
                                if (label == "") label += (modeNo + 1).toString()
                                val thisMode = modeNo
                                item {
                                    OutlinedButton (
                                        contentPadding = PaddingValues.Absolute(0.dp),
                                        modifier = Modifier.size(32.dp),
                                        onClick = { if (selectMode != null) { selectMode(thisMode) } },
                                        colors = if (thisMode == currentMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                modeNo++
                            }
                        }*/

                        /*items(numOfModes) {modeNo ->
                            OutlinedButton (
                                contentPadding = PaddingValues.Absolute(0.dp),
                                modifier = Modifier
                                   // .padding(dimensionResource(id = R.dimen.padding_small) / 2)
                                    .size(32.dp),
                                onClick = { if (selectMode != null) { selectMode(modeNo) } },
                                colors = if (modeNo == currentMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(
                                    text = (modeNo + 1).toString(),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }*/
                    /*}
                }
            }*/
            if (selectMode != null) {
                Button(
                    modifier = Modifier
                        .padding(start = paddingSmall, end = paddingSmall, bottom = paddingSmall)
                        .height(32.dp),
                    onClick = { selectMode(255) }   // TODO: get rid of number
                ) {
                    Text(
                        text = stringResource(id = R.string.device_off),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusItem(
    modifier: Modifier = Modifier,
    description: String,
    value: String,
    unit: String
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column (modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_small))) {
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Row {
                Text(
                    text = value,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = dimensionResource(R.dimen.padding_small)),
                    textAlign = TextAlign.Right,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily(Font(R.font.dseg7classic)),
                )
                Text(text = unit)
            }
        }


    }
}

@Composable
private fun StatusGrid(
    modifier: Modifier = Modifier,
    measurement: HPSMeasurementData
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small)),
    ) {
        val powerString = if (measurement.outputPower == null) "--"
            else if (measurement.outputPower >= 100f) "%3.0f".format(Locale.US, measurement.outputPower)
            else if (measurement.outputPower >= 10f) "%3.1f".format(Locale.US, measurement.outputPower)
            else if (measurement.outputPower >= 1f) "%3.2f".format(Locale.US, measurement.outputPower)
            else "%4.3f".format(Locale.US, measurement.outputPower)

        val tempString = if (measurement.temperature == null) "--" else "%3.1f".format(Locale.US, measurement.temperature)

        val inputVoltageString = if (measurement.inputVoltage == null) "--"
            else if (measurement.inputVoltage >= 10f) "%3.1f".format(Locale.US, measurement.inputVoltage)
            else "%3.2f".format(Locale.US, measurement.inputVoltage)

        val socString = if (measurement.stateOfCharge == null) "--" else "%4.1f".format(Locale.US, measurement.stateOfCharge)

        item {
            StatusItem(
                description = stringResource(R.string.output_power),
                value = powerString,
                unit = stringResource(R.string.watt))
        }
        item {
            StatusItem(
                description = stringResource(R.string.temperature),
                value = tempString,
                unit = stringResource(R.string.degree_celcius))
        }
        item {
            StatusItem(
                description = stringResource(R.string.input_voltage),
                value = inputVoltageString,
                unit = stringResource(R.string.volt))
        }
        item {
            StatusItem(
                description = stringResource(R.string.state_of_charge),
                value = socString,
                unit = stringResource(R.string.percent))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusPreviewBright() {
    DeviceInformation(
        model = "KD2",
        hardwareRev = "1.0",
        firmwareRev = "3.0.0-alpha",
        availableFirmwareRev = "3.1.2",
        firmwareUpdateProgress = null
    ) {

    }
}