package com.thomasR.helen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.thomasR.helen.R
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelDescription
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize
import com.thomasR.helen.profile.kd2.data.KD2ChannelFeature

/*fun getKD2ChannelConfigView(
    channelsSize: List<HPSFeatureChannelSize>?,
    features: KD2ChannelFeature
) : ChannelsConfigView {
    val description = channelsSize?.getOrNull(2)
    return if (description != null && description.channelDescription == HPSFeatureChannelDescription.CURRENT)
        KD23ChannelsConfigView(features = features)
    else
        KD21ChannelsConfigView(features = features)
}*/

class HelenaChannelsConfigView(
    override val profiles: List<String> = listOf("Helena", "SimpleHelena"),
    private val features: KD2ChannelFeature
): ChannelsConfigView {

    @Composable
    override fun ChannelsView(
        profile: Int?,
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier
    ) {
        when (profile) {
            profiles.indexOf("Helena") -> {
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (!isChannelsConfigValid(channelsConfig, channelsSize)) {
                        Text(stringResource(id = R.string.kd2_unexpected))
                        return
                    }

                    val (adaptive, spot, flood) = encodeSpotFlood(channelsConfig)
                    // adaptive setup
                    if (adaptive) {
                        Row (verticalAlignment = Alignment.CenterVertically) {
                            Icon(ImageVector.vectorResource(R.drawable.adaptive_beam), null, modifier.size(32.dp))
                            Text("%3d%%".format(spot * 5))
                        }
                    }
                    // without pitch compensation
                    else {
                        Row (verticalAlignment = Alignment.CenterVertically) {
                            Icon(ImageVector.vectorResource(R.drawable.flood_beam), null, modifier.size(32.dp))
                            Text("%3d%%".format(flood * 5))
                        }
                        Row (verticalAlignment = Alignment.CenterVertically) {
                            Icon(ImageVector.vectorResource(R.drawable.high_beam), null, modifier.size(32.dp))
                            Text("%3d%%".format(spot * 5))
                        }
                    }
                }
            }
            profiles.indexOf("SimpleHelena") -> {
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (!isChannelsConfigValid(channelsConfig, channelsSize)) {
                        Text(stringResource(id = R.string.kd2_unexpected))
                        return
                    }
                    val icon = if (channelsConfig[1].specialFeature == 1)
                        ImageVector.vectorResource(R.drawable.adaptive_beam)
                    else
                        ImageVector.vectorResource(R.drawable.high_beam)
                    Row (verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, modifier.size(32.dp))
                        Text("%3d%%".format(channelsConfig[1].intensity * 5))
                    }
                }
            }
            else -> {
                DefaultChannelsConfigView().ChannelsView(
                    profile = null,
                    channelsConfig = channelsConfig,
                    channelsSize = channelsSize,
                    modifier = modifier
                )
            }
        }
    }

    @Composable
    override fun ChannelsConfig(
        profile: Int?,
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier,
        onChanged: (List<HPSChannelConfig>, Boolean) -> Unit
    ) {
        when (profile) {
            profiles.indexOf("Helena") -> {
                HelenaChannelsConfig(
                    channelsConfig = channelsConfig,
                    channelsSize = channelsSize,
                    modifier = modifier,
                    onChanged = onChanged
                )
            }
            profiles.indexOf("SimpleHelena") -> {
                SimpleHelenaChannelsConfig(
                    channelsConfig = channelsConfig,
                    channelsSize = channelsSize,
                    modifier = modifier,
                    onChanged = onChanged
                )
            }
            else -> {
                DefaultChannelsConfigView().ChannelsConfig(
                    profile = null,
                    channelsConfig = channelsConfig,
                    channelsSize = channelsSize,
                    modifier = modifier,
                    onChanged = onChanged
                )
            }
        }
    }

    @Composable
    private fun HelenaChannelsConfig(
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier,
        onChanged: (List<HPSChannelConfig>, Boolean) -> Unit
    ) {
        Column(modifier = modifier/*.padding(horizontal = dimensionResource(id = R.dimen.padding_medium))*/) {
            if (!isChannelsConfigValid(channelsConfig, channelsSize)) {
                Text(stringResource(id = R.string.kd2_unexpected))
                return
            }

            // current channels
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (adaptive, spot, flood) = encodeSpotFlood(channelsConfig)

                if (features.adaptiveSupported) {
                    IconToggleButton(
                        checked = adaptive,
                        onCheckedChange = {
                            val config = decodeSpotFlood(it, spot, flood).toMutableList()
                            onChanged(config, true)
                        }
                    ) {
                        val icon = if (channelsConfig[0].specialFeature != 0)
                            ImageVector.vectorResource(R.drawable.adaptive_beam)
                        else
                            ImageVector.vectorResource(R.drawable.adaptive_beam_disabled)
                        Icon(icon, null)
                    }
                } else Spacer(Modifier.size(48.dp))
                if (adaptive) {
                    ChannelView(
                        name = stringResource(id = R.string.kd2_adaptive_channel),
                        value = spot.toFloat()
                    ) {
                        val config = decodeSpotFlood(true, it.toInt()).toMutableList()
                        onChanged(config, true)
                    }
                } else {
                    Row {
                        ChannelView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                name = stringResource(id = R.string.kd2_flood_channel),
                                value = flood.toFloat()
                            ) {
                                val config = decodeSpotFlood(false, spot, it.toInt()).toMutableList()
                                onChanged(config, true)
                            }
                        ChannelView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            name = stringResource(id = R.string.kd2_spot_channel),
                            value = spot.toFloat()
                        ) {
                            val config = decodeSpotFlood(false, it.toInt(), flood).toMutableList()
                            onChanged(config, true)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SimpleHelenaChannelsConfig(
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier,
        onChanged: (List<HPSChannelConfig>, Boolean) -> Unit
    ) {
        Column(modifier = modifier) {
            if (!isChannelsConfigValid(channelsConfig, channelsSize)){
                Text(stringResource(id = R.string.kd2_unexpected))
                return
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (features.adaptiveSupported) {
                    IconToggleButton(
                        checked = channelsConfig[1].specialFeature != 0,
                        onCheckedChange = {
                            val newConfig = channelsConfig.toMutableList()
                            newConfig[1] = channelsConfig[1].copy(specialFeature = if (it) 1 else 0)
                            onChanged(newConfig, true)
                        }
                    ) {
                        val icon = if (channelsConfig[1].specialFeature != 0)
                            ImageVector.vectorResource(R.drawable.adaptive_beam)
                        else
                            ImageVector.vectorResource(R.drawable.adaptive_beam_disabled)
                        Icon(icon, null)
                    }
                } else Spacer(Modifier.size(48.dp))
                Column {
                    Text(
                        text = /*stringResource(R.string.kd2_led_channel) +" " +
                                        */(channelsConfig[1].intensity * 5).toString() + "%",
                        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small))
                    )
                    Slider(
                        value = channelsConfig[1].intensity.toFloat(),
                        onValueChange = {
                            val newConfig = channelsConfig.toMutableList()
                            newConfig[1] = channelsConfig[1].copy(intensity = it.toInt())
                            onChanged(newConfig, true)
                        },
                        steps = 19,
                        valueRange = 0f..20f
                    )
                }
            }
        }
    }

    @Composable
    private fun ChannelView(
        modifier: Modifier = Modifier,
        name: String,
        value: Float,
        onValueChanged: (Float) -> Unit
    ) {
        Column (modifier = modifier) {
            Text(
                text = name + " " + (value * 5).toInt().toString() + "%",
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small))
            )
            Slider(
                value = value,
                onValueChange = onValueChanged,
                steps = 19,
                valueRange = 0f..20f
            )
        }
    }

    private fun isChannelsConfigValid(
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>
    ) : Boolean {
        if (channelsConfig.size != channelsSize.size) return false
        if (channelsConfig.size != 2) return false
        if (channelsSize[0].channelDescription != HPSFeatureChannelDescription.CURRENT) return false
        return channelsSize[1].channelDescription == HPSFeatureChannelDescription.CURRENT
    }

    private fun encodeSpotFlood(
        config: List<HPSChannelConfig>
    ) : Triple<Boolean, Int, Int> {
        // if at least one channel is adaptive, all channels will be treated like that
        return if (config[0].specialFeature != 0 || config[1].specialFeature != 0) {
            val intensity = config[1].intensity
            Triple(true, intensity, minOf(intensity * 2, 20))
        } else {
            val spot = config[1].intensity
            val flood = config[0].intensity
            Triple(false, spot, flood)
        }
    }

    private fun decodeSpotFlood(
        adaptive: Boolean,
        spot: Int,
        flood: Int? = null
    ) : List<HPSChannelConfig> {
        val config = mutableListOf<HPSChannelConfig>()
        // the output of the flood driver needs to be doubled, because it is not equipped with a 30° lens but with a elliptic 30°*60° lens
        if (adaptive) {
            config.add(HPSChannelConfig(minOf(spot * 2, 20), 1))
            config.add(HPSChannelConfig(spot, 1))
        } else {
            config.add(HPSChannelConfig(flood!!, 0))
            config.add(HPSChannelConfig(spot, 0))
        }

        return config
    }
}