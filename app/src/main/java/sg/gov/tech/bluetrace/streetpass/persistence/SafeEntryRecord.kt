package sg.gov.tech.bluetrace.streetpass.persistence

import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import sg.gov.tech.safeentry.selfcheck.model.SafeEntryMatch

@Entity(tableName = "safe_entry_table")

class SafeEntryRecord constructor(

    @ColumnInfo(name = "venueName")
    val venueName: String,

    @ColumnInfo(name = "venueId")
    val venueId: String,

    @ColumnInfo(name = "tenantName")
    val tenantName: String,

    @ColumnInfo(name = "tenantId")
    val tenantId: String,

    @ColumnInfo(name = "postalCode")
    val postalCode: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "checkInTimeMS")
    val checkInTimeMS: Long,

    @Nullable
    @ColumnInfo(name = "groupMembers")
    val groupMembers: String? = null,

    @ColumnInfo(name = "groupMembersCount")
    val groupMembersCount: Int = 0

) {

    fun getPlaceName(): String = if (tenantName.isEmpty()) {
        venueName
    } else tenantName

    fun isCheckOut() = checkOutTimeMS != 0L

    fun matchesMatch(seMatch: SafeEntryMatch): Boolean {
        return (checkInTimeMS / 1000 == seMatch.safeentry.checkin.time &&
                postalCode == seMatch.safeentry.location.postalCode
                )
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "checkOutTimeMS")
    var checkOutTimeMS: Long = 0

    @ColumnInfo(name = "checkedOut")
    var checkedOut: Boolean = false

}
