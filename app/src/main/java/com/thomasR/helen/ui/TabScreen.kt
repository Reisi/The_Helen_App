package com.thomasR.helen.ui

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.mandatorySystemGesturesPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thomasR.helen.R
import com.thomasR.helen.profile.dfu.data.Completed
import com.thomasR.helen.profile.dfu.data.Idle
import com.thomasR.helen.profile.dfu.data.Starting
import com.thomasR.helen.profile.genericAccess.GADataParser
import com.thomasR.helen.profile.genericAccess.data.GAEventHandled
import com.thomasR.helen.profile.genericAccess.data.GANameChangeResponse
import com.thomasR.helen.profile.genericAccess.data.GANameChangeResponseCode
import com.thomasR.helen.profile.helenProject.HPSControlPointDataParser
import com.thomasR.helen.profile.helenProject.HPSModesDataParser
import com.thomasR.helen.profile.helenProject.data.ControlPointEvent
import com.thomasR.helen.profile.helenProject.data.HPSEventHandled
import com.thomasR.helen.profile.helenProject.data.WriteModesResponse
import com.thomasR.helen.repository.HelenRepository
import kotlinx.coroutines.launch
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState

/// TODO move to KD2 specific class/file
private fun isKD2SetupNecessary(
    model: String?,
    isSupported: Boolean?,
    isExpertMode: Boolean,
    isComPinModeSupported: Boolean
) : Boolean {
    if (model != "KD2") return false
    if (isSupported != true) return false
    if (!isExpertMode && !isComPinModeSupported) return false
    return true
}

@Composable
fun TabScreen(
    modifier: Modifier = Modifier,
    repository: HelenRepository,
    helenViewModel: HelenViewModel = viewModel(factory = HelenViewModel.Companion.Factory(repository)),
    navigateBack: () -> Unit,
    reconnect: (String) -> Unit
) {
    val helenData by helenViewModel.data.collectAsState()
    val helenEvents by helenViewModel.events.collectAsState()
    val info by helenViewModel.disData.collectAsState()
    val dfu by helenViewModel.dfuProgress.collectAsState()
    val projectData by helenViewModel.hpsData.collectAsState()
    val projectEvents by helenViewModel.hpsEvents.collectAsState()
    val kd2Data by helenViewModel.kd2data.collectAsState()

    val allTabs = stringArrayResource(id = R.array.tabScreen_Headings).toMutableList()
    val availableTabs = allTabs.toMutableList()
    var selectedTab by remember { mutableStateOf(availableTabs[0]) }

    // for now a setup page is only realized for KD2 models, so this tile is just removed for non
    // KD2 models
    if (helenData.isUartSupported != true)
        availableTabs.remove(allTabs[3])
    if (!isKD2SetupNecessary(
            model = info.model,
            isSupported = helenData.isKd2Supported,
            isExpertMode = helenData.setupProfile == null,
            isComPinModeSupported = kd2Data.feature.configFeatures.comPinModeSupported
        ))
        availableTabs.remove(allTabs[2])

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember {SnackbarHostState()}

    if (
        helenData.connectionState != GattConnectionState.STATE_CONNECTED && // go back if not connected
        BluetoothAdapter.checkBluetoothAddress(helenData.address) &&        // but not in case of dummy device
        dfu == Idle                                                         // or ongoing dfu
    ) {
        navigateBack()
    }

    // request to reconnect to device after firmware update
    /// TODO this should not be done here in the ui
    var reconnectRequested by remember { mutableStateOf(false) }
    if (dfu == Starting) reconnectRequested = false
    if (dfu == Completed && !reconnectRequested && helenData.address != null) {
        reconnect(helenData.address!!)
        Log.d("TabScreen", "trying to reconnect to device ${helenData.address}")
        reconnectRequested = true
    }

    val channelsConfigView = when(info.model) {
        "KD2" -> getKD2ChannelConfigView(projectData.feature?.channelSize, kd2Data.feature.channelFeature)
        else -> DefaultChannelsConfigView()
    }

    Scaffold (
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.mandatorySystemGesturesPadding()
            )
        },
        topBar = {
            var changeName: ((String) -> Unit)? = { helenViewModel.changeName(it) }
            if (helenData.nameChangeSupported != true) changeName = null

            var startSearch: (() -> Unit)? = { helenViewModel.requestSearch(); helenViewModel.enableControlPoint(false) }
            if (projectData.feature?.feature?.searchRequestSupported != true) startSearch = null

            var factoryReset: (() -> Unit)? = { helenViewModel.factoryReset() }
            if (projectData.feature?.feature?.factoryResetSupported != true) factoryReset = null

            DeviceTopAppBar(
                deviceName = helenData.name ?: "",
                navigateBack = navigateBack,
                changeName = changeName,
                isRemoteControlEnabled = projectData.isControlPointEnabled ?: false,
                remoteControl = { helenViewModel.enableControlPoint(it) },
                startSearch = startSearch,
                factoryReset = factoryReset,
                profiles = channelsConfigView.profiles,
                selectProfile = { helenViewModel.selectSetupProfile(it) },
                selectedProfile = helenData.setupProfile
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    ) {
        // show selected control point responses as snackbar
        if (projectEvents !is HPSEventHandled) {
            val message = when (projectEvents) {
                is ControlPointEvent -> {
                    HPSControlPointDataParser().getControlPointResponseMessage(
                        context = LocalContext.current,
                        response = projectEvents as ControlPointEvent
                    )
                }
                is WriteModesResponse -> {
                    HPSModesDataParser().getWriteResponseMessage(
                        context = LocalContext.current,
                        response = projectEvents as WriteModesResponse
                    )
                }
                else -> null
            }

            LaunchedEffect(snackbarHostState) {
                if (message != null) snackbarHostState.showSnackbar(message)
                helenViewModel.clearProjectEvent()
            }
        }

        // snackbar message for name change
        if (helenEvents is GANameChangeResponse) {
            val response = (helenEvents as GANameChangeResponse).responseCode
            val message = GADataParser().getNameChangeResponseMessage(LocalContext.current, response)

            LaunchedEffect(snackbarHostState) {
                if (message != null) snackbarHostState.showSnackbar(message)
                helenViewModel.clearEvent()
            }
        }

        Column(
            modifier = Modifier.padding(it)
        ) {
            val currentIndex = availableTabs.indexOf(selectedTab)
            TabRow(selectedTabIndex = currentIndex) {
                availableTabs.forEachIndexed { _, title ->
                    Tab(
                        selected = title == selectedTab,
                        onClick = { selectedTab = title },
                        text = { Text(text = title) }
                    )
                }
            }

            when (selectedTab) {
                allTabs[0] -> StatusScreen(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium)),
                    helenViewModel = helenViewModel
                )

                allTabs[1] -> ConfigScreen(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium)),
                    helenViewModel = helenViewModel,
                    channelsConfigView = channelsConfigView
                )

                allTabs[2] -> if (helenData.isKd2Supported == true) {
                    KD2SetupScreen(
                        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium)),
                        channelSize = projectData.feature?.channelSize,
                        //expertMode = helenData.setupProfile == null,
                        repository = repository,
                        showSnackBar = {scope.launch {
                                snackbarHostState.showSnackbar(it)
                            } }
                    )
                }

                allTabs[3] -> if (helenData.isUartSupported == true) {
                    UartScreen(
                        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_medium)),
                        userName = "root",  // maybe use phone device name?
                        deviceName = helenData.name ?: "remote",
                        repository = repository.uart
                    )
                }

                else -> {
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = "something went wrong",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

/*@Preview(showBackground = true)
@Composable
private fun PreviewBright() {
    HelenTheme(useDarkTheme = false) {
        TabScreen()
    }
}*/