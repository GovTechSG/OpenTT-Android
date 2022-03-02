package sg.gov.tech.bluetrace.streetpass.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface StreetPassLiteDao {

    @Query("SELECT * from btl_record_table ORDER BY timestamp ASC")
    fun getRecords(): LiveData<List<StreetPassLiteRecord>>

    @Query("SELECT * from btl_record_table ORDER BY timestamp ASC LIMIT :pageSize OFFSET :itemIndex")
    fun getPagedRecords(pageSize: Int, itemIndex: Int): List<StreetPassLiteRecord>

    @Query("SELECT * from btl_record_table ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecentRecord(): LiveData<StreetPassLiteRecord?>

    @Query("SELECT * from btl_record_table ORDER BY timestamp DESC LIMIT 1")
    fun getLastRecord(): StreetPassLiteRecord?

    @Query("SELECT * from btl_record_table ORDER BY timestamp ASC")
    fun getCurrentRecords(): List<StreetPassLiteRecord>

    @Query("DELETE FROM btl_record_table")
    fun nukeDb()

    @Query("DELETE FROM btl_record_table WHERE timestamp < :before")
    suspend fun purgeOldRecords(before: Long)

    @RawQuery
    fun getRecordsViaQuery(query: SupportSQLiteQuery): List<StreetPassLiteRecord>

    @Query("SELECT Count(*) from btl_record_table WHERE timestamp >= :startTime and timestamp < :endTime ORDER BY timestamp ASC")
    fun countRecordsInRange(startTime: Long, endTime: Long): Int

    @Query("SELECT Count(*) from btl_record_table WHERE timestamp >= :startTime and timestamp < :endTime ORDER BY timestamp ASC")
    fun liveCountRecordsInRange(startTime: Long, endTime: Long): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: StreetPassLiteRecord)
}
