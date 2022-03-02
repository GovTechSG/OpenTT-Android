package sg.gov.tech.bluetrace.streetpass

import android.content.Context
import sg.gov.tech.bluetrace.bluetooth.gatt.GattServer
import sg.gov.tech.bluetrace.bluetooth.gatt.GattService

class StreetPassServer constructor(val context: Context, val serviceUUIDString: String) {

    private val TAG = "StreetPassServer"
    private var gattServer: GattServer? = null

    init {
        gattServer = setupGattServer(context, serviceUUIDString)
    }

    private fun setupGattServer(context: Context, serviceUUIDString: String): GattServer? {
        val gattServer = GattServer(context, serviceUUIDString)
        var started = gattServer.startServer()

        if (started) {
            val readService = GattService(context, serviceUUIDString)
            gattServer.addService(readService)
            return gattServer
        }
        return null
    }

    fun tearDown() {
        gattServer?.stop()
    }

    fun checkServiceAvailable(): Boolean {
        return gattServer?.bluetoothGattServer?.services?.filter {
            it.uuid.toString().equals(serviceUUIDString)
        }?.isNotEmpty() ?: false
    }

}