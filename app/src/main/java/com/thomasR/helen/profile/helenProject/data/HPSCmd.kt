package com.thomasR.helen.profile.helenProject.data

sealed class HPSCmd

data object ReadModes : HPSCmd()
data class WriteModes(val modes: List<List<HPSModeConfig>>) : HPSCmd()

data class EnableControlPoint(val enabled: Boolean) : HPSCmd()

data object RequestMode : HPSCmd()
data class SetMode(val modeNo: Int) : HPSCmd()
data object RequestSearch : HPSCmd()
data object FactoryReset : HPSCmd()
data class OverrideMode(val channelConfig: List<HPSChannelConfig>) : HPSCmd()
