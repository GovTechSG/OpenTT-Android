package sg.gov.tech.bluetrace.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.text.SimpleDateFormat
import java.util.*

abstract class DayChangedBroadcastReceiver : BroadcastReceiver() {

    private var date = Date()
    private val dateFormat by lazy { SimpleDateFormat("yyMMdd", Locale.getDefault()) }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        val currentDate = Date()

        if ((action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) || !isSameDay(
                currentDate
            )
        ) {
            date = currentDate
            onDayChanged()
        }
    }

    private fun isSameDay(currentDate: Date) =
        dateFormat.format(currentDate) == dateFormat.format(date)

    abstract fun onDayChanged()

    companion object {

        /**
         * Create the [IntentFilter] for the [DayChangedBroadcastReceiver].
         *
         * @return The [IntentFilter]
         */
        fun getIntentFilter() = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
    }
}
