package com.thomasR.helen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VideogameAssetOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.thomasR.helen.R
import com.thomasR.helen.service.GA_DEVICE_NAME_MAX_LENGTH

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceTopAppBar(
    modifier: Modifier = Modifier,
    deviceName: String,
    navigateBack: () -> Unit,
    changeName: ((String) -> Unit)?,
    isRemoteControlEnabled: Boolean,
    remoteControl: ((Boolean) -> Unit),
    startSearch: (() -> Unit)?,
    factoryReset: (() -> Unit)?,
    profiles: List<String>,
    selectProfile: (Int?) -> Unit,
    selectedProfile: Int?,
    //expertModeSelect: ((Boolean) -> Unit)?,
    //isExpertMode: Boolean?
) {
    var changeNameDialog by rememberSaveable { mutableStateOf(false) }
    //var startSearchDialog by rememberSaveable { mutableStateOf(false) }
    var factoryResetDialog by rememberSaveable { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        modifier = modifier,
        // make title clickable if name change is supported
        title = {
            if (changeName != null) {
                Text(deviceName, Modifier.clickable { changeNameDialog = true })
            } else {
                Text(deviceName)
            }
        },
        navigationIcon = {
            IconButton(onClick = navigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            // icon button for enabling/disabling remote control
            IconButton(onClick = { remoteControl(!isRemoteControlEnabled) }) {
                val icon = if (isRemoteControlEnabled) Icons.Default.VideogameAsset
                           else Icons.Default.VideogameAssetOff
                Icon(icon, stringResource(id = R.string.remote_control))
            }
            // device menu
            if (isRemoteControlEnabled || profiles.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        var dividerNecessary = false
                        if (isRemoteControlEnabled) {
                            // initiate search remote
                            if (startSearch != null) {
                                dividerNecessary = true
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.initiate_search)) },
                                    onClick = { expanded = false; startSearch() /*startSearchDialog = true*/ },
                                    leadingIcon = { Icon(Icons.Default.SettingsRemote, null) }
                                )
                            }
                            // factory reset
                            if (factoryReset != null) {
                                dividerNecessary = true
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.factory_reset)) },
                                    onClick = { expanded = false; factoryResetDialog = true },
                                    leadingIcon = { Icon(Icons.Default.Restore, null) }
                                )
                            }
                        }
                        if (profiles.isNotEmpty()) {
                            if (dividerNecessary)
                                HorizontalDivider(Modifier.padding(vertical = dimensionResource(id = R.dimen.padding_medium)))
                            // profiles menu entries
                            for (i in profiles.indices) {
                                DropdownMenuItem(
                                    text = { Text(profiles[i]) },
                                    onClick = { expanded = false; selectProfile(i) },
                                    leadingIcon = {
                                        val simpleIcon = if (selectedProfile == i)
                                            Icons.Default.RadioButtonChecked
                                        else
                                            Icons.Default.RadioButtonUnchecked
                                        Icon(simpleIcon, null)
                                    }
                                )
                            }
                            // expert mode menu entry
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.expert_mode)) },
                                onClick = { expanded = false; selectProfile(null) },
                                leadingIcon = {
                                    val simpleIcon = if (selectedProfile == null)
                                        Icons.Default.RadioButtonChecked
                                    else
                                        Icons.Default.RadioButtonUnchecked
                                    Icon(simpleIcon, null)
                                }
                            )
                        }
                    }
                }
            }
        }
    )

    if (changeNameDialog) {
        NameDialog(
            oldName = deviceName,
            onCancel = { changeNameDialog = false },
            onOK = { changeNameDialog = false; changeName!!(it) }   // changeNameDialog will never be set if changeName is null
        )
    }

    /*if (startSearchDialog) {
        HintDialog(
            title = stringResource(id = R.string.initiate_search),
            hint = stringResource(id = R.string.initiate_search_hint),
            onCancel = { startSearchDialog = false },
            onOk = { startSearchDialog = false; startSearch!!() }
        )
    }*/

    if (factoryResetDialog) {
        HintDialog(
            title = stringResource(id = R.string.factory_reset),
            hint = stringResource(id = R.string.factory_reset_hint),
            onCancel = { factoryResetDialog = false },
            onOk = { factoryResetDialog = false; factoryReset!!() }
        )
    }
}

@Composable
fun NameDialog(
    modifier: Modifier = Modifier,
    oldName: String,
    onCancel: () -> Unit,
    onOK: (String) -> Unit
) {
    fun isNameValid(name: String) : Boolean {
        return name.length <= GA_DEVICE_NAME_MAX_LENGTH
    }

    var newName by rememberSaveable { mutableStateOf(oldName) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = { /*TODO*/ },
        confirmButton = {
            TextButton(
                onClick = { onOK(newName) },
                enabled = isNameValid(newName)
            ) { Text(stringResource(id = R.string.upload)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(id = R.string.cancel)) }
        },
        text = {
            Column {
                Text(stringResource(id = R.string.new_name))
                OutlinedTextField(
                    value = newName,
                    isError = !isNameValid(newName),
                    onValueChange = { newName = it },
                    singleLine = true
                )
            }
        }
    )
}

@Composable
private fun HintDialog(
    modifier: Modifier = Modifier,
    title: String,
    hint: String,
    onCancel: () -> Unit,
    onOk: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        confirmButton = { TextButton(onClick = onOk) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
        title = { Text(text = title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = { Text(text = hint, modifier = Modifier.verticalScroll(rememberScrollState())) }
    )
}