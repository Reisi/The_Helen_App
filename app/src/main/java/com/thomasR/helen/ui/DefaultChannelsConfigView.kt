package com.thomasR.helen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thomasR.helen.R
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelDescription
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize

class DefaultChannelsConfigView(
    override val profiles: List<String> = emptyList()
): ChannelsConfigView {

    @Composable
    override fun ChannelsView(
        profile: Int?,
        channelsConfig: List<HPSChannelConfig>,
        channelsSize: List<HPSFeatureChannelSize>,
        modifier: Modifier
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (channelsConfig.isEmpty() && channelsSize.isEmpty())
                Text(stringResource(id = R.string.channel_config_na))
            else if (channelsConfig.size != channelsSize.size)
                Text(stringResource(id = R.string.channel_config_invalid))
            else
                for (i in channelsConfig.indices) {ChannelElement(channelsConfig[i], channelsSize[i])}
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
        val validList = remember { mutableListOf<Boolean>() }
        // initialize valid list
        if (validList.isEmpty()) {
            for (i in channelsConfig.indices) validList.add(true)
        }

        for (i in channelsConfig.indices) {
            ChannelConfig(
                modifier = modifier,//.padding(horizontal = dimensionResource(id = R.dimen.padding_medium)),
                channelConfig = channelsConfig[i],
                channelSize = channelsSize[i]
            ) {config, valid ->
                val newConfig = channelsConfig.toMutableList()
                newConfig[i] = config
                validList[i] = valid
                var isValid = true
                for (j in validList.indices) {
                    if (!validList[j]) {
                        isValid = false
                        break;
                    }
                }
                onChanged(newConfig, isValid)
            }
            if (i != channelsConfig.size)
                Spacer(modifier.height(dimensionResource(id = R.dimen.padding_medium)))
        }
    }

    @Composable
    private fun ChannelElement(
        channelConfig: HPSChannelConfig,
        channelSize: HPSFeatureChannelSize,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            val intensity = channelConfig.intensity.toString()
            val icon = when (channelSize.channelDescription) {
                HPSFeatureChannelDescription.VOLTAGE -> ImageVector.vectorResource(id = R.drawable.voltage_icon)
                HPSFeatureChannelDescription.CURRENT -> ImageVector.vectorResource(id = R.drawable.current_icon)
                HPSFeatureChannelDescription.PWM -> ImageVector.vectorResource(id = R.drawable.pwm_icon)
                //HPSFeatureChannelDescription.SWITCH -> "S"
                else -> ImageVector.vectorResource(id = R.drawable.dummy_icon)
            }
            Icon(icon, null, Modifier.size(32.dp))
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = intensity,
            )
        }
    }

    @Composable
    private fun ChannelConfig(
        modifier: Modifier = Modifier,
        channelConfig: HPSChannelConfig,
        channelSize: HPSFeatureChannelSize,
        onChanged: (HPSChannelConfig, Boolean) -> Unit
    ) {
        var featureString by rememberSaveable { mutableStateOf(channelConfig.specialFeature.toString()) }
        var intensityString by rememberSaveable { mutableStateOf(channelConfig.intensity.toString()) }
        val channelTypes = stringArrayResource(id = R.array.channel_types).toList()
        val bitSizeFeature = channelSize.specialFeatureBitSize
        val bitSizeIntensity = channelSize.channelBitSize - channelSize.specialFeatureBitSize

        Column(modifier = modifier.fillMaxWidth()) {

            Text(text = channelTypes[channelSize.channelDescription.ordinal], modifier = modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = featureString,
                    onValueChange = {
                        featureString = it
                        val (config, valid) = isConfigValid(featureString, bitSizeFeature, intensityString, bitSizeIntensity)
                        onChanged(config, valid)
                        /*if (isFeatureValid(featureString, bitSizeFeature) &&
                            isIntensityValid(intensityString, bitSizeIntensity))
                            onChanged(HPSChannelConfig(intensityString.toInt(), featureString.toInt()))*/
                    },
                    label = { Text(stringResource(R.string.feature)) },
                    isError = !isFeatureValid(featureString, bitSizeFeature),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                )
                OutlinedTextField(
                    value = intensityString,
                    onValueChange = {
                        intensityString = it
                        val (config, valid) = isConfigValid(featureString, bitSizeFeature, intensityString, bitSizeIntensity)
                        onChanged(config, valid)
                        /*if (isFeatureValid(featureString, bitSizeFeature) &&
                            isIntensityValid(intensityString, bitSizeIntensity))
                            onChanged(HPSChannelConfig(intensityString.toInt(), featureString.toInt()))*/
                    },
                    label = { Text(stringResource(R.string.intensity)) },
                    isError = !isIntensityValid(intensityString, bitSizeIntensity),
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

    private fun isConfigValid(
        featureString: String,
        featureBitSize: Int,
        intensityString: String,
        intensityBitSize: Int
    ) : Pair<HPSChannelConfig, Boolean> {
        val featureMax = (1 shl featureBitSize) - 1
        val intensityMax = (1 shl intensityBitSize) - 1
        var valid = true;
        var feature = featureString.toIntOrNull()
        var intensity = intensityString.toIntOrNull()
        if (feature == null || feature < 0) { feature = 0; valid = false }
        if (feature > featureMax) { feature = featureMax; valid = false }
        if (intensity == null || intensity < 0) { intensity = 0; valid = false }
        if (intensity > intensityMax) { intensity = intensityMax; valid = false }
        return Pair(HPSChannelConfig(intensity, feature), valid)
    }

    private fun isFeatureValid(feature: String, featureBitSize: Int) : Boolean {
        val value = feature.toIntOrNull() ?: return false
        if (value < 0) return false
        if (value >= 1 shl featureBitSize) return false
        return true
    }

    private fun isIntensityValid(intensity: String, intensityBitSize: Int) : Boolean {
        val value = intensity.toIntOrNull() ?: return false
        if (value < 0) return false
        if (value >= 1 shl intensityBitSize) return false
        return true
    }
}