package sg.gov.tech.bluetrace.streetpass.persistence

import android.content.Context
import sg.gov.tech.bluetrace.fragment.DateTools
import java.util.concurrent.TimeUnit

class SafeEntryRecordStorage(val context: Context) {
    val recordDao = StreetPassRecordDatabase.getDatabase(context).safeEntryDao()
    suspend fun purgeOldRecords(before: Long) {
        recordDao.purgeOldSafeEntryData(before)
    }

    fun getAllRecords(): List<SafeEntryRecord> {
        return recordDao.getAllRecords()
    }

    fun getRecordsForDays(days: Int): ArrayList<List<SafeEntryRecord>> {
        val records = ArrayList<List<SafeEntryRecord>>()
        var previousDay = DateTools.getStartOfDay(System.currentTimeMillis()).timeInMillis

        repeat(days) {
            records.add(
                recordDao.getAllRecordsInRange(
                    previousDay,
                    previousDay + TimeUnit.DAYS.toMillis(1)
                )
            )
            previousDay -= TimeUnit.DAYS.toMillis(1)
        }
        return records
    }

    fun getSafeEntryRecordById(venueId: String?, tenantId: String?): SafeEntryRecord? {
        return recordDao.getSafeEntryRecordById(venueId, tenantId)
    }
}
