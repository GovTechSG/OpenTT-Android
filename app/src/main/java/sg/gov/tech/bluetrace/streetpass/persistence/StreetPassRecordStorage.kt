package sg.gov.tech.bluetrace.streetpass.persistence

import android.content.Context
import sg.gov.tech.bluetrace.fragment.DateTools
import java.util.concurrent.TimeUnit

class StreetPassRecordStorage(val context: Context) {

    val recordDao = StreetPassRecordDatabase.getDatabase(context).recordDao()

    suspend fun saveRecord(record: StreetPassRecord) {
        recordDao.insert(record)
    }

    fun nukeDb() {
        recordDao.nukeDb()
    }

    fun getAllRecords(): List<StreetPassRecord> {
        return recordDao.getCurrentRecords()
    }

    fun getPagedRecords(pageSize: Int, itemIndex: Int): List<StreetPassRecord>{
        return recordDao.getPagedRecords(pageSize, itemIndex)
    }

    suspend fun purgeOldRecords(before: Long) {
        recordDao.purgeOldRecords(before)
    }

    fun getAllRecordsCountForDays(days: Int): List<Int> {
        val entryCountList = ArrayList<Int>()
        var previousDay = DateTools.getStartOfDay(System.currentTimeMillis()).timeInMillis

        repeat(days) {
            entryCountList.add(recordDao.countBTnBTLRecordsInRange(previousDay, previousDay + TimeUnit.DAYS.toMillis(1)))
            previousDay -= TimeUnit.DAYS.toMillis(1)
        }

        return entryCountList
    }
}
