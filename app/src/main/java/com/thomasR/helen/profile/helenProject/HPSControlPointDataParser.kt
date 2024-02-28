package com.thomasR.helen.profile.helenProject

import android.content.Context
import com.thomasR.helen.R
import com.thomasR.helen.profile.helenProject.data.ControlPointEvent
import com.thomasR.helen.profile.helenProject.data.HPSContorlPointOpCode
import com.thomasR.helen.profile.helenProject.data.HPSControlPointIndication
import com.thomasR.helen.profile.helenProject.data.HPSControlPointResponseValue
import com.thomasR.helen.profile.kd2.data.KD2ControlPointCommonResponse
import no.nordicsemi.android.common.core.DataByteArray
import no.nordicsemi.android.common.core.IntFormat

class HPSControlPointDataParser {
    fun isIndicationEnabled(bytes: DataByteArray): Boolean {
        if (bytes.size != 2) return false
        val cccdValue = bytes.getIntValue(IntFormat.FORMAT_UINT16_LE, 0)
        return cccdValue == 2
    }

    fun decode(bytes: DataByteArray): HPSControlPointIndication? {
        if (bytes.size < 3) return null

        val opCode = decodeOpCode(bytes.value[1].toInt()) ?: return null
        val responseValue = decodeResponseValue(bytes.value[2].toInt()) ?: return null
        var mode: Int? = null

        if (opCode == HPSContorlPointOpCode.REQUEST_MODE) {
            if (bytes.size != 4) return null
            mode = bytes.value[3].toInt()
        }

        return HPSControlPointIndication(opCode, responseValue, mode)
    }

    fun getControlPointResponseMessage(
        context: Context,
        response: ControlPointEvent
    ): String? {
        val opcode = response.opCode
        val responseValue = response.responseValue
        val messageIndex = if (responseValue == HPSControlPointResponseValue.OPERATION_FAILED) 2 else responseValue.ordinal

        return when(opcode) {
            HPSContorlPointOpCode.SET_MODE -> if (responseValue == HPSControlPointResponseValue.SUCCESS) null else
                context.resources.getStringArray(R.array.set_mode_status).toMutableList()[messageIndex]
            HPSContorlPointOpCode.REQUEST_SEARCH ->
                context.resources.getStringArray(R.array.initiate_search_status).toMutableList()[messageIndex]
            HPSContorlPointOpCode.FACTORY_RESET ->
                context.resources.getStringArray(R.array.factory_reset_status).toMutableList()[messageIndex]
            else -> null
        }
    }

    private fun decodeOpCode(opCode: Int) : HPSContorlPointOpCode? {
        for (oc in HPSContorlPointOpCode.values()) {
            if (oc.id == opCode) return oc
        }
        return null
    }

    private fun decodeResponseValue(response: Int) : HPSControlPointResponseValue? {
        for (rv in HPSControlPointResponseValue.values()) {
            if (rv.id == response) return rv
        }
        return null
    }
}