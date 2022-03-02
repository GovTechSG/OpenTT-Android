package sg.gov.tech.bluetrace.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog

class UnpauseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        CentralLog.i("UnpauseAlarmReceiver", "Unpause alarm received, starting service")
        context?.let {
            Utils.startBluetoothMonitoringService(context)
        }
    }
}
