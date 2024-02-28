package com.thomasR.helen.profile.genericAccess

import android.content.Context
import com.thomasR.helen.R
import com.thomasR.helen.profile.genericAccess.data.GANameChangeResponseCode

class GADataParser {

    fun getNameChangeResponseMessage(
        context: Context,
        response: GANameChangeResponseCode
    ): String? {
        val responseArray = context.resources.getStringArray(R.array.name_change_status)
        val index = response.ordinal

        return responseArray[index]
    }
}