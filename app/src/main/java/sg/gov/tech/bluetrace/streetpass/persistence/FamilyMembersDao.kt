package sg.gov.tech.bluetrace.streetpass.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FamilyMembersDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: FamilyMembersRecord)

    @Query("SELECT * from family_members_table")
    fun getAllMembers(): List<FamilyMembersRecord>

    @Query("SELECT Count(*) from family_members_table")
    fun getFamilyMembersCount(): Int

    @Query("DELETE FROM family_members_table WHERE nric = :nric ")
    fun removeFamilyMember(nric: String)

    @Query("DELETE FROM family_members_table")
    fun nukeDb()
}
