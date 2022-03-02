package sg.gov.tech.bluetrace.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.logging.persistence.LogRecord
import sg.gov.tech.bluetrace.logging.persistence.LogRecordDao
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*

@RunWith(AndroidJUnit4::class)
class LogRecordDBTest {

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private var logRecordDao: LogRecordDao? = null
        private lateinit var logRecord: LogRecord

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
            StreetPassRecordDatabase.TEST_MODE = true
            logRecordDao = StreetPassRecordDatabase.getDatabase(context).logRecordDao()
            logRecord = LogRecord(
                DBLogger.LogLevel.DEBUG.name,
                DBLogger.LogType.SAFEENTRY.name,
                "A mock tag",
                "A mock message"
            )
        }
    }

    @After
    fun clearData() {
        logRecordDao?.nukeDb()
    }

    @Test
    fun insertAndDeleteOldRecords1() = runBlocking {
        insertRandomRecords()
        // get allLogRecords
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val getAllLogRecords =
            getLogRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        assertEquals(getAllLogRecords?.size, 10)
        // delete oldRecords
        deleteOldRecords(System.currentTimeMillis())
        // check insert record & delete oldRecords test results
        val deletedRecords = getLogRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        assertTrue(deletedRecords.isNullOrEmpty())
    }

    @Test
    fun insertAndDeleteOldRecords2() = runBlocking {
        insertRandomRecords()
        // get allLogRecords
        val cal1 = Calendar.getInstance()
        cal1.add(Calendar.DAY_OF_YEAR, -30)
        val getAllLogRecords =
            getLogRecordsInRange(cal1.timeInMillis, System.currentTimeMillis())
        assertEquals(getAllLogRecords?.size, 10)
        // delete oldRecords
        val cal2 = Calendar.getInstance()
        cal2.add(Calendar.DAY_OF_YEAR, -5)
        deleteOldRecords(cal2.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getLogRecordsInRange(cal1.timeInMillis, System.currentTimeMillis())
        assertEquals(deletedRecords?.size, 2)
    }

    @Test
    fun insertAndDeleteOldRecords3() = runBlocking {
        insertRandomRecords()
        // get allLogRecords
        val cal1 = Calendar.getInstance()
        cal1.add(Calendar.DAY_OF_YEAR, -30)
        val getAllLogRecords =
            getLogRecordsInRange(cal1.timeInMillis, System.currentTimeMillis())
        assertEquals(getAllLogRecords?.size, 10)
        // delete oldRecords
        val cal2 = Calendar.getInstance()
        cal2.add(Calendar.DAY_OF_YEAR, -10)
        deleteOldRecords(cal2.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getLogRecordsInRange(cal1.timeInMillis, System.currentTimeMillis())
        assertEquals(deletedRecords?.size, 6)
    }

    @Test
    fun insertAndDeleteOldRecords4() = runBlocking {
        insertRandomRecords()
        // get allLogRecords
        val cal1 = Calendar.getInstance()
        cal1.add(Calendar.DAY_OF_YEAR, -30)
        val getAllLogRecords =
            getLogRecordsInRange(cal1.timeInMillis, System.currentTimeMillis())
        assertEquals(getAllLogRecords?.size, 10)
        // delete oldRecords
        val cal2 = Calendar.getInstance()
        cal2.add(Calendar.DAY_OF_YEAR, -15)
        deleteOldRecords(cal2.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getLogRecordsInRange(cal1.timeInMillis, System.currentTimeMillis())
        assertEquals(deletedRecords?.size, 8)
    }

    @Test
    fun isRecordsInRangeValid1() = runBlocking {
        insertRandomRecords()
        // get logRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        val getLogRecordsInRange =
            getLogRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getLogRecordsInRange?.size, 2)
    }

    @Test
    fun isRecordsInRangeValid2() = runBlocking {
        insertRandomRecords()
        // get logRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        val getLogRecordsInRange =
            getLogRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getLogRecordsInRange?.size, 6)
    }

    @Test
    fun isRecordsInRangeValid3() = runBlocking {
        insertRandomRecords()
        // get logRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -15)
        val getLogRecordsInRange =
            getLogRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getLogRecordsInRange?.size, 8)
    }

    @Test
    fun isRecordsInRangeValid4() = runBlocking {
        insertRandomRecords()
        // get logRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -20)
        val getLogRecordsInRange =
            getLogRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getLogRecordsInRange?.size, 9)
    }

    @Test
    fun isRecordsInRangeValid5() = runBlocking {
        insertRandomRecords()
        // get logRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -25)
        val getLogRecordsInRange =
            getLogRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getLogRecordsInRange?.size, 10)
    }

    private suspend fun insertRandomRecords() {
        var cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -21)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.MILLISECOND, 0)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -8)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -11)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -13)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -17)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -9)
        insertRecord(cal.timeInMillis)
        cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        insertRecord(cal.timeInMillis)
    }

    private suspend fun insertRecord(timeInMillis: Long) {
        logRecord.time = timeInMillis
        logRecordDao?.insert(logRecord)
    }

    private suspend fun deleteOldRecords(before: Long) {
        logRecordDao?.purgeOldRecords(before)
    }

    private fun getLogRecordsInRange(startTime: Long, endTime: Long): List<LogRecord>? {
        return logRecordDao?.getLogRecords(startTime, endTime)
    }
}