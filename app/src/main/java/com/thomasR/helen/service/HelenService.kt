package com.thomasR.helen.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile.GATT
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.thomasR.helen.R
import com.thomasR.helen.permission.BluetoothPermission
import com.thomasR.helen.repository.HelenRepository
import com.thomasR.helen.repository.MainRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.kotlin.ble.client.main.callback.ClientBleGatt
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattService
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattServices
import no.nordicsemi.android.kotlin.ble.core.data.GattConnectionState
import java.util.Timer
import kotlin.concurrent.schedule

private const val IS_ACTIVE = "com.thomasR.helen.service.IS_ACTIVE"

@SuppressLint("MissingPermission")
class HelenService : NotificationService() {

    private val binder = LocalBinder()
    private var isStarted = false
    private var isBound = false
    private var isForeground = false
    private var repository: MainRepository? = null

    /**
     * broadcast receiver to listen to incoming bluetooth connections while service is running
     */
    private val onConnection = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            @Suppress("DEPRECATION") val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                         else
                            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device?.type == BluetoothDevice.DEVICE_TYPE_LE) {
                val services = device.uuids
                if (services == null || services.contains(ParcelUuid(HPS_SERVICE_UUID))) {
                    Log.i("HelenService", "Possible compatible device connected, also connecting...")
                    // TODO this receiver also generates events for devices which are connected by the
                    //      helen app itself, add additional check or just rely that the connect
                    //      function won't create a second connection?
                    binder.connectDevice(device.address, device.name)
                }
            }
        }
    }

    /**
     * receiver to listen to local broadcast to determine if service is created or not
     */
    private val onIsActive = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // just change the result data to true
            resultData = "true"
        }
    }

    override fun onCreate() {
        super.onCreate()

        // add a receiver to listen to the isRunning query of the main activity
        val isActiveFilter = IntentFilter().apply {
            addAction(IS_ACTIVE)
        }
        ContextCompat.registerReceiver(this, onIsActive, isActiveFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        isStarted = true
        //updateNotification()

        return START_REDELIVER_INTENT
    }

    /**
     * method to check the already connected devices, if they are compatible and connects to them is yes
     *
     */
    private fun checkConnectedDevices() {
        // if bluetooth permissions are not granted yet just ignore
        /// TODO is there an event to get notified if permissions were granted? If yes it might be useful to call this function there.
        val btPermission = BluetoothPermission()
        if (!btPermission.isRequired || btPermission.arePermissionsGranted(this)) {
            val btManger = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val connectedDevices = btManger.getConnectedDevices(GATT)

            for (device in connectedDevices) {
                if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                    val services = device.uuids
                    // connect if device has helen project service, or if services are not available yet
                    if (services == null || services.contains(ParcelUuid(HPS_SERVICE_UUID))) {
                        Log.i("HelenService", "Possible compatible device found, connecting...")
                        binder.connectDevice(device.address, device.name)
                    }
                }
            }
        }
    }

    private fun onBindRebind() {
        isBound = true

        // add receiver to listen to bluetooth connections established for the case a device
        // connects automatically or by a background task of another app
        val btFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        }
        ContextCompat.registerReceiver(this, onConnection, btFilter, ContextCompat.RECEIVER_EXPORTED)

        // service can be put into background
        updateServiceState()

        // if bounded foreground service isn't necessary anymore
        //updateNotification()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

        Log.d("HelenService", "Service bound")

        onBindRebind()

        return binder
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)

        Log.d("HelenService", "Service rebound")

        onBindRebind()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        super.onUnbind(intent)
        isBound = false

        Log.d("HelenService", "Service unbound")

        unregisterReceiver(onConnection)

        // service might be put into foreground
        updateServiceState()

        // if service keeps running a notification is necessary when main application goes into background
        //updateNotification()

        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        // disconnect actively all connected devices, otherwise bonded devices stay connected
        if (repository != null) {
            for (device in repository!!.devices) {
                if (device.data.value.connectionState == GattConnectionState.STATE_CONNECTING ||
                    device.data.value.connectionState == GattConnectionState.STATE_CONNECTED) {
                    binder.disconnectDevice(device.data.value.address!!)
                }
            }
        }

        Log.d("HelenService", "Service destroyed")

        unregisterReceiver(onIsActive)
    }

    inner class LocalBinder: Binder() {

        fun getRepository() : MainRepository {
            if (this@HelenService.repository == null)
                this@HelenService.repository = MainRepository(this@HelenService)
            checkConnectedDevices()
            return this@HelenService.repository!!
        }

        fun setRepository(repository: MainRepository) {
            this@HelenService.repository = repository
            checkConnectedDevices()
        }

        fun connectDevice(address: String, name: String? = null) {
            if (repository == null) throw Exception("no repository set")

            val deviceRep = repository!!.getDeviceRepository(address)
            if (deviceRep == null) {
                repository!!.addDevice(address, name)
            }

            // start client if either device is new (deviceRep == null) or if the not incompatible device is disconnected
            if (deviceRep == null ||
                (deviceRep.data.value.connectionState == GattConnectionState.STATE_DISCONNECTED &&
                        deviceRep.data.value.isHelenProjectSupported != false))
                startGattClient(address)
        }

        fun disconnectDevice(address: String) {
            if (repository == null) throw Exception("no repository set")

            val deviceRep = repository!!.getDeviceRepository(address)
            if (deviceRep == null) {
                Log.e("HelenService", "device $address not known, cannot disconnect")
                return
            }

            val client = deviceRep.data.value.client
            if (client == null) {
                Log.e("HelenService", "no client for device $address, cannot disconnect")
                return
            }

            //Log.d("HelenService", "Disconnecting device $address")
            client.disconnect()
        }
    }

    private fun startGattClient(address: String) = lifecycleScope.launch {
        val deviceRepository = repository!!.getDeviceRepository(address)
        if (deviceRepository == null) {
            cancel()
            return@launch
        }

        // clear connectionState to prevent multiple connections
        deviceRepository.clearConnectionState()

        // TODO try to use client.reconnect()

        val client = ClientBleGatt.connect(this@HelenService, address, this)

        deviceRepository.setClient(client, this)

        // TODO: when connecting through scanning a failed connection will be re-initiated with the
        //       next scan report, but otherwise not
        if (client.connectionStateWithStatus.value?.state != GattConnectionState.STATE_CONNECTED) {
            cancel()
            return@launch
        }

        client.connectionStateWithStatus
            .filterNotNull()
            .onEach {
                Timer().schedule(100) {// let others subscribers do their work first
                    updateServiceState()
                    updateNotification()
                }

                if (it.state == GattConnectionState.STATE_DISCONNECTED) {
                    Timer().schedule(1000) { this@launch.cancel() }
                }
            }
            .launchIn(this)

        /*client.connectionStateWithStatus.onEach {
            Timer().schedule(100) {// let others subscribers do their work first
                updateServiceState()
                updateNotification()
            }
        }.launchIn(this)*/

        try {
            val services = client.discoverServices()
            configureGatt(services, deviceRepository, this)
            //Log.i("HelenService", "Gatt Client configured")
        } catch (e: Exception) {
            if (e.message == "HPS service not available") {
                //Log.i("HelenService", "Device $address not supported")
                Timer().schedule(100) { client.disconnect() }
            }
            else
                Log.e("HelenService", e.toString())
        }
    }.invokeOnCompletion { Log.d("HelenService", "gatt client for device $address stopped") }

    private suspend fun configureGatt(
        services: ClientBleGattServices,
        deviceRepository: HelenRepository,
        scope: CoroutineScope
    ) {

        // Generic access must be available by specs on all devices, thus cannot fail
        GenericAccess(deviceRepository, services, scope).configureGatt()

        try {
            DeviceInformation(deviceRepository.dis, services, scope).configureGatt()
        } catch (e: Exception) {
            // clearing not necessary on new devices, but in the case a device reconnects with
            // changed configuration (e.g. after firmware update) this should be cleared as a
            // precaution
            deviceRepository.dis.clear()
        }

        try {
            HelenProject(deviceRepository.hps, services, scope).configureGatt()
            deviceRepository.setHelenProjectSupported(true)
        } catch (e: Exception) {
            deviceRepository.setHelenProjectSupported(false)
            deviceRepository.hps.clear()
            throw Exception("HPS service not available")
        }

        try {
            UART(deviceRepository.uart, services, scope).configureGatt()
            deviceRepository.setUartSupported(true)
        } catch (e: Exception) {
            deviceRepository.setUartSupported(false)
            deviceRepository.uart.clear()
        }

        deviceRepository.setDfuSupported(DfuService.isDfuAvailable(services))

        when (deviceRepository.dis.data.value.model) {
            // when adding new models, handling of not supported cannot be done in else part of when
            "KD2" -> {
                try {
                    KD2Service(
                        repository = deviceRepository.kd2,
                        services = services,
                        channelCount = deviceRepository.hps.data.value.feature?.channelSize?.size ?: 0,
                        scope = scope
                    ).configureGatt()
                    deviceRepository.setKd2Supported(true)
                } catch (e: Exception) {
                    deviceRepository.setKd2Supported(false)
                    deviceRepository.kd2.clear()
                }
            }
            else -> {
                deviceRepository.setKd2Supported(false)
                deviceRepository.kd2.clear()
            }
        }
    }

    /**
     * method to update the service notification to show which devices are connected
     */
    private fun updateNotification() {
        if (isForeground)
            super.updateNotification(createNotificationMessage())
    }

    private fun createNotificationMessage() :String? {
        val devices = mutableListOf<String>()
        val reps = repository?.devices ?: emptyList()
        for (device in reps) {
            if (device.data.value.connectionState == GattConnectionState.STATE_CONNECTED ||
                device.data.value.connectionState == GattConnectionState.STATE_CONNECTING)
                devices.add(device.data.value.name ?: "unknown device")
        }
        if (devices.isEmpty()) return null

        var message = ""
        for (i in devices.indices) {
            message += devices[i]
            if (devices.size - i == 2) message += " " + getString(R.string.notification_message_and) + " "
            else if (devices.size - i > 2) message += ", "
        }
        message += " " + getString(R.string.notification_message_connected)

        return message
    }

    private fun isAnyDeviceConnected() : Boolean {
        val reps = repository?.devices ?: emptyList()
        for (device in reps) {
            if (device.data.value.connectionState == GattConnectionState.STATE_CONNECTED ||
                device.data.value.connectionState == GattConnectionState.STATE_CONNECTING) {
                return true
            }
        }
        return false
    }

    private fun updateServiceState() {
        val isConnected = isAnyDeviceConnected()

        if (!isForeground && isConnected && !isBound) {
            // put service to foreground if any device is connected but app has unbound
            super.startForegroundService(createNotificationMessage()!!)
            isForeground = true
        }

        if (isForeground && (!isConnected || isBound)) {
            // put service into background if no device is connected or app has bound
            super.stopForegroundService()
            isForeground = false
        }

        /*Log.d("HelenService", "connected $isConnected, bound $isBound, started $isStarted")

        if (isConnected && !isStarted) {
            val intent = Intent(this, HelenService::class.java)
            startService(intent)
            // isStarted = true // is done in inStarted()
            Log.d("HelenService", "Service started")
        }

        if (isStarted && !isConnected) {
            stopSelf()
            super.stopService() // stop being in foreground to remove notification
            Log.d("HelenService", "Service stopped")
            isStarted = false
        }*/
    }

    companion object {
        private var resultHandler: ((Boolean) -> Unit)? = null

        private val resultReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val isRunning = resultData == "true"
                resultHandler?.let { it(isRunning) }
                resultHandler = null
            }
        }

        /**
         * Method to check if the service is running. Checks if the service is running by sending an
         * ordered Broadcast. When the service is created it registers a receiver to listen to this
         * broadcast and changes the result data, which then is evaluated in the result receiver
         *
         * @param context the Application context
         * @param resultHandler handler which is called after the broadcast finally arrived in the
         * result handler with the running state of the service
         */
        fun isRunning(
            @ApplicationContext context: Context,
            resultHandler: (Boolean) -> Unit
        ) {
            this.resultHandler = resultHandler
            val intent = Intent(IS_ACTIVE).apply { setPackage("com.thomasR.helen") }
            context.sendOrderedBroadcast(intent, null, resultReceiver, null, 0, "false", null)
        }
    }
}