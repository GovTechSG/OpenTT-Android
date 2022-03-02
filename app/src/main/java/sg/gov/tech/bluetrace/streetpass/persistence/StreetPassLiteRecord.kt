package sg.gov.tech.bluetrace.streetpass.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "btl_record_table")
class StreetPassLiteRecord constructor(
    
    @ColumnInfo(name = "msg")
    var msg: String,

    @ColumnInfo(name = "rssi")
    val rssi: Int,

    @ColumnInfo(name = "txPower")
    val txPower: Int?
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "timestamp")
    var timestamp: Long = System.currentTimeMillis()
}
