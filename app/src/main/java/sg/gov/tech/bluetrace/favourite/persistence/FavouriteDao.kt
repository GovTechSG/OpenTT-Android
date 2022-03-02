package sg.gov.tech.bluetrace.favourite.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavouriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: FavouriteRecord)

    @Query("SELECT * from favourite_table")
    fun getAllRecords(): List<FavouriteRecord>

    @Query("DELETE FROM favourite_table")
    fun nukeDb()

    @Query("DELETE FROM favourite_table WHERE venueId = :venueId AND tenantId = :tenantId")
    fun deleteRecord(venueId: String?, tenantId: String?)

    @Query("SELECT * from favourite_table WHERE venueId = :venueId and :tenantId = tenantId LIMIT 1")
    fun getFavouriteRecordById(venueId: String?, tenantId: String?): FavouriteRecord?

    @Query("UPDATE favourite_table SET venueName = :venueName,tenantName = :tenantName WHERE venueId = :venueId and :tenantId = tenantId")
    fun updateVenueName(venueName: String?, tenantName: String?,  venueId: String?, tenantId: String?)

}
