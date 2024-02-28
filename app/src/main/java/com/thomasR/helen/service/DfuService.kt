package com.thomasR.helen.service

import android.app.Activity
import no.nordicsemi.android.dfu.DfuBaseService
import no.nordicsemi.android.kotlin.ble.client.main.service.ClientBleGattServices
import java.util.UUID

val DFU_SERVICE_UUID: UUID = UUID.fromString("0000FE59-0000-1000-8000-00805F9B34FB")

class DfuService : DfuBaseService() {
    override fun getNotificationTarget(): Class<out Activity> {
        return Class.forName("com.thomasR.helen.MainActivity") as Class<out Activity>
    }

    override fun isDebug(): Boolean {
        return true //super.isDebug()
    }

    companion object {
        fun isDfuAvailable(services: ClientBleGattServices) : Boolean {
            val service = services.findService(DFU_SERVICE_UUID)
            return service != null
        }
    }
}