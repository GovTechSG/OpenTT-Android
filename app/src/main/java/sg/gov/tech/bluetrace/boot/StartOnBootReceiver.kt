package sg.gov.tech.bluetrace.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger

class StartOnBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            CentralLog.d("StartOnBootReceiver", "boot completed received")

            try {
                //can i try a scheduled service start here?
                CentralLog.d("StartOnBootReceiver", "Attempting to start service")
                Utils.scheduleStartMonitoringService(context, 500)
            } catch (e: Throwable) {
                CentralLog.e("StartOnBootReceiver", e.localizedMessage)
                e.printStackTrace()
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                DBLogger.e(
                    DBLogger.LogType.BLUETRACE,
                    loggerTAG,
                    e.localizedMessage,
                    DBLogger.getStackTraceInJSONArrayString(e as Exception)
                )

            }

        }
    }
}
