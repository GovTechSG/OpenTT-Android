package sg.gov.tech.bluetrace.streetpass.persistence

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SafeEntryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: SafeEntryRecord)

    @Query("SELECT * from safe_entry_table WHERE checkedOut = 0 and checkInTimeMS >= :checkInTimeMS ORDER BY checkInTimeMS DESC")
    fun getUnexitedEntryRecords(checkInTimeMS: Long): LiveData<List<SafeEntryRecord>>

    @Query("SELECT * from safe_entry_table WHERE checkedOut = 0 ORDER BY checkInTimeMS DESC")
    fun getCurrentUnexitedEntryRecords(): List<SafeEntryRecord>

    @Query("SELECT * from safe_entry_table WHERE checkInTimeMS >= :startTime and checkInTimeMS < :endTime ORDER BY checkInTimeMS DESC")
    fun getAllRecordsInRange(startTime: Long, endTime: Long): List<SafeEntryRecord>

    @Query("SELECT * from safe_entry_table")
    fun getAllRecords(): List<SafeEntryRecord>

    @Query("UPDATE safe_entry_table SET checkOutTimeMS = :checkOutTimeMS, checkedOut = 1  WHERE id = :id AND checkedOut = 0")
    fun exitVenue(id: Int, checkOutTimeMS: Long)

    @Query("DELETE FROM safe_entry_table WHERE checkInTimeMS < :before")
    suspend fun purgeOldSafeEntryData(before: Long)

    @Query("SELECT * from safe_entry_table WHERE venueId = :venueId and :tenantId = tenantId LIMIT 1")
    fun getSafeEntryRecordById(venueId: String?, tenantId: String?): SafeEntryRecord?

    @Query("DELETE FROM safe_entry_table")
    fun nukeDb()
}
