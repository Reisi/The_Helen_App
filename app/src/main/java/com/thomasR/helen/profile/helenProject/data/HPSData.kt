package com.thomasR.helen.profile.helenProject.data

data class HPSData(
    val measurement: HPSMeasurementData? = null,
    val feature: HPSFeatureData? = null,
    val modes: List<List<HPSModeConfig>>? = null,
    val isControlPointEnabled: Boolean? = null,
    val ctrlPtIndication: HPSControlPointIndication? = null
)

data class HPSMeasurementData(
    val mode: Int? = null,
    val outputPower: Float? = null,
    val temperature: Float? = null,
    val inputVoltage: Float? = null,
    val stateOfCharge: Float? = null
)

enum class HPSFeatureChannelDescription {USER, CURRENT, VOLTAGE, PWM, SWITCH}

data class HPSFeatureChannelSize(
    val channelBitSize: Int,
    val specialFeatureBitSize: Int,
    val channelDescription: HPSFeatureChannelDescription
)

data class HPSFeatureBits(
    val modeSetSupported: Boolean = false,
    val searchRequestSupported: Boolean = false,
    val factoryResetSupported: Boolean = false
)

data class HPSFeatureData(
    val modeCount: Int,
    val channelSize: List<HPSFeatureChannelSize>,
    val feature: HPSFeatureBits
)

data class HPSHelenModeConfig(
    val ignored: Boolean,
    val preferred: Boolean,
    val temporary:Boolean,
    val off: Boolean
)

data class HPSChannelConfig(
    val intensity: Int,
    val specialFeature: Int,
)

data class HPSModeConfig(
    val helen: HPSHelenModeConfig,
    val channel: List<HPSChannelConfig>
)

enum class HPSContorlPointOpCode(val id: Int) {
    REQUEST_MODE(1),
    SET_MODE(2),
    REQUEST_SEARCH(3),
    FACTORY_RESET(5),
    RESPONSE_CODE(32)
}

enum class HPSControlPointResponseValue(val id: Int) {
    SUCCESS(1),
    NOT_SUPPORTED(2),
    INVALID_PARAMETER(3),
    OPERATION_FAILED(4)
}

data class HPSControlPointIndication(
    val requestOpCode: HPSContorlPointOpCode,
    val responseValue: HPSControlPointResponseValue,
    val mode: Int? = null
)