package com.thomasR.helen.profile.helenProject.data

sealed class HPSCmd

object ReadModes : HPSCmd()
data class WriteModes(val modes: List<List<HPSModeConfig>>) : HPSCmd()

data class EnableControlPoint(val enabled: Boolean) : HPSCmd()

object RequestMode : HPSCmd()
data class SetMode(val modeNo: Int) : HPSCmd()
object RequestSearch : HPSCmd()
object FactoryReset : HPSCmd()
