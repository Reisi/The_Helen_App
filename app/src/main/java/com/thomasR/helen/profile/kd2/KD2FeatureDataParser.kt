package com.thomasR.helen.profile.kd2

import com.thomasR.helen.profile.kd2.data.KD2ChannelFeature
import com.thomasR.helen.profile.kd2.data.KD2ConfigFeatures
import com.thomasR.helen.profile.kd2.data.KD2Feature
import no.nordicsemi.android.common.core.DataByteArray
import kotlin.experimental.and

class KD2FeatureDataParser {
    fun parse(bytes: DataByteArray) : KD2Feature? {
        if (bytes.size < 2) return null

        val channelConfig = (bytes.value[0] and 0x01).toInt() != 0
        val comPin = (bytes.value[0] and 0x02).toInt() != 0
        val internalComp = (bytes.value[0]  and 0x04).toInt() != 0
        val externalComp = (bytes.value[0] and 0x08).toInt() != 0

        val adaptive = (bytes.value[1] and 0x01).toInt() != 0

        return KD2Feature(
            configFeatures = KD2ConfigFeatures(channelConfig, comPin, internalComp, externalComp),
            channelFeature = KD2ChannelFeature(adaptive)
        )
    }
}