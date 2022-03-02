package sg.gov.tech.bluetrace.status.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteRecord

@Dao
interface StatusRecordDao {

    @Query("SELECT * from status_table ORDER BY timestamp ASC")
    fun getRecords(): LiveData<List<StatusRecord>>

    @Query("SELECT * from status_table ORDER BY timestamp ASC LIMIT :pageSize OFFSET :itemIndex")
    fun getPagedRecords(pageSize: Int, itemIndex: Int): List<StatusRecord>


    @Query("SELECT * from status_table ORDER BY timestamp ASC")
    fun getCurrentRecords(): List<StatusRecord>

    @Query("SELECT * from status_table where msg = :msg ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecentRecord(msg: String): LiveData<StatusRecord?>

    @Query("DELETE FROM status_table")
    fun nukeDb()

    @Query("DELETE FROM status_table WHERE timestamp < :before")
    suspend fun purgeOldRecords(before: Long)

    @RawQuery
    fun getRecordsViaQuery(query: SupportSQLiteQuery): List<StatusRecord>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: StatusRecord)

}
