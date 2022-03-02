package sg.gov.tech.bluetrace.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.logging.CentralLog

object Scheduler {
    const val TAG = "Scheduler"

    fun scheduleServiceIntent(
        requestCode: Int,
        context: Context,
        intent: Intent,
        timeFromNowInMillis: Long
    ) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + timeFromNowInMillis, alarmIntent
            )

        } else {
            alarmMgr.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + timeFromNowInMillis, alarmIntent
            )
        }

    }

    fun scheduleRepeatingServiceIntent(
        requestCode: Int,
        context: Context,
        intent: Intent,
        intervalMillis: Long
    ) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        CentralLog.d(
            TAG,
            "Purging alarm set to ${Preference.getLastPurgeTime(context) + intervalMillis}"
        )
        alarmMgr.setRepeating(
            AlarmManager.RTC,
            Preference.getLastPurgeTime(context) + intervalMillis,
            intervalMillis,
            alarmIntent
        )
    }

    fun cancelServiceIntent(requestCode: Int, context: Context, intent: Intent) {
        val alarmIntent =
            PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        alarmIntent.cancel()
    }

    fun scheduleExact(requestCode: Int, context: Context, intent: Intent, scheduleAtMillis: Long) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmMgr.setExact(AlarmManager.RTC_WAKEUP, scheduleAtMillis, alarmIntent)
    }
}
