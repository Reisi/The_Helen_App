package com.thomasR.helen.demo

import com.thomasR.helen.data.HelenData
import com.thomasR.helen.profile.deviceInformation.data.DeviceInformationData
import com.thomasR.helen.profile.helenProject.data.HPSChannelConfig
import com.thomasR.helen.profile.helenProject.data.HPSData
import com.thomasR.helen.profile.helenProject.data.HPSFeatureBits
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelDescription
import com.thomasR.helen.profile.helenProject.data.HPSFeatureChannelSize
import com.thomasR.helen.profile.helenProject.data.HPSFeatureData
import com.thomasR.helen.profile.helenProject.data.HPSHelenModeConfig
import com.thomasR.helen.profile.helenProject.data.HPSMeasurementData
import com.thomasR.helen.profile.helenProject.data.HPSModeConfig
import com.thomasR.helen.profile.kd2.data.KD2ChannelFeature
import com.thomasR.helen.profile.kd2.data.KD2ChannelSetup
import com.thomasR.helen.profile.kd2.data.KD2ComPinConfig
import com.thomasR.helen.profile.kd2.data.KD2ConfigFeatures
import com.thomasR.helen.profile.kd2.data.KD2ControlPointData
import com.thomasR.helen.profile.kd2.data.KD2Data
import com.thomasR.helen.profile.kd2.data.KD2Feature
import com.thomasR.helen.profile.kd2.data.KD2OpticType

class DummyDevices {
    val devices: List<DummyData> = listOf(
        DummyData(
            helenData = HelenData(
                name = "Helen",
                address = "just Helen board",
                isHelenProjectSupported = true,
                isKd2Supported = true,
                isUartSupported = false,
                nameChangeSupported = false
            ),
            deviceInformationData = DeviceInformationData(
                model = "KD2",
                hardwareRevision = "1.0",
                firmwareRevision = "3.0-alpha"
            ),
            hpsData = HPSData(
                measurement = HPSMeasurementData(2, 12.3f, 32.5f, 7.85f, 78.0f),
                feature = HPSFeatureData(
                    modeCount = 8,
                    channelSize = listOf(
                        HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.CURRENT),
                        HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.PWM)
                    ),
                    feature = HPSFeatureBits(true, true, true)
                ),
                modes = listOf(
                    listOf(
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(20, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(20, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = false, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(4, 1), HPSChannelConfig(20, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = false, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(16, 1), HPSChannelConfig(20, 0))
                        )
                    ), listOf(
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        )
                    )
                )
            ),
            kD2Data = KD2Data(
                feature = KD2Feature(
                    KD2ConfigFeatures(channelConfigSupported = true, comPinModeSupported = true),
                    KD2ChannelFeature(adaptiveSupported = true)
                ),
                KD2ControlPointData(
                    channelConfigs = listOf(
                        KD2ChannelSetup(
                            fullOutputPower = 18.0f,
                            outputLimit = 85,
                            opticType = KD2OpticType.SPOT,
                            opticOffset = 0f
                        ),
                        KD2ChannelSetup(
                            fullOutputPower = 0.5f,
                            outputLimit = 100,
                            opticType = KD2OpticType.NA,
                            opticOffset = 0f
                        )
                    ),
                    comPinConfig = KD2ComPinConfig.NOT_USED
                )
            )
        ),
        DummyData(
            helenData = HelenData(
                name = "SuperHelen",
                address = "Helen connected to Helena driver",
                isHelenProjectSupported = true,
                isKd2Supported = true,
                isUartSupported = false,
                nameChangeSupported = false
            ),
            deviceInformationData = DeviceInformationData(
                model = "KD2",
                hardwareRevision = "1.0",
                firmwareRevision = "3.0-alpha"
            ),
            hpsData = HPSData(
                measurement = HPSMeasurementData(3, 48.2f, 54.5f, 7.23f, 78.0f),
                feature = HPSFeatureData(
                    modeCount = 8,
                    channelSize = listOf(
                        HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.CURRENT),
                        HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.CURRENT),
                        HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.CURRENT),
                        HPSFeatureChannelSize(8, 3, HPSFeatureChannelDescription.PWM)
                    ),
                    feature = HPSFeatureBits(true, true, true)
                ),
                modes = listOf(
                    listOf(
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(20, 0),
                                HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(20, 0),
                                HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = false, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(4, 1), HPSChannelConfig(20, 0),
                                HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = false, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(16, 1), HPSChannelConfig(20, 0),
                                HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        )
                    ), listOf(
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(0, 0),
                                HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(0, 0),
                                HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(0, 0),
                                HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        ),
                        HPSModeConfig(
                            HPSHelenModeConfig(ignored = true, preferred = false, temporary = false, off = false),
                            listOf(HPSChannelConfig(0, 0), HPSChannelConfig(0, 0),
                                HPSChannelConfig(0, 0), HPSChannelConfig(0, 0))
                        )
                    )
                )
            ),
            kD2Data = KD2Data(
                feature = KD2Feature(
                    KD2ConfigFeatures(channelConfigSupported = true, comPinModeSupported = true),
                    KD2ChannelFeature(adaptiveSupported = true)
                ),
                KD2ControlPointData(
                    channelConfigs = listOf(
                        KD2ChannelSetup(
                            fullOutputPower = 18.0f,
                            outputLimit = 85,
                            opticType = KD2OpticType.SPOT,
                            opticOffset = 4.3f
                        ),
                        KD2ChannelSetup(
                            fullOutputPower = 18.0f,
                            outputLimit = 85,
                            opticType = KD2OpticType.FLOOD,
                            opticOffset = -4.3f
                        ),
                        KD2ChannelSetup(
                            fullOutputPower = 18.0f,
                            outputLimit = 85,
                            opticType = KD2OpticType.SPOT,
                            opticOffset = 4.3f
                        ),
                        KD2ChannelSetup(
                            fullOutputPower = 0.5f,
                            outputLimit = 100,
                            opticType = KD2OpticType.NA,
                            opticOffset = 0f
                        )
                    ),
                    comPinConfig = KD2ComPinConfig.NOT_USED
                )
            )
        )
    )
}