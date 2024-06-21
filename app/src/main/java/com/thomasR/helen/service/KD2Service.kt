package com.thomasR.helen.service

import android.annotation.SuppressLint
import com.thomasR.helen.profile.kd2.KD2ControlPointDataParser
import com.thomasR.helen.profile.kd2.KD2FeatureDataParser
import com.thomasR.helen.profile.kd2.data.KD2ControlPointOpCode
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestChannelConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestComPinConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestExternalComp
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestInternalComp
import com.thomasR.helen.profile.kd2.data.KD2ControlPointSetChannelConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointSetComPinConfig
import com.thomasR.helen.profile.kd2.data.KD2ControlPointSetExternalComp
import com.thomasR.helen.profile.kd2.data.KD2ControlPointSetInternalComp
import com.thomasR.helen.profile.kd2.data.KD2ControlPointChannelConfigReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointComPinConfigReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointCommonResponse
import com.thomasR.helen.profile.kd2.data.KD2ControlPointExternalCompReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointImuCalibrationStateReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointInternalCompReceived
import com.thomasR.helen.profile.kd2.data.KD2ControlPointRequestImuCalibrationState
import com.thomasR.helen.profile.kd2.data.KD2ControlPointStartImuCalibration
import com.thomasR.helen.repository.KD2Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
//import no.nordicsemi.android.common.core.DataByteArray
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattService
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattServices
import no.nordicsemi.android.kotlin.ble.core.data.util.DataByteArray
import no.nordicsemi.android.kotlin.ble.core.errors.DeviceDisconnectedException
import no.nordicsemi.android.kotlin.ble.core.errors.GattOperationException
import java.util.UUID

val KD2_SERVICE_UUID: UUID = UUID.fromString("4F770501-ED7D-11E4-840E-0002A5D5C51B")
private val KD2_FEATURE_CHARACTERISTIC_UUID:UUID = UUID.fromString("4F770502-ED7D-11E4-840E-0002A5D5C51B")
private val KD2_CONTROL_POINT_CHARACTERISTIC_UUID: UUID = UUID.fromString("4F770503-ED7D-11E4-840E-0002A5D5C51B")

@SuppressLint("MissingPermission")
class KD2Service(
    private val repository: KD2Repository,
    private val services: ClientBleGattServices,
    private val channelCount: Int,
    private val scope: CoroutineScope
) {
    suspend fun configureGatt() {
        val kd2Service = services.findService(KD2_SERVICE_UUID)
        val feature = kd2Service?.findCharacteristic(KD2_FEATURE_CHARACTERISTIC_UUID)
        val controlPoint = kd2Service?.findCharacteristic(KD2_CONTROL_POINT_CHARACTERISTIC_UUID)
        if (feature == null || controlPoint == null)
            throw Exception("KD2 characteristics not available")

        KD2FeatureDataParser().parse(feature.read())?.let { repository.onFeatureRead(it) }

        controlPoint.getNotifications()
            .mapNotNull { KD2ControlPointDataParser().decode(it) }
            .onEach {
                when(it) {
                    is KD2ControlPointCommonResponse ->
                        repository.onControlPointCommonResponse(KD2ControlPointCommonResponse(it.opCode, it.responseValue))
                    is KD2ControlPointChannelConfigReceived ->
                        repository.updateChannelConfig(it.channel, it.channelConfig)
                    is KD2ControlPointComPinConfigReceived ->
                        repository.updateComPinConfig(it.comPinConfig)
                    is KD2ControlPointInternalCompReceived ->
                        repository.updateInternalComp(it.compensation)
                    is KD2ControlPointExternalCompReceived ->
                        repository.updateExternalComp(it.compensation)
                    is KD2ControlPointImuCalibrationStateReceived ->
                        repository.updateImuCalibrationState(it.isCalibrated)
                    else -> {}
                }
            }
            .launchIn(scope)

        suspend fun controlPointWrite(value: DataByteArray) {
            try {
                controlPoint.write(value)
            } catch (e: GattOperationException) {
                /* TODO ? */
            } catch (e: DeviceDisconnectedException) {
                // in case device gets disconnected
            }
        }

        repository.command
            .onEach {
                when(it) {
                    is KD2ControlPointRequestChannelConfig -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.REQUEST_CHANNEL_CONFIG.id.toByte(), it.channel.toByte())
                    ))
                    is KD2ControlPointSetChannelConfig -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.SET_CHANNEL_CONFIG.id.toByte(), it.channel.toByte()) +
                                KD2ControlPointDataParser().encodeChannelConfig(it.channelConfig)
                    ))
                    is KD2ControlPointRequestComPinConfig -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.REQUEST_COM_PIN_CONFIG.id.toByte())
                    ))
                    is KD2ControlPointSetComPinConfig -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.SET_COM_PIN_CONFIG.id.toByte()) +
                        KD2ControlPointDataParser().encodeComPinConfig(it.comPinConfig)
                    ))
                    is KD2ControlPointRequestInternalComp -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.REQUEST_INTERNAL_COMP.id.toByte())
                    ))
                    is KD2ControlPointSetInternalComp -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.SET_INTERNAL_COMP.id.toByte()) +
                        KD2ControlPointDataParser().encodeInternalComp(it.internalComp)
                    ))
                    is KD2ControlPointRequestExternalComp -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.REQUEST_EXTERNAL_COMP.id.toByte())
                    ))
                    is KD2ControlPointSetExternalComp -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.SET_EXTERNAL_COMP.id.toByte()) +
                        KD2ControlPointDataParser().encodeExternalComp(it.externalComp)
                    ))
                    is KD2ControlPointRequestImuCalibrationState -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.REQUEST_IMU_CALIBRATION_STATE.id.toByte())
                    ))
                    is KD2ControlPointStartImuCalibration -> controlPointWrite(DataByteArray(
                        byteArrayOf(KD2ControlPointOpCode.START_IMU_CALIBRATION.id.toByte())
                    ))
                }
            }
            .launchIn(scope)

        scope.launch {
            if (repository.data.value.feature.configFeatures.channelConfigSupported) {
                for (i in 0 until channelCount) {
                    try {
                        controlPointWrite(
                            DataByteArray(
                                byteArrayOf(
                                    KD2ControlPointOpCode.REQUEST_CHANNEL_CONFIG.id.toByte(),
                                    i.toByte()
                                )
                            )
                        )
                    } catch (_: Exception) {}
                }
            }
            if (repository.data.value.feature.configFeatures.comPinModeSupported)
                controlPointWrite(DataByteArray(byteArrayOf(KD2ControlPointOpCode.REQUEST_COM_PIN_CONFIG.id.toByte())))
            if (repository.data.value.feature.configFeatures.internalCompensationSupported)
                controlPointWrite(DataByteArray(byteArrayOf(KD2ControlPointOpCode.REQUEST_INTERNAL_COMP.id.toByte())))
            if (repository.data.value.feature.configFeatures.externalCompensationSupported)
                controlPointWrite(DataByteArray(byteArrayOf(KD2ControlPointOpCode.REQUEST_EXTERNAL_COMP.id.toByte())))
            if (repository.data.value.feature.configFeatures.imuCalibrationSupported)
                controlPointWrite(DataByteArray(byteArrayOf(KD2ControlPointOpCode.REQUEST_IMU_CALIBRATION_STATE.id.toByte())))
        }
    }
}