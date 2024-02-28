package com.thomasR.helen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

fun getKD2ChannelConfigView(
    channelsSize: List<HPSFeatureChannelSize>?,
    features: KD2ChannelFeature
) : ChannelsConfigView {
    val description = channelsSize?.getOrNull(2)
    return if (description != null && description.channelDescription == HPSFeatureChannelDescription.CURRENT)
        KD23ChannelsConfigView(features = features)
    else
        KD21ChannelsConfigView(features = features)
}

class KD21ChannelsConfigView(
    override val profiles: List<String> = listOf("SpottyHelen"),
    val features: KD2ChannelFeature
): ChannelsConfigView {

    @Composable
    override fun ChannelsView(
        profile: Int?,
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier
    ) {
        if (profile != 0) {
            DefaultChannelsConfigView().ChannelsView(
                profile = null,
                channelsConfig = channelsConfig,
                channelsSize = channelsSize,
                modifier = modifier
            )
            return
        }
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            if (!isChannelsConfigValid(channelsConfig, channelsSize)) {
                Text(stringResource(id = R.string.kd2_unexpected))
                return
            }
            for (i in channelsConfig.indices) {
                val icon = if (i == 0) {
                    if (channelsConfig[i].specialFeature == 1)
                        ImageVector.vectorResource(R.drawable.adaptive_beam)
                    else
                        ImageVector.vectorResource(R.drawable.high_beam)
                }
                else ImageVector.vectorResource(R.drawable.pwm_icon)
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier.size(32.dp)
                    )
                    Text(
                        text = "%3d%%".format(channelsConfig[i].intensity * 5)
                    )
                }
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
        if (profile != 0) {
            DefaultChannelsConfigView().ChannelsConfig(
                profile = null,
                channelsConfig = channelsConfig,
                channelsSize = channelsSize,
                modifier = modifier,
                onChanged = onChanged
            )
            return
        }
        Column(modifier = modifier/*.padding(horizontal = dimensionResource(id = R.dimen.padding_medium))*/) {
            if (!isChannelsConfigValid(channelsConfig, channelsSize)){
                Text(stringResource(id = R.string.kd2_unexpected))
                return
            }
            // led channel is always available
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (features.adaptiveSupported) {
                    IconToggleButton(
                        checked = channelsConfig[0].specialFeature != 0,
                        onCheckedChange = {
                            val newConfig = channelsConfig.toMutableList()
                            newConfig[0] = channelsConfig[0].copy(specialFeature = if (it) 1 else 0)
                            onChanged(newConfig, true)
                        }
                    ) {
                        val icon = if (channelsConfig[0].specialFeature != 0)
                            ImageVector.vectorResource(R.drawable.adaptive_beam)
                        else
                            ImageVector.vectorResource(R.drawable.adaptive_beam_disabled)
                        Icon(icon, null)
                    }
                } else Spacer(Modifier.size(48.dp))
                Column {
                    Text(
                        text = stringResource(R.string.kd2_led_channel) +" " +
                                (channelsConfig[0].intensity * 5).toString() + "%",
                        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small))
                    )
                    Slider(
                        value = channelsConfig[0].intensity.toFloat(),
                        onValueChange = {
                            val newConfig = channelsConfig.toMutableList()
                            newConfig[0] = channelsConfig[0].copy(intensity = it.toInt())
                            onChanged(newConfig, true)
                        },
                        steps = 19,
                        valueRange = 0f..20f
                    )
                }
            }

            if (channelsConfig.size >= 2) {
                Row {
                    Spacer(Modifier.size(48.dp))
                    //Icon(ImageVector.vectorResource(R.drawable.dummy_icon), null)
                    Column {
                        Text(
                            text = stringResource(id = R.string.kd2_pwm_channel) + " " +
                                    (channelsConfig[1].intensity * 5).toString() + "%",
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

            if (channelsConfig.size == 3) {
                Row {
                    //Icon(ImageVector.vectorResource(R.drawable.dummy_icon), null)
                    Spacer(Modifier.size(48.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.kd2_com_channel) + " " +
                                    (channelsConfig[2].intensity * 5).toString() + "%",
                            modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small))
                        )
                        Slider(
                            value = channelsConfig[2].intensity.toFloat(),
                            onValueChange = {
                                val newConfig = channelsConfig.toMutableList()
                                newConfig[2] = channelsConfig[2].copy(intensity = it.toInt())
                                onChanged(newConfig, true)
                            },
                            steps = 19,
                            valueRange = 0f..20f
                        )
                    }
                }
            }
        }
    }

    private fun isChannelsConfigValid(
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>
    ) : Boolean {
        if (channelsConfig.size != channelsSize.size) return false
        if (channelsConfig.size > 3 || channelsConfig.size < 1) return false
        if (channelsSize[0].channelDescription != HPSFeatureChannelDescription.CURRENT) return false
        if (channelsSize.size >= 2 && channelsSize[1].channelDescription != HPSFeatureChannelDescription.PWM) return false
        return !(channelsSize.size == 3 && channelsSize[2].channelDescription != HPSFeatureChannelDescription.PWM)

        // TODO: maybe check also channel feature and bitsize?

    }
}

class KD23ChannelsConfigView(
    override val profiles: List<String> = listOf("SuperHelen"),
    val features: KD2ChannelFeature
) : ChannelsConfigView {

    @Composable
    override fun ChannelsView(
        profile: Int?,
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier
    ) {
        if (profile != 0) {
            DefaultChannelsConfigView().ChannelsView(
                profile = null,
                channelsConfig = channelsConfig,
                channelsSize = channelsSize,
                modifier = modifier
            )
            return
        }
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            if (!isChannelsConfigValid(channelsConfig, channelsSize)) {
                Text(stringResource(id = R.string.kd2_unexpected))
                return
            }

            val (adaptive, spot, flood) = encodeSpotFlood(channelsConfig)
            if (adaptive) {
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.adaptive_beam),
                        contentDescription = null,
                        modifier.size(32.dp)
                    )
                    Text(
                        text = "%3d%%".format(spot * 5),
                        modifier = Modifier.fillMaxHeight()
                    )
                }

            } else {
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.high_beam),
                        contentDescription = null,
                        modifier.size(32.dp)
                    )
                    Text(
                        text = "%3d%%".format(spot * 5),
                        modifier = Modifier.fillMaxHeight()
                    )
                }
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.flood_beam),
                        contentDescription = null,
                        modifier.size(32.dp)
                    )
                    Text(
                        text = "%3d%%".format(flood * 5),
                        modifier = Modifier.fillMaxHeight()
                    )
                }

            }
            // PWM channels ?
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
        if (profile != 0) {
            DefaultChannelsConfigView().ChannelsConfig(
                profile = null,
                channelsConfig = channelsConfig,
                channelsSize = channelsSize,
                modifier = modifier,
                onChanged = onChanged
            )
            return
        }
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
                            config.addAll(channelsConfig.drop(3))
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
                        config.addAll(channelsConfig.drop(3))
                        onChanged(config, true)
                    }
                } else {
                    Row {
                        ChannelView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            name = stringResource(id = R.string.kd2_spot_channel),
                            value = spot.toFloat()
                        ) {
                            val config = decodeSpotFlood(false, it.toInt(), flood).toMutableList()
                            config.addAll(channelsConfig.drop(3))
                            onChanged(config, true)
                        }
                        ChannelView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            name = stringResource(id = R.string.kd2_flood_channel),
                            value = flood.toFloat()
                        ) {
                            val config = decodeSpotFlood(false, spot, it.toInt()).toMutableList()
                            config.addAll(channelsConfig.drop(3))
                            onChanged(config, true)
                        }
                    }
                }
            }

            // optional PWM channels
            if (channelsConfig.size >= 4) {
                Row {
                    //Icon(ImageVector.vectorResource(R.drawable.dummy_icon), null)
                    Spacer(Modifier.size(48.dp))
                    ChannelView(
                        name = stringResource(id = R.string.kd2_pwm_channel),
                        value = channelsConfig[3].intensity.toFloat()
                    ) {
                        val config = channelsConfig.toMutableList()
                        config[3] = HPSChannelConfig(it.toInt(), 0)
                        onChanged(config, true)
                    }
                }
            }
            if (channelsConfig.size >= 5) {
                Row {
                    //Icon(ImageVector.vectorResource(R.drawable.dummy_icon), null)
                    Spacer(Modifier.size(48.dp))
                    ChannelView(
                        name = stringResource(id = R.string.kd2_com_channel),
                        value = channelsConfig[4].intensity.toFloat()
                    ) {
                        val config = channelsConfig.toMutableList()
                        config[4] = HPSChannelConfig(it.toInt(), 0)
                        onChanged(config, true)
                    }
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
        if (channelsConfig.size > 5 || channelsConfig.size < 3) return false
        for (i in channelsSize.indices) {
            if (i < 3) {
                if (channelsSize[i].channelDescription != HPSFeatureChannelDescription.CURRENT) return false
            } else {
                if (channelsSize[i].channelDescription != HPSFeatureChannelDescription.PWM) return false
            }
        }

        // TODO: maybe check also channel feature and bitsize?

        return true
    }

    private fun encodeSpotFlood(
        config: List<HPSChannelConfig>
    ) : Triple<Boolean, Int, Int> {
        // if at least one channel is adaptive, all channels will be treated like that
        return if (config[0].specialFeature != 0 || config[1].specialFeature != 0 || config[2].specialFeature != 0) {
            // in adaptive mode the settings is determined by the brightest spot
            // TODO create mapping table, for now just the spot is returned
            val intensity = maxOf(config[0].intensity, config[2].intensity)
            Triple(true, intensity, intensity)
        } else {
            // in non adaptive mode the brightest spot defines the spot overall
            val spot = maxOf(config[0].intensity, config[2].intensity)
            val flood = config[1].intensity
            Triple(false, spot, flood)
        }
    }

    private fun decodeSpotFlood(
        adaptive: Boolean,
        spot: Int,
        flood: Int? = null
    ) : List<HPSChannelConfig> {
        val config = mutableListOf<HPSChannelConfig>()

        if (adaptive) {
            // TODO create mapping table, for now just functional dummy
            config.add(HPSChannelConfig(spot, 1))
            config.add(HPSChannelConfig(spot / 2, 1))
            config.add(HPSChannelConfig(spot, 1))
        } else {
            config.add(HPSChannelConfig(spot, 0))
            config.add(HPSChannelConfig(flood!!, 0))
            config.add(HPSChannelConfig(spot, 0))
        }

        return config
    }
}
