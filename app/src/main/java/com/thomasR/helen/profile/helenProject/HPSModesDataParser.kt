package com.thomasR.helen.profile.helenProject

import android.content.Context
import com.thomasR.helen.R
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize
import com.thomasR.helen.profile.helenProject.data.HPSFeatureData
import com.thomasR.helen.profile.helenProject.data.HPSHelenModeConfig
import com.thomasR.helen.profile.helenProject.data.HPSModeConfig
import com.thomasR.helen.profile.helenProject.data.WriteModesResponse
import no.nordicsemi.android.common.core.DataByteArray
import no.nordicsemi.android.common.core.IntFormat
import kotlin.experimental.or

class HPSModesDataParser {
    fun getWriteResponseMessage(
        context: Context,
        response: WriteModesResponse
    ) : String {
        return if (response.success)
            context.resources.getString(R.string.write_mode_success)
        else
            context.resources.getString(R.string.write_mode_failed)
    }

    fun decode(bytes: DataByteArray, features: HPSFeatureData) : List<List<HPSModeConfig>>? {
        if (bytes.size != getRequiredSize(features)) return null

        // First read out channel configuration. The generated list will be a list of channels each
        // containing a list of modes
        var channelOffset = features.modeCount * 2
        val channelList = mutableListOf<List<HPSChannelConfig>>()
        for (channelSize in features.channelSize) {
            val channelByteSize = channelSize.channelBitSize * features.modeCount / 8
            channelList.add(
                decodeChannel(
                    bytes = bytes.copyOfRange(channelOffset, channelOffset + channelByteSize),
                    modeCnt = features.modeCount,
                    channelSize = channelSize
                )
            )
            channelOffset += channelByteSize
        }

        var modeOffset = 0
        val list = mutableListOf<List<HPSModeConfig>>()
        var group: MutableList<HPSModeConfig>? = null
        for (i in 0 until features.modeCount) {
            val raw = bytes.getIntValue(IntFormat.FORMAT_UINT16_LE, modeOffset)!!
            modeOffset += 2

            if (isFirstInGroup(raw)){
                group = mutableListOf<HPSModeConfig>()
            }
            if (group == null) return null  // invalid configuration

            val helenMode = decodeHelenMode(raw)
            val channels = mutableListOf<HPSChannelConfig>()
            for(channel in channelList) {
                channels.add(channel[i])
            }

            group.add(HPSModeConfig(helenMode, channels))

            if (isLastInGroup(raw)) {
                list.add(group)
                group = null
            }
        }

        return list
    }


    private fun getRequiredSize(features: HPSFeatureData): Int {
        var requiredSize = features.modeCount * 2
        for (channel in features.channelSize) {
            val channelBitSize = features.modeCount * channel.channelBitSize
            requiredSize += channelBitSize / 8
        }

        return requiredSize
    }

    private fun isFirstInGroup(raw: Int): Boolean {
        return (raw and 0x0002) != 0
    }

    private fun isLastInGroup(raw: Int): Boolean {
        return (raw and 0x0004) != 0
    }

    private fun decodeHelenMode(raw: Int): HPSHelenModeConfig {
        return HPSHelenModeConfig(
            ignored = (raw and 0x0001) != 0,
            preferred = (raw and 0x0008) != 0,
            temporary = (raw and 0x0010) != 0,
            off = (raw and 0x0020) != 0
        )
    }

    // for now, only works if channel size represents one byte (bitsize 8)
    private fun decodeChannel(
        bytes: DataByteArray, modeCnt: Int, channelSize: HPSFeatureChannelSize
    ): List<HPSChannelConfig> {
        val channelModes = mutableListOf<HPSChannelConfig>()
        var offset = 0
        for(i in 0 until modeCnt) {
            val rawChannelMode = bytes.getIntValue(IntFormat.FORMAT_UINT8, offset)!!
            offset += channelSize.channelBitSize / 8

            channelModes.add(decodeChannelMode(rawChannelMode, channelSize))
        }

        return channelModes
    }

    private fun decodeChannelMode(raw: Int, channelSize: HPSFeatureChannelSize): HPSChannelConfig {
        val intensity = raw and ((1 shl(channelSize.channelBitSize - channelSize.specialFeatureBitSize)) - 1)
        val spft = raw shr(channelSize.channelBitSize - channelSize.specialFeatureBitSize)

        return HPSChannelConfig(intensity, spft)
    }

    fun encode(config: List<List<HPSModeConfig>>, features: HPSFeatureData) : DataByteArray {
        var bytes = encodeHelenModes(config)
        bytes += encodeChannels(config, features)
        return DataByteArray(bytes)
    }

    private fun encodeHelenModes(modes: List<List<HPSModeConfig>>) : ByteArray {
        var bytes = byteArrayOf()

        for (group in modes) {
            for (i in group.indices) {
                var encoded: UShort = 0u
                val mode = group[i].helen

                if (mode.ignored) encoded = encoded or 0x0001u
                if (i == 0) encoded = encoded or 0x0002u
                if (i == group.lastIndex) encoded = encoded or 0x0004u
                if (mode.preferred) encoded = encoded or 0x0008u
                if (mode.temporary) encoded = encoded or 0x0010u
                if (mode.off) encoded = encoded or 0x0020u

                bytes += byteArrayOf(
                    (encoded and 0xFFu).toByte(),
                    (encoded.toUInt().shr(8) and 0xFFu).toByte()
                )
            }
        }

        return bytes
    }

    private fun encodeChannels(channels: List<List<HPSModeConfig>>, features: HPSFeatureData) : ByteArray {
        val channelCnt = channels[0][0].channel.size
        var bytes = byteArrayOf()

        for (channel in 0 until channelCnt) {
            for (group in channels) {
                for (mode in group) {
                    bytes += encode8bitChannel(mode.channel[channel], features.channelSize[channel])
                }
            }
        }

        return bytes
    }

    private fun encode8bitChannel(config: HPSChannelConfig, channelSize: HPSFeatureChannelSize) : Byte {
        var byte = config.intensity.toByte()
        byte = byte or config.specialFeature.shl(channelSize.channelBitSize - channelSize.specialFeatureBitSize).toByte()

        return byte
    }
}