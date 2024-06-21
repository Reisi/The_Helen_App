package com.thomasR.helen.profile.kd2.data

data class KD2Data(
    val feature: KD2Feature = KD2Feature(),
    val controlPointData: KD2ControlPointData = KD2ControlPointData()
)

data class KD2Feature(
    val configFeatures: KD2ConfigFeatures = KD2ConfigFeatures(),
    val channelFeature: KD2ChannelFeature = KD2ChannelFeature()
)

data class KD2ConfigFeatures(
    val channelConfigSupported: Boolean = false,
    val comPinModeSupported: Boolean = false,
    val internalCompensationSupported: Boolean = false,
    val externalCompensationSupported: Boolean = false,
    val imuCalibrationSupported: Boolean = false,
)

data class KD2ChannelFeature(
    val adaptiveSupported: Boolean = false
)

data class KD2ControlPointData(
    val channelConfigs: List<KD2ChannelSetup> = emptyList(),
    val comPinConfig: KD2ComPinConfig? = null,
    val internalComp: KD2InternalComp? = null,
    val externalComp: KD2ExternalComp? = null,
    val isImuCalibrated: Boolean? = null
)

