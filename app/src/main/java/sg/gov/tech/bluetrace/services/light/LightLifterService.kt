package sg.gov.tech.bluetrace.services.light

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.LogWorker
import java.util.concurrent.TimeUnit

class LightLifterService : IntentService("LightLifterService") {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onHandleIntent(intent: Intent?) {
        var commandToRun =
            LightTasks.INVALID
        intent?.let {
            val commandToRunIndex = intent.getIntExtra(TASK_KEY, LightTasks.INVALID.index)
            commandToRun =
                LightTasks.findByIndex(
                    commandToRunIndex
                )
        }

        when (commandToRun) {
            LightTasks.METRIC -> {
                val metricUploadRequest = PeriodicWorkRequestBuilder<UploadMetricWorker>(
                    8,
                    TimeUnit.HOURS,
                    30,
                    TimeUnit.MINUTES
                ).build()
                WorkManager.getInstance(this)
//                    .enqueue(metricUploadRequest)
                    .enqueueUniquePeriodicWork(
                        "metric",
                        ExistingPeriodicWorkPolicy.KEEP,
                        metricUploadRequest
                    )
            }

            LightTasks.PURGE -> {
                val purgeRequest = PeriodicWorkRequestBuilder<OldRecordsPurgeWorker>(
                    12,
                    TimeUnit.HOURS,
                    30,
                    TimeUnit.MINUTES
                ).build()

                WorkManager.getInstance(this)
                    .enqueueUniquePeriodicWork(
                        "purge",
                        ExistingPeriodicWorkPolicy.KEEP,
                        purgeRequest
                    )
            }

            LightTasks.LOGGING -> {
                val loggingRequest =
                    PeriodicWorkRequestBuilder<LogWorker>(15, TimeUnit.MINUTES).build()

                WorkManager.getInstance(this)
                    .enqueueUniquePeriodicWork(
                        "logging",
                        ExistingPeriodicWorkPolicy.KEEP,
                        loggingRequest
                    )
            }
        }

    }

    companion object {
        const val TASK_KEY = "TASK_KEY"

        fun scheduleMetricUpload(context: Context) {
            CentralLog.i("Metric", "Scheduling repeating metrics")
            val intent = Intent(context, LightLifterService::class.java)
            intent.putExtra(
                TASK_KEY,
                LightTasks.METRIC.index
            )
            context.startService(intent)
        }

        fun schedulePurge(context: Context) {
            CentralLog.i("Purge", "Scheduling repeating purge")
            val intent = Intent(context, LightLifterService::class.java)
            intent.putExtra(
                TASK_KEY,
                LightTasks.PURGE.index
            )
            context.startService(intent)
        }

        fun scheduleLogging(context: Context) {
            CentralLog.i("Logging", "Scheduling repeating logging")
            val intent = Intent(context, LightLifterService::class.java)
            intent.putExtra(
                TASK_KEY,
                LightTasks.LOGGING.index
            )
            context.startService(intent)
        }
    }
}

enum class LightTasks(val index: Int, val string: String) {
    INVALID(-1, "INVALID"),
    METRIC(0, "METRIC"),
    PURGE(1, "PURGE"),
    LOGGING(2, "LOGGING");

    companion object {
        private val types = values().associate { it.index to it }
        fun findByIndex(index: Int) = types[index] ?: INVALID
    }
}
