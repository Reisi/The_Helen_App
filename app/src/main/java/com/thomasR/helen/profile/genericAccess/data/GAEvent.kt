package com.thomasR.helen.profile.genericAccess.data

sealed class GAEvent

data object GAEventHandled: GAEvent()

enum class GANameChangeResponseCode {
    SUCCESS,
    NOT_SUPPORTED,
    INVALID_LENGHT,
    FAILED
}

data class GANameChangeResponse(val responseCode: GANameChangeResponseCode): GAEvent()