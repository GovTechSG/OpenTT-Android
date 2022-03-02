package sg.gov.tech.bluetrace.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.metrics.Metrics
import sg.gov.tech.bluetrace.revamp.splash.SplashActivity
import sg.gov.tech.bluetrace.services.BluetoothMonitoringService.Companion.PUSH_NOTIFICATION_ID


class FCMService : FirebaseMessagingService() {

    private val TAG = "FCMService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        CentralLog.e(TAG, "Saw a fcm message")
        CentralLog.d(TAG, "From: ${remoteMessage.from}")


        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            CentralLog.e(TAG, "Message data payload: " + remoteMessage.data)

            //Put the announcement into Shared Preferences
            remoteMessage.data["body"]?.let { body ->
                Preference.putAnnouncement(baseContext, body)
            }

            remoteMessage.data["command"]?.let {
                var cmd = it.toIntOrNull() ?: 4
                Utils.startBluetoothMonitoringServiceViaIndex(this, cmd)
            }
            remoteMessage.data["purpose"]?.let {
                Metrics(this).upload()
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            CentralLog.d(TAG, "Message Notification Body: ${it.body}")
            it.body?.let { msg ->

                //check the notification for data
                sendNotification(it.title, msg, remoteMessage.data)
            }
        }
    }

    override fun onNewToken(token: String) {
        CentralLog.d(TAG, "Refreshed token: $token")
    }

    private fun sendNotification(
        title: String?,
        messageBody: String,
        notifData: Map<String, String>
    ) {
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        notifData.keys.forEach { key ->
            intent.putExtra(key, notifData[key])
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val defaultChannelId = BuildConfig.PUSH_NOTIFICATION_CHANNEL_NAME
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, defaultChannelId)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(this, R.color.notification_tint))

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            //should create channels as needed.
            //e.g next time got channel X? need to create here too
            val channel = NotificationChannel(
                defaultChannelId,
                BuildConfig.PUSH_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(
            PUSH_NOTIFICATION_ID /* ID of notification */,
            notificationBuilder.build()
        )
    }
}
