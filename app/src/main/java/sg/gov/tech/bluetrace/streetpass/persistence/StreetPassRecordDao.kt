package sg.gov.tech.bluetrace.streetpass.persistence

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface StreetPassRecordDao {

    @Query("SELECT * from record_table ORDER BY timestamp ASC")
    fun getRecords(): LiveData<List<StreetPassRecord>>

    @Query("SELECT * from record_table ORDER BY timestamp ASC LIMIT :pageSize OFFSET :itemIndex")
    fun getPagedRecords(pageSize: Int, itemIndex: Int): List<StreetPassRecord>


    @Query("SELECT * from record_table ORDER BY timestamp DESC LIMIT 1")
    fun getMostRecentRecord(): LiveData<StreetPassRecord?>

    @Query("SELECT * from record_table ORDER BY timestamp DESC LIMIT 1")
    fun getLastRecord(): StreetPassRecord?

    @Query("SELECT * from record_table ORDER BY timestamp ASC")
    fun getCurrentRecords(): List<StreetPassRecord>

    @Query("DELETE FROM record_table")
    fun nukeDb()

    @Query("DELETE FROM record_table WHERE timestamp < :before")
    suspend fun purgeOldRecords(before: Long)

    @RawQuery
    fun getRecordsViaQuery(query: SupportSQLiteQuery): List<StreetPassRecord>

    @Query("SELECT Count(*) from record_table WHERE timestamp >= :startTime and timestamp < :endTime")
    fun countBTRecordsInRange(startTime: Long, endTime: Long): Int

    @Query("SELECT (SELECT Count(*) from record_table WHERE timestamp >= :startTime and timestamp < :endTime) + (SELECT Count(*) from btl_record_table WHERE timestamp >= :startTime and timestamp < :endTime)")
    fun countBTnBTLRecordsInRange(startTime: Long, endTime: Long): Int

    @Query("SELECT (SELECT Count(*) from record_table WHERE timestamp >= :startTime and timestamp < :endTime) + (SELECT Count(*) from btl_record_table WHERE timestamp >= :startTime and timestamp < :endTime)")
    suspend fun liveCountRecordsInRange(startTime: Long, endTime: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: StreetPassRecord)

    @Query("SELECT (SELECT Count(DISTINCT msg) from record_table WHERE timestamp >= :startTime and timestamp < :endTime) + (SELECT Count(DISTINCT msg) from btl_record_table WHERE timestamp >= :startTime and timestamp < :endTime)")
    fun countUniqueBTnBTLTempId(startTime: Long, endTime: Long): Int

}
