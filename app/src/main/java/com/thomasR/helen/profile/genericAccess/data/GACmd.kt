package com.thomasR.helen.profile.genericAccess.data

sealed class GACmd

data class GASetName(val name: String) : GACmd()
