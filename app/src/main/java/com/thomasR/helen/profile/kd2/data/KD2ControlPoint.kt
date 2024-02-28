package com.thomasR.helen.profile.kd2.data

import androidx.annotation.FloatRange
import androidx.annotation.IntRange

sealed class KD2ControlPointIndication

enum class KD2ControlPointOpCode(val id: Int) {
    REQUEST_CHANNEL_CONFIG(1),
    SET_CHANNEL_CONFIG(2),
    REQUEST_COM_PIN_CONFIG(3),
    SET_COM_PIN_CONFIG(4),
    REQUEST_INTERNAL_COMP(5),
    SET_INTERNAL_COMP(6),
    REQUEST_EXTERNAL_COMP(7),
    SET_EXTERNAL_COMP(8),
    RESPONSE_CODE(32)
}

enum class KD2ControlPointResponseValue(val id: Int) {
    SUCCESS(1),
    NOT_SUPPORTED(2),
    INVALID_PARAMETER(3),
    OPERATION_FAILED(4)
}

enum class KD2OpticType{NA, SPOT, MEDIUM, FLOOD}

data class KD2ChannelSetup(
    val fullOutputPower: Float = 0f,
    @IntRange(0, 100) val outputLimit: Int = 0,
    val opticType: KD2OpticType = KD2OpticType.NA,
    @FloatRange(-180.0, 180.0) val opticOffset: Float = 0f,
)

enum class KD2ComPinConfig{NOT_USED, COM, BUTTON, PWM}

data class KD2InternalComp(
    @FloatRange(0.0, 2.0) val currentGainFactor: Float,
    @FloatRange(-25.0, 25.0) val temperatureOffset: Float
)

data class KD2ExternalComp(
    @FloatRange(0.0, 2.0) val currentGainFactorLeft: Float,
    @FloatRange(0.0, 2.0) val currentGainFactorRight: Float,
    @FloatRange(-25.0, 25.0) val temperatureOffset: Float
)

object KD2ControlPointResponseHandled: KD2ControlPointIndication()

data class KD2ControlPointCommonResponse(
    val opCode: KD2ControlPointOpCode,
    val responseValue: KD2ControlPointResponseValue
) : KD2ControlPointIndication()

data class KD2ControlPointChannelConfigReceived(
    val channel: Int,
    val channelConfig: KD2ChannelSetup,
) : KD2ControlPointIndication()

data class KD2ControlPointComPinConfigReceived(
    val comPinConfig: KD2ComPinConfig
) : KD2ControlPointIndication()

data class KD2ControlPointInternalCompReceived(
    val compensation: KD2InternalComp
) : KD2ControlPointIndication()

data class KD2ControlPointExternalCompReceived(
    val compensation: KD2ExternalComp
) : KD2ControlPointIndication()

sealed class KD2ControlPointCommand

data class KD2ControlPointRequestChannelConfig(
    val channel: Int
) : KD2ControlPointCommand()

data class KD2ControlPointSetChannelConfig(
    val channel: Int,
    val channelConfig: KD2ChannelSetup
) : KD2ControlPointCommand()

object KD2ControlPointRequestComPinConfig : KD2ControlPointCommand()

data class KD2ControlPointSetComPinConfig(
    val comPinConfig: KD2ComPinConfig
) : KD2ControlPointCommand()

object KD2ControlPointRequestInternalComp : KD2ControlPointCommand()

data class KD2ControlPointSetInternalComp(
    val internalComp: KD2InternalComp
) : KD2ControlPointCommand()

object KD2ControlPointRequestExternalComp : KD2ControlPointCommand()

data class KD2ControlPointSetExternalComp(
    val externalComp: KD2ExternalComp
) : KD2ControlPointCommand()