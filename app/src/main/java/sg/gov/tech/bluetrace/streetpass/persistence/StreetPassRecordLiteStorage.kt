package sg.gov.tech.bluetrace.streetpass.persistence

import android.content.Context

class StreetPassLiteRecordLiteStorage(val context: Context) {

    val recordDao =  StreetPassRecordDatabase.getDatabase(context).bleRecordDao()

    suspend fun saveRecord(record: StreetPassLiteRecord) {
        recordDao.insert(record)
    }

    fun nukeDb() {
        recordDao.nukeDb()
    }

    fun getAllRecords(): List<StreetPassLiteRecord> {
        return recordDao.getCurrentRecords()
    }

    fun getPagedRecords(pageSize: Int, itemIndex: Int): List<StreetPassLiteRecord>{
        return recordDao.getPagedRecords(pageSize, itemIndex)
    }


    suspend fun purgeOldRecords(before: Long) {
        recordDao.purgeOldRecords(before)
    }
}
