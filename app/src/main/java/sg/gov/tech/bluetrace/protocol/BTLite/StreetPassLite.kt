package sg.gov.tech.bluetrace.protocol.BTLite

import android.util.Base64
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.streetpass.ConnectionRecord
import sg.gov.tech.bluetrace.streetpass.PeripheralDevice

class StreetPassLite {

    companion object {

        fun processReadRequestDataReceived(
            dataRead: ByteArray,
            peripheralAddress: String,
            rssi: Int
        ): ConnectionRecord? {
            return if (dataRead.size >= 20) {
                val peripheral = PeripheralDevice("TT Token", peripheralAddress)
                val msg = Base64.encodeToString(dataRead, Base64.NO_WRAP)
                ConnectionRecord(
                    version = dataRead[19].toInt(),
                    msg = msg,
                    org = "GOVTECH",
                    peripheral = peripheral,
                    central = TracerApp.asCentralDevice(),
                    rssi = rssi,
                    txPower = null
                )
            } else null
        }
    }

}
