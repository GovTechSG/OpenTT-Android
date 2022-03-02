package sg.gov.tech.bluetrace.favourite.persistence

import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "favourite_table", primaryKeys = ["venueId", "tenantId"])

class FavouriteRecord constructor(

    @ColumnInfo(name = "venueId")
    val venueId: String,

    @Nullable
    @ColumnInfo(name = "venueName")
    val venueName: String,

    @ColumnInfo(name = "tenantId")
    val tenantId: String,

    @Nullable
    @ColumnInfo(name = "tenantName")
    val tenantName: String,

    @ColumnInfo(name = "postalCode")
    val postalCode: String,

    @ColumnInfo(name = "address")
    val address: String

)
