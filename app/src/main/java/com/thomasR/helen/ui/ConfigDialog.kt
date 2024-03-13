package com.thomasR.helen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thomasR.helen.R
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize
import com.thomasR.helen.profile.helenProject.data.HPSHelenModeConfig
import com.thomasR.helen.profile.helenProject.data.HPSModeConfig
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelDescription
import com.thomasR.helen.repository.Theme
import com.thomasR.helen.ui.theme.HelenTheme

//@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ConfigDialog(
    modifier: Modifier = Modifier,
    modeNo: Int,
    config: HPSModeConfig,
    channelSize: List<HPSFeatureChannelSize>,
    profile: Int?,
    channelsConfig: ChannelsConfigView = DefaultChannelsConfigView(),
    onChannelsChanged: ((List<HPSChannelConfig>?) -> Unit)? = null,
    controlPointEnabled: Boolean = false,
    onCancel: () -> Unit,
    onDone: (HPSModeConfig) -> Unit
) {
    var state by remember { mutableStateOf(config.copy()) }
    var valid by remember { mutableStateOf(true) }
    var override by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = {
            if (override && onChannelsChanged != null)
                onChannelsChanged(null)
            onCancel()
        },
        dismissButton = {
            TextButton(onClick = {
                if (override && onChannelsChanged != null)
                    onChannelsChanged(null)
                onCancel()
            }) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                if (override && onChannelsChanged != null)
                    onChannelsChanged(null)
                onDone(state)
            }) {
                Text(stringResource(R.string.ok))
            }
        },
        title = {
            Row (
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Absolute.SpaceBetween
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(3f),
                    text = stringResource(id = R.string.mode) + " " + (modeNo + 1).toString(),
                    textAlign = TextAlign.Center
                )
                if (onChannelsChanged != null) {
                    Switch(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        checked = override,
                        onCheckedChange = {
                            override = it
                            if (!it) {
                                onChannelsChanged(null)
                            } else if (valid) {
                                onChannelsChanged(state.channel)
                            }
                        },
                        enabled = controlPointEnabled
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ModeConfig(
                    modifier = Modifier.padding(top = 16.dp),
                    config = state.helen,
                    onIgnoredChanged = {
                        state = state.copy(helen = state.helen.copy(ignored = it))
                    },
                    onPreferredChanged = {
                        state = state.copy(helen = state.helen.copy(preferred = it))
                    },
                    onTemporaryChanged = {
                        state = state.copy(helen = state.helen.copy(temporary = it))
                    },
                    onOffChanged = { state = state.copy(helen = state.helen.copy(off = it)) }
                )

                HorizontalDivider(
                    modifier = modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline
                )

                channelsConfig.ChannelsConfig(
                    profile = profile,
                    channelsConfig = state.channel,
                    channelsSize = channelSize,
                    modifier = Modifier
                ) { config, isValid ->
                    state = state.copy(channel = config)
                    valid = isValid
                    if (override && isValid && onChannelsChanged != null)
                        onChannelsChanged(config)
                }

                HorizontalDivider(
                    modifier = modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    )
}

@Composable
fun ModeConfig(
    modifier: Modifier = Modifier,
    config: HPSHelenModeConfig,
    onIgnoredChanged: (Boolean) -> Unit,
    onPreferredChanged: (Boolean) -> Unit,
    onTemporaryChanged: (Boolean) -> Unit,
    onOffChanged: (Boolean) -> Unit
) {
    Row (
        modifier = modifier.fillMaxWidth()
    ) {
        ModeConfigElement(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = stringResource(R.string.mode_ignored),
            checked = config.ignored,
            onCheckChanged = onIgnoredChanged
        )
        ModeConfigElement(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = stringResource(R.string.mode_preferred),
            checked = config.preferred,
            onCheckChanged = onPreferredChanged
        )
        ModeConfigElement(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = stringResource(R.string.mode_temporary),
            checked = config.temporary,
            onCheckChanged = onTemporaryChanged
        )
        ModeConfigElement(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = stringResource(R.string.mode_off),
            checked = config.off,
            onCheckChanged = onOffChanged
        )
    }
}

@Composable
fun ModeConfigElement(
    modifier: Modifier = Modifier,
    label: String,
    checked: Boolean,
    onCheckChanged: (Boolean) -> Unit
) {
    Column (
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Checkbox(
            //enabled = enabled,
            checked = checked,
            onCheckedChange = onCheckChanged,
        )
        Text(label)
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=600dp")
@Composable
private fun StatusPreviewBright() {
    HelenTheme (theme = Theme.DARK) {

        ConfigDialog(
           // modifier = Modifier.height(700.dp),
            modeNo = 2,
            config = HPSModeConfig(
                HPSHelenModeConfig(true, false, false, false),
                listOf(
                    HPSChannelConfig(10, 1),
                    HPSChannelConfig(10, 1),
                    HPSChannelConfig(10, 1),
                    HPSChannelConfig(10, 1),
                    HPSChannelConfig(10, 1),
                )
            ),
            channelSize = listOf(
                HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.CURRENT),
                HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.CURRENT),
                HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.CURRENT),
                HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.PWM),
                HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.PWM),
            ),
            profile = null,
            onChannelsChanged = {  },
            controlPointEnabled = true,
            onCancel = { /*TODO*/ },
            onDone = {})
    }

}