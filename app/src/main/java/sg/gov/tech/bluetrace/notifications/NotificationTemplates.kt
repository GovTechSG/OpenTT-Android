package sg.gov.tech.bluetrace.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.services.BluetoothMonitoringService.Companion.PENDING_ACTIVITY

class NotificationTemplates {

    companion object {

        fun getStartupNotification(context: Context, channel: String): Notification {

            val builder = NotificationCompat.Builder(context, channel)
                .setContentText("Tracer is setting up its antennas")
                .setContentTitle("Setting things up")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setWhen(System.currentTimeMillis())
                .setSound(null)
                .setVibrate(null)
                .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }

        fun getRunningNotification(context: Context, channel: String): Notification {

            var intent = Intent(context, MainActivity::class.java)

            val activityPendingIntent = PendingIntent.getActivity(
                context, PENDING_ACTIVITY,
                intent, 0
            )

            val builder = NotificationCompat.Builder(context, channel)
                .setContentTitle(context.getText(R.string.service_ok_title))
                .setContentText(context.getText(R.string.service_ok_body))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(activityPendingIntent)
                .setTicker(context.getText(R.string.service_ok_body))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getText(R.string.service_ok_body))
                )
                .setWhen(System.currentTimeMillis())
                .setSound(null)
                .setVibrate(null)
                .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }

        fun getUserPausedNotification(context: Context, channel: String): Notification {

            var intent = Intent(context, MainActivity::class.java)

            val activityPendingIntent = PendingIntent.getActivity(
                context, PENDING_ACTIVITY,
                intent, 0
            )

            val timeResumed = Utils.getTime(Preference.getPauseUntil(context))

            val contentText = context.getString(R.string.service_paused_body)
            val builder = NotificationCompat.Builder(context, channel)
                .setContentTitle(context.getText(R.string.tracetogether_paused))
                .setContentText(contentText)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setContentIntent(activityPendingIntent)
                .setTicker(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setWhen(System.currentTimeMillis())
                .setSound(null)
                .setVibrate(null)
                .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }

        fun lackingThingsNotification(context: Context, channel: String): Notification {
            var intent = Intent(context, MainActivity::class.java)

            val activityPendingIntent = PendingIntent.getActivity(
                context, PENDING_ACTIVITY,
                intent, 0
            )

            val builder = NotificationCompat.Builder(context, channel)
                .setContentTitle(context.getText(R.string.service_not_ok_title))
                .setContentText(context.getText(R.string.service_not_ok_body))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notification_warning)
                .setTicker(context.getText(R.string.service_not_ok_body))
                .addAction(
                    R.drawable.ic_notification_setting,
                    context.getText(R.string.service_not_ok_action),
                    activityPendingIntent
                )
                .setContentIntent(activityPendingIntent)
                .setWhen(System.currentTimeMillis())
                .setSound(null)
                .setVibrate(null)
                .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }

    }
}
