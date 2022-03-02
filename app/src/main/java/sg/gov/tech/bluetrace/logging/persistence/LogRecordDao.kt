package sg.gov.tech.bluetrace.logging.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LogRecordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(logRecord: LogRecord)

    @Query("DELETE FROM log_table WHERE time <= :before")
    suspend fun purgeOldRecords(before: Long)

    @Query("SELECT * from log_table WHERE time >= :startTime and time < :endTime ORDER BY time ASC LIMIT :pageSize OFFSET :itemIndex")
    fun getPagedRecords(
        pageSize: Int,
        itemIndex: Int,
        startTime: Long,
        endTime: Long
    ): List<LogRecord>

    @Query("SELECT * from log_table WHERE time >= :startTime and time < :endTime ORDER BY time DESC")
    fun getLogRecords(startTime: Long, endTime: Long): List<LogRecord>

    @Query("DELETE FROM log_table")
    fun nukeDb()

}