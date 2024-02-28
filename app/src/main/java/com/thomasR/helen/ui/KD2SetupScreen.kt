package com.thomasR.helen.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomasR.helen.R
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelDescription
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize
import com.thomasR.helen.profile.kd2.data.KD2ChannelSetup
import com.thomasR.helen.profile.kd2.data.KD2ComPinConfig
import com.thomasR.helen.profile.kd2.data.KD2OpticType
import com.thomasR.helen.profile.kd2.KD2ControlPointDataParser
import com.thomasR.helen.profile.kd2.data.KD2ControlPointCommonResponse
import com.thomasR.helen.repository.HelenRepository
import com.thomasR.helen.repository.KD2Repository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val POWER_MAX = 40f
private const val POWER_MIN = 0f
private const val LIMIT_MAX = 100
private const val LIMIT_MIN = 0
private const val OFFSET_MAX = 20f
private const val OFFSET_MIN = -20f

private val simple3ChannelSetup = listOf(
    KD2ChannelSetup(18f, 85, KD2OpticType.SPOT, 4.3f),  // TODO check angle offset (value and sign)
    KD2ChannelSetup(18f, 85, KD2OpticType.FLOOD, -4.3f),
    KD2ChannelSetup(18f, 85, KD2OpticType.SPOT, 4.3f),
    KD2ChannelSetup(0.5f, 100, KD2OpticType.NA, 0.0f),
    KD2ChannelSetup(0f, 100, KD2OpticType.NA, 0.0f)
)

private val simple1ChannelSetup = listOf(
    KD2ChannelSetup(18f, 85, KD2OpticType.SPOT, 0f),  // TODO check angle offset (value and sign)
    KD2ChannelSetup(0.5f, 100, KD2OpticType.NA, 0.0f),
    KD2ChannelSetup(0f, 100, KD2OpticType.NA, 0.0f)
)

@Composable
fun KD2SetupScreen(
    modifier: Modifier = Modifier,
    channelSize: List<HPSFeatureChannelSize>?,
    //expertMode: Boolean,
    repository: HelenRepository,
    viewModel: KD2ViewModel = viewModel(factory = KD2ViewModel.Companion.Factory(repository)),
    showSnackBar: (String) -> Unit
) {
    val response by viewModel.response.collectAsState()
    if (response is KD2ControlPointCommonResponse) {
        val message = KD2ControlPointDataParser().getControlPointCommonResponseMessage(
            context = LocalContext.current,
            response = response as KD2ControlPointCommonResponse
        )
        Log.d("KD2SetupScreen", "common response received")
        showSnackBar(message)
        viewModel.clearControlPointResponse()
    }

    var showSetupDialog by rememberSaveable { mutableStateOf(false) }
    //var ignoreWrongSetup by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val kd2Data by viewModel.data.collectAsState()
    val helenData by viewModel.deviceData.collectAsState()
    val channelConfigs = kd2Data.controlPointData.channelConfigs
    val comPinConfig = kd2Data.controlPointData.comPinConfig

    val channelNames = stringArrayResource(id = R.array.kd2setup_channel_names).toList()
    val nameOffset =
        if (channelSize?.get(1)?.channelDescription == HPSFeatureChannelDescription.CURRENT) 0 else 2

    LazyColumn (modifier = modifier.padding(top = dimensionResource(R.dimen.padding_medium))) {
        if (helenData.setupProfile != 0) {
            for (i in channelConfigs.indices) {
                item {
                    var isValid by rememberSaveable { mutableStateOf(true) }
                    KD2ChannelSetup(
                        channelName = channelNames[i + nameOffset],
                        channelConfig = channelConfigs[i],
                        onChanged = { config, valid -> isValid = valid; viewModel.updateChannelConfig(i, config) }
                    )
                    ReloadUpload(
                        onReloadClick = { viewModel.reloadChannelConfig(i) },
                        onUploadClick = { viewModel.uploadChannelConfig(i, channelConfigs[i]) },
                        uploadEnabled = isValid
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_small)))
                }
            }
        } else if (!helenData.ignoreWrongSetup) {
            val ref = if (nameOffset == 0) simple3ChannelSetup else simple1ChannelSetup
            showSetupDialog = !isSimpleSetupValid(channelConfigs, ref)
        }

        if (comPinConfig != null) {
            item {
                KD2ComPinSetup(
                    comPinSetup = comPinConfig,
                    onSelectionChanged = { viewModel.updateComPinUsage(it) }
                )
                ReloadUpload(
                    onReloadClick = { viewModel.reloadComPinUsage() },
                    onUploadClick = { viewModel.uploadComPinUsage(comPinConfig) }
                )
            }
        }

        item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars)) }
    }

    if (showSetupDialog) {
        AlertDialog(
            text = {Text(stringResource(id = R.string.kd2_incorrect_setup))},
            onDismissRequest = { showSetupDialog = false; viewModel.setIgnoreWrongSetup(true) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSetupDialog = false;
                        val ref = if (nameOffset == 0) simple3ChannelSetup else simple1ChannelSetup
                        scope.launch {
                            for (i in channelConfigs.indices) {
                                viewModel.updateChannelConfig(i, ref[i])
                                viewModel.uploadChannelConfig(i, ref[i])
                                delay(250)
                            }
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.upload))
                }
            },
            dismissButton = { TextButton(onClick = { showSetupDialog = false; viewModel.setIgnoreWrongSetup(true) }) {
                Text(stringResource(id = R.string.cancel))
            }}
        )
    }
}

private fun isSimpleSetupValid(config: List<KD2ChannelSetup>, ref: List<KD2ChannelSetup>) : Boolean {
    return config == ref.take(config.size)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KD2ChannelSetup(
    modifier: Modifier = Modifier,
    channelName: String? = null,
    channelConfig: KD2ChannelSetup,
    onChanged: (KD2ChannelSetup, Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val optics = stringArrayResource(id = R.array.kd2setup_optics).toList()

    var power by rememberSaveable(channelConfig) { mutableStateOf(channelConfig.fullOutputPower.toString()) }
    var limit by rememberSaveable(channelConfig) { mutableStateOf(channelConfig.outputLimit.toString()) }
    var optic by rememberSaveable(channelConfig) { mutableIntStateOf(channelConfig.opticType.ordinal) }
    var offset by rememberSaveable(channelConfig) { mutableStateOf(channelConfig.opticOffset.toString()) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (channelName != null) {
            Text(text = channelName, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_small)))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
        ) {
            OutlinedTextField(
                value = power,
                onValueChange = {
                    power = it
                    val (config, valid) = isChannelSetupValid(power, limit, optic.toEnum<KD2OpticType>(), offset)
                    onChanged(config, valid)
                },
                label = { Text(stringResource(R.string.kd2setup_fullPower)) },
                isError = !isPowerValid(power),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
            )
            OutlinedTextField(
                value = limit,
                onValueChange = {
                    limit = it
                    val (config, valid) = isChannelSetupValid(power, limit, optic.toEnum<KD2OpticType>(), offset)
                    onChanged(config, valid)
                },
                label = { Text(stringResource(R.string.kd2setup_limit)) },
                isError = !isLimitValid(limit),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {expanded = !expanded},
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                OutlinedTextField(
                    modifier = Modifier.menuAnchor(),
                    readOnly = true,
                    value = optics[optic],
                    onValueChange = { },
                    label = {Text(stringResource(id = R.string.kd2setup_optictype))},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    optics.forEach {type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                optic = optics.indexOf(type)
                                val (config, valid) = isChannelSetupValid(power, limit, optic.toEnum<KD2OpticType>(), offset)
                                onChanged(config, valid)
                                expanded = false
                            },
                            // contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            OutlinedTextField(
                value = offset,
                onValueChange = {
                    offset = it
                    val (config, valid) = isChannelSetupValid(power, limit, optic.toEnum<KD2OpticType>(), offset)
                    onChanged(config, valid)
                },
                label = { Text(stringResource(R.string.kd2setup_angleOffset)) },
                isError = !isOffsetValid(offset),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
            )
        }
    }
}

private fun isChannelSetupValid(
    power: String,
    limit: String,
    opticType: KD2OpticType?,
    opticOffset: String
) : Pair<KD2ChannelSetup, Boolean> {
    val valid = isPowerValid(power) && isLimitValid(limit) && opticType != null && isOffsetValid(opticOffset)

    return Pair(
        KD2ChannelSetup(power.toFloat(), limit.toInt(), opticType ?: KD2OpticType.NA, opticOffset.toFloat()),
        valid
    )
}

private fun isPowerValid(power: String) : Boolean {
    val value = power.toFloatOrNull() ?: return false
    if (value > POWER_MAX) return false
    if (value < POWER_MIN) return false
    return true
}

private fun isLimitValid(limit: String) : Boolean {
    val value = limit.toIntOrNull() ?: return false
    if (value > LIMIT_MAX) return false
    if (value < LIMIT_MIN) return false
    return true
}

private fun isOffsetValid(offset: String) : Boolean {
    val value = offset.toFloatOrNull() ?: return false
    if (value > OFFSET_MAX) return false
    if (value < OFFSET_MIN) return false
    return true
}

inline fun <reified T : Enum<T>> Int.toEnum(): T? {
    return enumValues<T>().firstOrNull { it.ordinal == this }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KD2ComPinSetup(
    modifier: Modifier = Modifier,
    comPinSetup: KD2ComPinConfig,
    onSelectionChanged: (KD2ComPinConfig) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    //var setup by rememberSaveable(comPinSetup.ordinal) { mutableIntStateOf(comPinSetup.ordinal) }
    val options = stringArrayResource(id = R.array.kd2setup_comPinArray).toList()

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = {expanded = !expanded}
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = options[comPinSetup.ordinal],
            onValueChange = { },
            label = {Text(stringResource(id = R.string.kd2setup_comPin))},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach {type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        //setup = options.indexOf(type)
                        onSelectionChanged(options.indexOf(type).toEnum<KD2ComPinConfig>() ?: KD2ComPinConfig.NOT_USED)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

/*@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun StatusPreviewBright() {
    HelenTheme(useDarkTheme = false) {
        KD2SetupScreen()
    }
}*/