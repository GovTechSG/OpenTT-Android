package sg.gov.tech.bluetrace.logging.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_table")

class LogRecord constructor(

    @ColumnInfo(name = "level")
    val level: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "tag")
    val tag: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "metaData")
    val metaData: String? = null

) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo(name = "time")
    var time: Long = System.currentTimeMillis()

}