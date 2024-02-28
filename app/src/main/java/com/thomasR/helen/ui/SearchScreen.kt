package com.thomasR.helen.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.thomasR.helen.R
import com.thomasR.helen.data.PermissionAlertData
import com.thomasR.helen.demo.DummyData
import com.thomasR.helen.permission.BluetoothPermission
import com.thomasR.helen.permission.LocationPermission
import com.thomasR.helen.repository.HelenRepository
import com.thomasR.helen.repository.MainRepository
import com.thomasR.helen.repository.Theme
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState

@Composable
fun SearchScreen (
    modifier: Modifier = Modifier,
    repository: MainRepository,
    connectDevice: (String, String?) -> Unit,
    disconnectDevice: (String) -> Unit,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Companion.Factory(repository, connectDevice, disconnectDevice)),
    permissionViewModel: PermissionViewModel = viewModel(),
    dummyDevices: List<DummyData>,
    addDummyDevice: (DummyData) -> Unit,
    theme: Theme,
    changeTheme: (Theme) -> Unit,
    openDevice: (HelenRepository) -> Unit
) {
    val isScanning by searchViewModel.isScanning.collectAsState()
    val devices = searchViewModel.devices

    searchViewModel.ObserveLifecycleEvents(lifecycle = LocalLifecycleOwner.current.lifecycle)

    //val permissionViewModel = viewModel<PermissionViewModel>()
    val bluetoothEnabledState by permissionViewModel.bluetoothEnabledState.collectAsState()
    val locationEnabledState by permissionViewModel.locationEnabledState.collectAsState()
    val permissionAlertState by permissionViewModel.permissionAlertState.collectAsState()

    Scaffold (
        modifier = modifier,
        topBar = {
            SearchTopAppBar(
                dummyDevices = dummyDevices,
                addDummyDevice = addDummyDevice,
                theme = theme,
                changeTheme = changeTheme,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        Column (modifier = Modifier.padding(paddingValues)) {
            BluetoothEnable(enabled = bluetoothEnabledState)
            LocationEnable(enabled = locationEnabledState, necessary = true)
            SearchButton(
                modifier = Modifier.padding( vertical = dimensionResource(id = R.dimen.padding_small), horizontal = dimensionResource(id = R.dimen.padding_medium)),
                enabled = bluetoothEnabledState && locationEnabledState,
                isSearching = isScanning,
                permissionAlertState = permissionAlertState,
                bluetoothPermission = permissionViewModel.bluetoothPermission,
                locationPermission = permissionViewModel.locationPermission,
                startScanning = { searchViewModel.startScanning() },
                stopScanning = { searchViewModel.stopScanning() },
                setAlert = {alert, confirm, action ->
                    permissionViewModel.setAlertData(alert, confirm, action)
                },
                clearAlert = { permissionViewModel.clearAlert() },
            )
            DeviceList(
                deviceList = devices,
                openDevice = openDevice,
                connectDevice = { searchViewModel.connectDevice(it, null)},
                disconnectDevice = { searchViewModel.disconnectDevice(it) },
                isDeviceBonded = { searchViewModel.isDeviceBonded(it) },
                removeDevice = {address, keepBond -> searchViewModel.removeDevice(address, keepBond) }
            )
        }
    }
}

@Composable
fun <viewModel : LifecycleObserver> viewModel.ObserveLifecycleEvents(lifecycle: Lifecycle) {
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(this@ObserveLifecycleEvents)
        onDispose {
            lifecycle.removeObserver(this@ObserveLifecycleEvents)
        }
    }
}

@Composable
private fun BluetoothEnable(
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    if (!enabled) {
        Row (
            modifier = modifier
                .background(MaterialTheme.colorScheme.error)
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.padding_medium)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            Text(
                text = stringResource(id = R.string.bluetooth_disabled),
                color = MaterialTheme.colorScheme.onError
            )
            TextButton(
                onClick = { enableBluetooth(context)} ) {
                Text(
                    text = stringResource(id = R.string.turn_on),
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun enableBluetooth(context: Context) {
    context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
}

@Composable
private fun LocationEnable(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    necessary: Boolean
) {
    if (!enabled && necessary) {
        Row (
            modifier = modifier
                .background(MaterialTheme.colorScheme.error)    // TODO is this the correct color, maybe container color?
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.padding_medium)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            Text(
                text = stringResource(id = R.string.location_disabled),
                color = MaterialTheme.colorScheme.onError
            )
            TextButton(
                onClick = { openLocationSettings(context) } ) {
                Text(
                    text = stringResource(id = R.string.open_settings),
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

private fun openLocationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

private fun openPermissionSettings(context: Context) {
    ContextCompat.startActivity(
        context,
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ),
        null
    )
}

@Composable
private fun SearchButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    isSearching: Boolean,
    permissionAlertState: PermissionAlertData,
    bluetoothPermission: BluetoothPermission,
    locationPermission: LocationPermission,
    startScanning: () -> Unit,
    stopScanning: () -> Unit,
    setAlert: (String, String, () -> Unit) -> Unit,
    clearAlert: () -> Unit
) {
    val locationRationalText = stringResource(id = R.string.location_permission_rational)
    val bluetoothRationalText = stringResource(id = R.string.bluetooth_permission_rational)
    val locationDeniedText = stringResource(id = R.string.location_permission_denied)
    val bluetoothDeniedText = stringResource(id = R.string.bluetooth_permission_denied)
    val okText = stringResource(id = R.string.ok)
    val openSettingsText = stringResource(id = R.string.open_settings)

    val requiredLocationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val requiredBluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        for (result in it) {
            if (!result.value) {
                if (result.key == Manifest.permission.ACCESS_FINE_LOCATION)
                    setAlert(locationDeniedText, openSettingsText) {
                        openPermissionSettings(context)
                        clearAlert()
                    }
                else
                    setAlert(bluetoothDeniedText, openSettingsText) {
                        openPermissionSettings(context)
                        clearAlert()
                    }
                return@rememberLauncherForActivityResult
            }
        }
        startScanning()
    }

    Button(
        modifier = modifier,
        onClick = {
            // no need to check permissions if already scanning
            if (isSearching) {
                stopScanning()
            } else if (locationPermission.isRequired) {
                when {
                    locationPermission.isGranted(context) -> startScanning()
                    locationPermission.shouldShowRational(context) -> {
                        setAlert(locationRationalText, okText) {
                            launcher.launch(requiredLocationPermissions)
                            clearAlert()
                        }
                    }
                    else -> launcher.launch(requiredLocationPermissions)
                }
            } else if (bluetoothPermission.isRequired) {
                when {
                    bluetoothPermission.arePermissionsGranted(context) -> startScanning()
                    bluetoothPermission.shouldShowRational(context) -> {
                        setAlert(bluetoothRationalText, okText) {
                            launcher.launch(requiredBluetoothPermissions)
                            clearAlert()
                        }
                    }
                    else -> launcher.launch(requiredBluetoothPermissions)
                }
            }
        },
        enabled = enabled
    ) {
        Text(
            text = if (!enabled) stringResource(id = R.string.bluetooth_disabled)
                   else if (isSearching) stringResource(R.string.search_stop)
                   else stringResource(R.string.search_start),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }

    if (permissionAlertState.showAlert) {
        RationalDialog(
            rationalText = permissionAlertState.alertText,
            okButtonString = permissionAlertState.confirmText,
            onOk = permissionAlertState.onConfirmAction,
            onCancel = clearAlert
        )
    }
}

@Composable
private fun RationalDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    rationalText: String,
    okButtonString: String,
    onOk: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        title = {
            if (title != null) {
                Text(text = title)
            }
        },
        text = {Text(text = rationalText)},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onOk) {
                Text(okButtonString)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
private fun DeviceList(
    modifier: Modifier = Modifier,
    deviceList: List<HelenRepository>,
    openDevice: (HelenRepository) -> Unit,
    connectDevice: (String) -> Unit,
    disconnectDevice: (String) -> Unit,
    isDeviceBonded: (String) -> Boolean,
    removeDevice: (String, Boolean) -> Unit
) {
    var deleteBondDialog by rememberSaveable { mutableStateOf("") }
    val statusStrings = stringArrayResource(id = R.array.device_status)

    LazyColumn(
        modifier = modifier
    )  {
        val devices = mapOf(
            statusStrings[1] to mutableListOf<HelenRepository>(),
            statusStrings[2] to mutableListOf<HelenRepository>(),
            statusStrings[3] to mutableListOf<HelenRepository>(),
            statusStrings[4] to mutableListOf<HelenRepository>()
        )

        for (device in deviceList) {
            if (!BluetoothAdapter.checkBluetoothAddress(device.data.value.address)) {
                devices[statusStrings[3]]!!.add(device) //dummies.add(device)
            } else if (device.data.value.isHelenProjectSupported == true) {
                if (device.data.value.connectionState == null ||
                    device.data.value.connectionState == GattConnectionState.STATE_CONNECTING ||
                    device.data.value.connectionState == GattConnectionState.STATE_CONNECTED)
                    devices[statusStrings[1]]!!.add(device)
                else if (device.data.value.connectionState == GattConnectionState.STATE_DISCONNECTING ||
                    device.data.value.connectionState == GattConnectionState.STATE_DISCONNECTED)
                    devices[statusStrings[2]]!!.add(device)
            } else if (device.data.value.isHelenProjectSupported == false) {
                devices[statusStrings[4]]!!.add(device)//incompatible.add(device)
            }
        }

        var dividerNecessary = false    // initiate to false to prevent divider at top of the list
        for (device in devices) {
            if (device.value.isNotEmpty()) {
                if (dividerNecessary)
                    item { HorizontalDivider(Modifier.padding(dimensionResource(id = R.dimen.padding_medium))) }
                dividerNecessary = true
            }

            items(device.value) {
                val name = it.data.value.name
                val info = if (device.key == statusStrings[1] && it.data.value.connectionState == null)
                    statusStrings[0] else device.key
                val model = it.dis.data.value.model
                val clickable = device.key != statusStrings[4]
                val onClick: () -> Unit = when(device.key) {
                    statusStrings[4] -> { { } }
                    statusStrings[2] -> { { connectDevice(it.data.value.address!!) } }
                    else -> { { openDevice(it) } }
                }
                val swipeIcon = if (device.key == statusStrings[1]) Icons.Default.LinkOff else Icons.Default.Delete
                val swipeNote = if (device.key == statusStrings[1]) R.string.swipe_to_disconnect else R.string.swipe_to_remove
                val onSwipe: () -> Unit = if (device.key == statusStrings[1]) {
                        { disconnectDevice(it.data.value.address!!) }
                } else {
                    {
                        if (!isDeviceBonded(it.data.value.address!!))
                            removeDevice(it.data.value.address!!, false)
                        else
                            deleteBondDialog = it.data.value.address!!
                    }
                }

                SwipeableDeviceListItem(
                    modifier = Modifier.clickable(clickable) { onClick() },
                    name = name,
                    info = info,
                    model = model,
                    swipeIcon = swipeIcon,
                    swipeNote = stringResource(id = swipeNote),
                    onSwipe = { onSwipe() }
                )
            }
        }

        item {
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }

    if (BluetoothAdapter.checkBluetoothAddress(deleteBondDialog)) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = { removeDevice(deleteBondDialog, true); deleteBondDialog = "" },
            confirmButton = {
                TextButton(onClick = { removeDevice(deleteBondDialog, false); deleteBondDialog = "" } ) {
                    Text(stringResource(R.string.delete_bond))
                }
            },
            dismissButton = {
                TextButton(onClick = { removeDevice(deleteBondDialog, true); deleteBondDialog = "" }) {
                    Text(stringResource(R.string.keep_bond))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_bond_heading),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center)
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_bond_info),
                    modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        )

    }
}


/*private fun isMacAddress(str: String?) : Boolean {
    return BluetoothAdapter.checkBluetoothAddress(str)

    val macRegex = Regex("([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}")
    return str?.matches(macRegex) ?: false
}*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDeviceListItem(
    modifier: Modifier = Modifier,
    name: String?,
    info: String,
    model: String?,
    swipeNote: String? = null,
    swipeIcon: ImageVector? = null,
    onSwipe: () -> Unit
) {
    val imageVector = getModelVector(model)
    var isDismissed by remember { mutableStateOf(false)}
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                isDismissed = true
                false
            } else true
        },
        positionalThreshold = {distance -> distance * 0.25f}
    )

    if (isDismissed) {
        onSwipe()
        isDismissed = false
    }

    val alignment = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd,
        SwipeToDismissBoxValue.Settled -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
    }

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(id = R.dimen.padding_medium)),
                contentAlignment = alignment
            ) {
                Row {
                    if (alignment == Alignment.CenterStart) {
                        if (swipeIcon != null) {
                            Icon(swipeIcon, null)
                            Spacer(Modifier.width(dimensionResource(id = R.dimen.padding_small)))
                        }
                        if (swipeNote != null) { Text(swipeNote) }
                    }
                    else {
                        if (swipeNote != null) { Text(swipeNote) }
                        if (swipeIcon != null) {
                            Spacer(Modifier.width(dimensionResource(id = R.dimen.padding_small)))
                            Icon(swipeIcon, null)
                        }
                    }
                }
            }
        }
    ) {
        ListItem(
            headlineContent = { Text(name ?: "n/a") },
            supportingContent = { Text(info) },
            leadingContent = {
                if (imageVector != null) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.image_size))
                            .padding(dimensionResource(R.dimen.padding_small))
                    )
                }
            }
        )
    }
}

@Composable
private fun getModelVector(model: String?) : ImageVector? {
    val imageResourceId: Int = when(model) {
        "KD2" -> R.drawable.helen_icon
        //"Helena" -> R.drawable.helena_list_icon
        else -> return null
    }

    return ImageVector.vectorResource(id = imageResourceId)
}

/*@Preview(showBackground = true)
@Composable
private fun PreviewBright() {
    HelenTheme (useDarkTheme = false) {
        SearchScreen(onDeviceClicked = {})
    }
}*/

/*@Preview(showBackground = true)
@Composable
private fun PreviewDark() {
    HelenTheme (useDarkTheme = true) {
        SearchScreen(onDeviceClicked = {})
    }
}*/