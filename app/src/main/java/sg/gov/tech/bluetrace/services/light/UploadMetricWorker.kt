package sg.gov.tech.bluetrace.services.light

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.metrics.Metrics

class UploadMetricWorker(val context: Context, workParams: WorkerParameters) :
    Worker(context, workParams) {

    override fun doWork(): Result {
        Metrics(context).upload()
        CentralLog.i("UploadMetricWorker", "Performed scheduled upload")
        return Result.success()
    }
}
