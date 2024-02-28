package com.thomasR.helen.profile.helenProject.data

sealed class HPSEvent

data object HPSEventHandled: HPSEvent()

// control point events
data class ControlPointEvent(
    val opCode: HPSContorlPointOpCode,
    val responseValue: HPSControlPointResponseValue
): HPSEvent()

// write mode events
data class WriteModesResponse(val success: Boolean): HPSEvent()