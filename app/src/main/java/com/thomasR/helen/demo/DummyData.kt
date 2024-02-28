package com.thomasR.helen.demo

import com.thomasR.helen.data.HelenData
import com.thomasR.helen.profile.deviceInformation.data.DeviceInformationData
import com.thomasR.helen.profile.helenProject.data.HPSData
import com.thomasR.helen.profile.kd2.data.KD2Data

data class DummyData(
    val helenData: HelenData,
    val deviceInformationData: DeviceInformationData,
    val hpsData: HPSData,
    val kD2Data: KD2Data = KD2Data(),
)
