package sg.gov.tech.bluetrace.streetpass.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members_table")

class FamilyMembersRecord constructor(

    @ColumnInfo(name = "nric")
    var nric: String,

    @ColumnInfo(name = "nickName")
    val nickName: String
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
}
