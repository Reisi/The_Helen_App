package com.thomasR.helen.service

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.util.Log
import com.thomasR.helen.profile.helenProject.data.FactoryReset
import com.thomasR.helen.profile.helenProject.data.HPSContorlPointOpCode
import com.thomasR.helen.profile.helenProject.data.ReadModes
import com.thomasR.helen.profile.helenProject.data.RequestMode
import com.thomasR.helen.profile.helenProject.data.RequestSearch
import com.thomasR.helen.profile.helenProject.data.SetMode
import com.thomasR.helen.profile.helenProject.data.WriteModes
import com.thomasR.helen.profile.helenProject.HPSControlPointDataParser
import com.thomasR.helen.profile.helenProject.HPSFeatureDataParser
import com.thomasR.helen.profile.helenProject.HPSMeasurementDataParser
import com.thomasR.helen.profile.helenProject.HPSModesDataParser
import com.thomasR.helen.profile.helenProject.data.EnableControlPoint
import com.thomasR.helen.profile.helenProject.data.HPSControlPointIndication
import com.thomasR.helen.profile.helenProject.data.OverrideMode
import com.thomasR.helen.repository.HPSRepository
import com.thomasR.helen.repository.HelenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
//import no.nordicsemi.android.common.core.DataByteArray
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattService
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattServices
import no.nordicsemi.android.kotlin.ble.core.data.util.DataByteArray
import no.nordicsemi.android.kotlin.ble.core.errors.DeviceDisconnectedException
import no.nordicsemi.android.kotlin.ble.core.errors.GattOperationException
import java.util.UUID

val HPS_SERVICE_UUID: UUID = UUID.fromString("4F770301-ED7D-11E4-840E-0002A5D5C51B")
private val HPS_MEASUREMENT_CHARACTERISTIC_UUID: UUID = UUID.fromString("4F770302-ED7D-11E4-840E-0002A5D5C51B")
private val HPS_FEATURE_CHARACTERISTIC_UUID: UUID = UUID.fromString("4F770303-ED7D-11E4-840E-0002A5D5C51B")
private val HPS_MODES_CHARACTERISTIC_UUID: UUID = UUID.fromString("4F770304-ED7D-11E4-840E-0002A5D5C51B")
private val HPS_CONTROL_POINT_CHARACTERISTIC_UUID: UUID = UUID.fromString("4F770305-ED7D-11E4-840E-0002A5D5C51B")
private val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@SuppressLint("MissingPermission")
class HelenProject(
    private val repository: HPSRepository,
    private val services: ClientBleGattServices,
    private val scope: CoroutineScope
) {


    suspend fun configureGatt()/*: ClientBleGattService */{
        val hpService = services.findService(HPS_SERVICE_UUID)
        val hpMeasurement = hpService?.findCharacteristic(HPS_MEASUREMENT_CHARACTERISTIC_UUID)
        val hpFeature = hpService?.findCharacteristic(HPS_FEATURE_CHARACTERISTIC_UUID)
        val hpModes = hpService?.findCharacteristic(HPS_MODES_CHARACTERISTIC_UUID)
        val hpControlPoint = hpService?.findCharacteristic(HPS_CONTROL_POINT_CHARACTERISTIC_UUID)
        val hpControlPointCccd = hpControlPoint?.findDescriptor(CCCD_DESCRIPTOR_UUID)

        if (hpFeature == null || hpModes == null || hpControlPoint == null || hpControlPointCccd == null)
            throw Exception("helen project service not available")

        var controlPointEnabled = false
        //lateinit var controlPointFlow: Flow<HPSControlPointIndication>
        var controlPointJob: Job? = null

        suspend fun controlPointWrite(value: DataByteArray) {
            try {
                hpControlPoint.write(value)
            } catch (e: GattOperationException) {
                /* TODO ? */
            } catch (e: DeviceDisconnectedException) {
                // in case device gets disconnected
            }
        }

        suspend fun controlPointEnable(enable: Boolean) {
            if (enable != (controlPointJob == null))
                throw Exception("control point already in desired state")
            if (enable) {
                controlPointJob = hpControlPoint.getNotifications()
                    .mapNotNull { HPSControlPointDataParser().decode(it) }
                    .onEach { repository.onHPSControlPointIndicationReceived(it) }
                    .launchIn(scope)
            } else {
                controlPointJob?.cancel()
                controlPointJob = null
            }
        }

        repository.command
            .onEach {
                when(it) {
                    is ReadModes -> repository.onHPSModeConfigRead(
                        HPSModesDataParser().decode(
                            hpModes.read(), repository.data.value.feature!!
                        )
                    )
                    is WriteModes -> {
                        try {
                            hpModes.write(HPSModesDataParser().encode(
                                    it.modes,
                                    repository.data.value.feature!!
                            ))
                            repository.onWriteConfigResponseReceived(true)
                        } catch (_: Exception) {
                            repository.onWriteConfigResponseReceived(false)
                        }
                    }
                    is EnableControlPoint -> {
                        controlPointEnabled = it.enabled
                        repository.onControlPointEnableChanged(it.enabled)
                        controlPointEnable(it.enabled)
                        /*if (it.enabled) {
                            Log.d("HelenProject", "control point hash ${hpControlPoint.hashCode()}")
                            controlPointJob = controlPointFlow.launchIn(scope)
                            //hpControlPointCccd.write(DataByteArray(byteArrayOf(0x02, 0x00)))
                        } else {
                            controlPointJob?.cancel()
                            controlPointJob = null
                        //hpControlPointCccd.write(DataByteArray(byteArrayOf(0x00, 0x00)))
                        }*/

                    }
                    is RequestMode -> if (controlPointEnabled) {
                        controlPointWrite(DataByteArray(byteArrayOf(
                                HPSContorlPointOpCode.REQUEST_MODE.id.toByte()
                        )))
                    }
                    is SetMode -> if (controlPointEnabled) {
                        controlPointWrite(DataByteArray(byteArrayOf(
                                HPSContorlPointOpCode.SET_MODE.id.toByte(), it.modeNo.toByte()
                        )))
                    }
                    is RequestSearch -> if (controlPointEnabled) {
                        controlPointWrite(DataByteArray(byteArrayOf(
                                HPSContorlPointOpCode.REQUEST_SEARCH.id.toByte()
                        )))
                    }
                    is FactoryReset -> if (controlPointEnabled) {
                        controlPointWrite(DataByteArray(byteArrayOf(
                                HPSContorlPointOpCode.FACTORY_RESET.id.toByte()
                        )))
                    }
                    is OverrideMode ->
                    {
                        val feature = repository.data.value.feature
                        if (controlPointEnabled && feature != null) {
                            var msg = byteArrayOf(HPSContorlPointOpCode.OVERRIDE_MODE.id.toByte())
                            msg += HPSModesDataParser().encodeOverrideChannels(it.channelConfig, feature)
                            controlPointWrite(DataByteArray(msg))
                        }
                    }
                }
            }
            .launchIn(scope)

        hpMeasurement?.getNotifications()
            ?.mapNotNull { HPSMeasurementDataParser().parse(it) }
            ?.onEach { repository.onHPSMeasurementDataReceived(it) }
            ?.launchIn(scope)

        repository.onHPSFeatureRead(HPSFeatureDataParser().parse(hpFeature.read()))

        repository.onHPSModeConfigRead(HPSModesDataParser().decode(hpModes.read(), repository.data.value.feature!!))

        //controlPointFlow = hpControlPoint.getNotifications()
        //    .mapNotNull { HPSControlPointDataParser().decode(it) }
        //    .onEach { repository.onHPSControlPointIndicationReceived(it) }
            //.launchIn(scope)

        controlPointEnabled = HPSControlPointDataParser().isIndicationEnabled(hpControlPointCccd.read())
        repository.onControlPointEnableChanged(controlPointEnabled)
        if (controlPointEnabled)
            controlPointEnable(controlPointEnabled)
        //if (controlPointEnabled) { controlPointJob = controlPointFlow.launchIn(scope) }

        return// hpService
    }
}