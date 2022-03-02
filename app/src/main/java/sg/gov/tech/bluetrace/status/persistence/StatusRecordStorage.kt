package sg.gov.tech.bluetrace.status.persistence

import android.content.Context
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase

class StatusRecordStorage(val context: Context) {

    val statusDao = StreetPassRecordDatabase.getDatabase(context).statusDao()

    suspend fun saveRecord(record: StatusRecord) {
        statusDao.insert(record)
    }

    fun nukeDb() {
        statusDao.nukeDb()
    }

    fun getAllRecords(): List<StatusRecord> {
        return statusDao.getCurrentRecords()
    }

    fun getPagedRecords(pageSize: Int, itemIndex: Int): List<StatusRecord>{
        return statusDao.getPagedRecords(pageSize, itemIndex)
    }

    suspend fun purgeOldRecords(before: Long) {
        statusDao.purgeOldRecords(before)
    }
}
