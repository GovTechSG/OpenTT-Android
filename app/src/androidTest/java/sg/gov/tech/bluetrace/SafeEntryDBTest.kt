package sg.gov.tech.bluetrace.database.sg.gov.tech.bluetrace

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryDao
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@RunWith(AndroidJUnit4::class)
class SafeEntryDBTest {

    private lateinit var db: StreetPassRecordDatabase
    private var safeEntryDao: SafeEntryDao? = null

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, StreetPassRecordDatabase::class.java
        ).build()
        safeEntryDao = db.safeEntryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertData() = runBlocking {
        val cal2 = Calendar.getInstance()
        cal2.add(Calendar.DATE, -25)
        var hr = cal2.get(Calendar.HOUR_OF_DAY)
        var min = cal2.get(Calendar.MINUTE)
//        cal2..set(Calendar.HOUR_OF_DAY, hr)
//        cal2..set(Calendar.MINUTE, min)
        safeEntryDao?.insert(getSEData(0))
        safeEntryDao?.insert(getSEData(-1))
        safeEntryDao?.insert(getSEData(-5))
        safeEntryDao?.insert(getSEData(-11))
        safeEntryDao?.insert(getSEData(-12))
        safeEntryDao?.insert(getSEData(-16))
        safeEntryDao?.insert(getSEData(-16, hr - 1))
        safeEntryDao?.insert(getSEData(-16, hr, min - 1))
        safeEntryDao?.insert(getSEData(-16, hr, min + 1))
        safeEntryDao?.insert(getSEData(-16, hr + 1, min))
        safeEntryDao?.insert(getSEData(-16, hr + 1))
        safeEntryDao?.insert(getSEData(-16, hr + 2))
        safeEntryDao?.insert(getSEData(-19))
        safeEntryDao?.insert(getSEData(-21))
        safeEntryDao?.insert(getSEData(-24))
        safeEntryDao?.insert(getSEData(-25, hr, min + 1))
        safeEntryDao?.insert(getSEData(-25, hr + 1, min))
        safeEntryDao?.insert(getSEData(-25, hr + 1))
        safeEntryDao?.insert(getSEData(-25, hr + 2))

        safeEntryDao?.insert(getSEData(-25))

        safeEntryDao?.insert(getSEData(-25, hr - 1))
        safeEntryDao?.insert(getSEData(-25, hr, min - 1))

        val records = safeEntryDao?.getAllRecords()
        assertEquals(records?.size, 22)
    }

    @Test
    @Throws(Exception::class)
    fun purgeOldRecords() = runBlocking {
        insertData()
        val format1 = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val cal2 = Calendar.getInstance()
        cal2.add(Calendar.DATE, -25)
        Log.d("DD", "formated date dete" + format1.format(cal2.time))
        val purgeThreshold = cal2.timeInMillis
        safeEntryDao?.purgeOldSafeEntryData(purgeThreshold)
        val records = safeEntryDao?.getAllRecords()
        records?.forEach { Log.d("DD", "formated date post" + it.tenantName) }
//        assertEquals(records?.size, 9)
        assertEquals(records?.size, 19)
    }

    fun getSEData(day: Int, hr: Int = 0, min: Int = 0): SafeEntryRecord {
        val format1 = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, day)
        cal.set(Calendar.HOUR_OF_DAY, hr)
        cal.set(Calendar.MINUTE, min)
        cal.set(Calendar.SECOND, 0)
        Log.d("DD", "formated date" + format1.format(cal.time))
        return SafeEntryRecord(
            "a fake location",
            "venue1721",
            "Sam : $day" + format1.format(cal.time),
            "Sam @ 123456",
            "123456",
            "Addressed to customers",
            cal.timeInMillis
        )
    }
}
