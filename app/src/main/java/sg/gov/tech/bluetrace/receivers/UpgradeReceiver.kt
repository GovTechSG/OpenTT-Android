package sg.gov.tech.bluetrace.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger

class UpgradeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        try {
            if (Intent.ACTION_MY_PACKAGE_REPLACED != intent!!.action) return
            // Start your service here.
            context?.let {
                CentralLog.i("UpgradeReceiver", "Starting service from upgrade receiver")
                Utils.startBluetoothMonitoringService(context)
            }
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(loggerTAG, "Unable to handle upgrade: ${e.localizedMessage}")
            DBLogger.e(
                DBLogger.LogType.BLUETRACE,
                loggerTAG,
                "Unable to handle upgrade: ${e.localizedMessage}",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
        }
    }
}