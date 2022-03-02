package sg.gov.tech.bluetrace.services.light

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.status.persistence.StatusRecordStorage
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecordStorage
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordStorage
import java.util.*
import kotlin.coroutines.CoroutineContext

class OldRecordsPurgeWorker(val context: Context, workParams: WorkerParameters) :
    Worker(context, workParams), CoroutineScope {

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    override fun doWork(): Result {

        val logDao = StreetPassRecordDatabase.getDatabase(context).logRecordDao()

        launch {

            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -BuildConfig.PURGE_DAYS)
            val streetpassPurgeThreshold = cal.timeInMillis

            val cal2 = Calendar.getInstance()
            cal2.add(Calendar.DATE, -BuildConfig.PURGE_SE_DAYS)
            val safeentryPurgeThreshold = cal2.timeInMillis

            CentralLog.i(
                "OldRecordsPurgeWorker",
                "Coroutine - Purging of data before epoch time $streetpassPurgeThreshold"
            )

            val safeEntryRecordStorage = SafeEntryRecordStorage(context)
            val streetPassRecordStorage = StreetPassRecordStorage(context)
            val statusRecordStorage = StatusRecordStorage(context)


            safeEntryRecordStorage.purgeOldRecords(safeentryPurgeThreshold)
            streetPassRecordStorage.purgeOldRecords(streetpassPurgeThreshold)
            statusRecordStorage.purgeOldRecords(streetpassPurgeThreshold)

            val logCalendar = Calendar.getInstance()
            logCalendar.add(Calendar.DATE, -BuildConfig.LOG_PURGE_DAYS)
            val logDaysAgo = logCalendar.timeInMillis
            logDao.purgeOldRecords(logDaysAgo)

            Preference.putLastPurgeTime(context, System.currentTimeMillis())
        }

        CentralLog.i("OldRecordsPurgeWorker", "Performed scheduled purge")
        return Result.success()
    }
}
