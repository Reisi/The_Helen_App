package com.thomasR.helen.profile.kd2

import android.content.Context
import com.thomasR.helen.R
import com.thomasR.helen.profile.kd2.data.KD2ChannelSetup
import com.thomasR.helen.profile.kd2.data.KD2ComPinConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointIndication
import com.thomasR.helen.profile.kd2.data.KD2ControlPointOpCode
import com.thomasR.helen.profile.kd2.data.KD2ControlPointResponseValue
import com.thomasR.helen.profile.kd2.data.KD2ExternalComp
import com.thomasR.helen.profile.kd2.data.KD2InternalComp
import com.thomasR.helen.profile.kd2.data.KD2OpticType
import com.thomasR.helen.profile.kd2.data.KD2ControlPointChannelConfigReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointComPinConfigReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointCommonResponse
import com.thomasR.helen.profile.kd2.data.KD2ControlPointExternalCompReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointImuCalibrationStateReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointInternalCompReceived
import no.nordicsemi.android.kotlin.ble.core.data.util.DataByteArray
import no.nordicsemi.android.kotlin.ble.core.data.util.IntFormat

class KD2ControlPointDataParser {
    fun decode(bytes: DataByteArray) : KD2ControlPointIndication? {
        if (bytes.size < 3) return null
        if (bytes.value[0].toInt() != KD2ControlPointOpCode.RESPONSE_CODE.id) return null

        val opCode = decodeOpCode(bytes.value[1].toInt()) ?: return null
        val responseValue = decodeResponseValue(bytes.value[2].toInt()) ?: return null

        if (responseValue != KD2ControlPointResponseValue.SUCCESS)
            return KD2ControlPointCommonResponse(opCode, responseValue)

        // TODO: length check
        when(opCode) {
            KD2ControlPointOpCode.REQUEST_CHANNEL_CONFIG ->
                return KD2ControlPointChannelConfigReceived(
                    bytes.value[3].toInt(),
                    decodeChannelConfig(bytes.copyOfRange(4, bytes.value.size))
                )
            KD2ControlPointOpCode.REQUEST_COM_PIN_CONFIG ->
                return KD2ControlPointComPinConfigReceived(
                    decodeComPinConfig(bytes.value[3])
                )
            KD2ControlPointOpCode.REQUEST_INTERNAL_COMP ->
                return KD2ControlPointInternalCompReceived(
                    decodeInternalComp(bytes.copyOfRange(3, bytes.value.size))
                )
            KD2ControlPointOpCode.REQUEST_EXTERNAL_COMP ->
                return KD2ControlPointExternalCompReceived(
                    decodeExternalComp(bytes.copyOfRange(3, bytes.value.size))
                )
            KD2ControlPointOpCode.REQUEST_IMU_CALIBRATION_STATE ->
                return KD2ControlPointImuCalibrationStateReceived(
                    decodeImuCalibrationState(bytes.copyOfRange(3, bytes.value.size))
                )
            else -> return KD2ControlPointCommonResponse(opCode, responseValue)
        }
    }

    fun getControlPointCommonResponseMessage(context: Context, response: KD2ControlPointCommonResponse): String {
        val opCode: String?
        val responseValue: String?
        try {
            opCode =
                context.resources.getStringArray(R.array.KD2_op_codes).toList()[response.opCode.id - 1]
        } finally { }
        try {
            responseValue =
                context.resources.getStringArray(R.array.KD2_response_values).toList()[response.responseValue.id - 1]
        } finally { }
        return if (opCode == null || responseValue == null)
            context.resources.getString(R.string.KD2_unknown_response)
        else
            "$opCode $responseValue"
    }

    fun encodeChannelConfig(config: KD2ChannelSetup) : ByteArray {
        val power = getPower(config.fullOutputPower)
        val limit = getLimit(config.outputLimit)
        val optic = getOptic(config.opticType)
        val offset = getOffset(config.opticOffset)

        return byteArrayOf(
            power.toByte(), power.toInt().shr(8).toByte(),
            limit.toByte(),
            optic.toByte(),
            offset.toByte(), offset.toInt().shr(8).toByte()
        )
    }

    fun encodeComPinConfig(config: KD2ComPinConfig) : ByteArray {
        return byteArrayOf(config.ordinal.toByte())
    }

    fun encodeInternalComp(comp: KD2InternalComp) : ByteArray {
        val gain = getInternalCurrentGain(comp.currentGainFactor)
        val offset = getInternalTempOffset(comp.temperatureOffset)

        return byteArrayOf(
            0xFF.toByte(), 0x7F, 0x00, 0x00,
            gain.toByte(), gain.toInt().shr(8).toByte(), 0x00, 0x00,
            0xFF.toByte(), 0x7F, offset.toByte(), offset.toInt().shr(8).toByte()
        )
    }

    fun encodeExternalComp(comp: KD2ExternalComp) : ByteArray {
        val leftGain = getExternalCurrentGain(comp.currentGainFactorLeft)
        val rightGain = getExternalCurrentGain(comp.currentGainFactorRight)
        val offset = getExternalTempOffset(comp.temperatureOffset)

        return byteArrayOf(
            offset.toByte(), offset.toInt().shr(8).toByte(),
            leftGain.toByte(),
            rightGain.toByte()
        )
    }

    private fun decodeOpCode(opCode: Int) : KD2ControlPointOpCode? {
        for (oc in KD2ControlPointOpCode.values()) {
            if (oc.id == opCode) return oc
        }
        return null
    }

    private fun decodeResponseValue(response: Int) : KD2ControlPointResponseValue? {
        for (rv in KD2ControlPointResponseValue.values()) {
            if (rv.id == response) return rv
        }
        return null
    }

    private fun decodeChannelConfig(bytes: DataByteArray) : KD2ChannelSetup {
        var power = 0.toUShort()
        var limit = 0.toUByte()
        var type = 0.toUByte()
        var offset = 0.toShort()

        if (bytes.size == 6) {
            power = bytes.getIntValue(IntFormat.FORMAT_UINT16_LE, 0)!!.toUShort()
            limit = bytes.getIntValue(IntFormat.FORMAT_UINT8, 2)!!.toUByte()
            type = bytes.getIntValue(IntFormat.FORMAT_UINT8, 3)!!.toUByte()
            offset = bytes.getIntValue(IntFormat.FORMAT_SINT16_LE, 4)!!.toShort()
        }
        return(KD2ChannelSetup(getPower(power), getLimit(limit), getOptic(type), getOffset(offset)))
    }

    private fun decodeComPinConfig(byte: Byte) : KD2ComPinConfig {
        return byte.toUByte().toEnum<KD2ComPinConfig>() ?: KD2ComPinConfig.NOT_USED
    }

    private fun decodeInternalComp(bytes: DataByteArray) : KD2InternalComp {
        val gain = bytes.getIntValue(IntFormat.FORMAT_UINT16_LE, 4)!!.toUShort()
        val offset = bytes.getIntValue(IntFormat.FORMAT_SINT16_LE, 10)!!.toShort()

        return KD2InternalComp(getInternalCurrentGain(gain), getInternalTempOffset(offset))
    }

    private fun decodeExternalComp(bytes: DataByteArray) : KD2ExternalComp {
        val offset = bytes.getIntValue(IntFormat.FORMAT_SINT16_LE, 0)!!.toShort()
        val leftGain = bytes.getIntValue(IntFormat.FORMAT_UINT8, 2)!!.toUByte()
        val rightGain = bytes.getIntValue(IntFormat.FORMAT_UINT8, 3)!!.toUByte()

        return KD2ExternalComp(
            getExternalCurrentGain(leftGain),
            getExternalCurrentGain(rightGain),
            getExternalTempOffset(offset)
        )
    }

    private fun decodeImuCalibrationState(bytes: DataByteArray) : Boolean {
        return bytes.getIntValue(IntFormat.FORMAT_UINT8, 0) != 0
    }

    private fun getPower(raw: UShort) : Float { return raw.toFloat() / 1000f }
    private fun getLimit(raw: UByte) : Int { return if (raw <= 100u) raw.toInt() else 100 }
    private fun getOptic(raw: UByte) : KD2OpticType { return raw.toEnum<KD2OpticType>() ?: KD2OpticType.NA }
    private fun getOffset(raw: Short) : Float { return raw.toFloat() / 100f }

    private fun getInternalCurrentGain(raw: UShort) : Float { return raw.toFloat() / 32768f }
    private fun getInternalTempOffset(raw: Short) : Float { return raw.toFloat() / 128f }

    private fun getExternalTempOffset(raw: Short) : Float { return raw.toFloat() / 4f }
    private fun getExternalCurrentGain(raw: UByte) : Float { return raw.toFloat() / 128f }

    private fun getPower(power: Float) : UShort { return (power * 1000f).toInt().toUShort() }
    private fun getLimit(limit: Int) : UByte { return limit.toUByte() }
    private fun getOptic(type: KD2OpticType) : UByte {return type.ordinal.toUByte() }
    private fun getOffset(offset: Float) : Short { return (offset * 100f).toInt().toShort() }

    private fun getInternalCurrentGain(gain: Float) : UShort { return (gain * 32768f).toInt().toUShort() }
    private fun getInternalTempOffset(offset: Float) : Short { return (offset * 128f).toInt().toShort() }

    private fun getExternalTempOffset(offset: Float) : Short { return (offset * 4f).toInt().toShort() }
    private fun getExternalCurrentGain(gain: Float) : UByte { return (gain * 128f).toInt().toUByte() }

    //Int to Enum
    inline fun <reified T : Enum<T>> UByte.toEnum(): T? {
        return enumValues<T>().firstOrNull { it.ordinal.toUByte() == this }
    }
}