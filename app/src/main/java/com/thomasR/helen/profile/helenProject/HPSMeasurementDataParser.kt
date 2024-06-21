package com.thomasR.helen.profile.helenProject

import com.thomasR.helen.profile.helenProject.data.HPSMeasurementData
import no.nordicsemi.android.kotlin.ble.core.data.util.DataByteArray
import no.nordicsemi.android.kotlin.ble.core.data.util.IntFormat
import kotlin.experimental.and

class HPSMeasurementDataParser {
    fun parse(bytes: DataByteArray) : HPSMeasurementData? {
        if (bytes.size < 3) return null

        var offset = 1
        val flags: Byte = bytes.getByte(offset)!!
        offset += 1

        val mode = bytes.getIntValue(IntFormat.FORMAT_UINT8, offset)
        offset += 1

        val powerPresent = (flags and 0x01).toInt() != 0
        val temperaturePresent = (flags and 0x02).toInt() != 0
        val inputVoltagePresent = (flags and 0x04).toInt() != 0
        val stateOfChargePresent = (flags and 0x08).toInt() != 0
        var rawPower: Int? = null
        var rawTemperature: Int? = null
        var rawInputVoltage: Int? = null
        var rawStateOfCharge: Int? = null

        if (powerPresent) {
            if (bytes.size < offset + 2) return null
            rawPower = bytes.getIntValue(IntFormat.FORMAT_UINT16_LE, offset)
            offset += 2
        }
        if (temperaturePresent) {
            if (bytes.size < offset + 1) return null
            rawTemperature = bytes.getIntValue(IntFormat.FORMAT_SINT8, offset)
            offset += 1
        }
        if (inputVoltagePresent) {
            if (bytes.size < offset + 2) return null
            rawInputVoltage = bytes.getIntValue(IntFormat.FORMAT_UINT16_LE, offset)
            offset += 2
        }
        if (stateOfChargePresent) {
            if (bytes.size < offset + 1) return null
            rawStateOfCharge = bytes.getIntValue(IntFormat.FORMAT_UINT8, offset)
            offset += 1
        }

        return HPSMeasurementData(
            mode = mode,
            outputPower = getPower(rawPower),
            temperature = getTemperature(rawTemperature),
            inputVoltage = getVoltage(rawInputVoltage),
            stateOfCharge = getStateOfCharge(rawStateOfCharge)
        )
    }

    private fun getPower(rawPower: Int?): Float? {
        if (rawPower == null) return null
        return rawPower.toFloat() / 1000f
    }

    private fun getTemperature(rawTemperature: Int?): Float? {
        if (rawTemperature == null) return null
        return rawTemperature.toFloat()
    }

    private fun getVoltage(rawInputVoltage: Int?): Float? {
        if (rawInputVoltage == null) return null
        return rawInputVoltage.toFloat() / 1000f
    }

    private fun getStateOfCharge(rawStateOfCharge: Int?): Float? {
        if (rawStateOfCharge == null) return null
        return rawStateOfCharge.toFloat() / 2f
    }
}