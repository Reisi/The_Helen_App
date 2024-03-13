package com.thomasR.helen.profile.helenProject

import com.thomasR.helen.profile.helenProject.data.HPSFeatureBits
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelDescription
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize
import com.thomasR.helen.profile.helenProject.data.HPSFeatureData
import no.nordicsemi.android.common.core.DataByteArray
import no.nordicsemi.android.common.core.IntFormat

class HPSFeatureDataParser {
    fun parse(bytes: DataByteArray) : HPSFeatureData? {
        if (bytes.size < 4) return null

        var offset = 0

        val modeCnt = bytes.getIntValue(IntFormat.FORMAT_UINT8, offset)!!
        offset += 1

        val channelCnt = bytes.getIntValue(IntFormat.FORMAT_UINT8, offset)!!
        offset += 1

        if (bytes.size < (4 + channelCnt * 2)) return null

        val channelSize = mutableListOf<HPSFeatureChannelSize>()
        for (i in 0 until channelCnt) {
            val chnsz = bytes.getIntValue(IntFormat.FORMAT_UINT16_LE, offset)!!
            offset += 2

            channelSize.add(convertChannelSize(chnsz))
        }

        val feature = bytes.getIntValue(IntFormat.FORMAT_UINT16_LE, offset)

        return HPSFeatureData(modeCnt, channelSize, convertFeature(feature))
    }

    private fun convertChannelSize(raw: Int): HPSFeatureChannelSize {
        val channelSize = raw and 0xFF
        val spftSize = (raw shr(8)) and 0xF
        val desc = (raw shr(12)).toEnum<HPSFeatureChannelDescription>() ?: HPSFeatureChannelDescription.USER

        return HPSFeatureChannelSize(channelSize, spftSize, desc)
    }

    private fun convertFeature(raw: Int?): HPSFeatureBits {
        return if (raw == null)
            HPSFeatureBits()
        else HPSFeatureBits(
            modeSetSupported = (raw and 0x0001) != 0,
            searchRequestSupported = (raw and 0x0002) != 0,
            factoryResetSupported = (raw and 0x0004) != 0,
            modeOverrideSupported = (raw and 0x0008) != 0
        )
    }

    //Int to Enum
    inline fun <reified T : Enum<T>> Int.toEnum(): T? {
        return enumValues<T>().firstOrNull { it.ordinal == this }
    }
}