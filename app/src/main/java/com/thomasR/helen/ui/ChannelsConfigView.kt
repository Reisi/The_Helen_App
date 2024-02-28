package com.thomasR.helen.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize

interface ChannelsConfigView {
    val profiles: List<String>

    @Composable
    fun ChannelsView(
        profile: Int?,
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier
    )

    @Composable
    fun ChannelsConfig(
        profile: Int?,
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier,
        onChanged: (List<HPSChannelConfig>, Boolean) -> Unit
    )
}