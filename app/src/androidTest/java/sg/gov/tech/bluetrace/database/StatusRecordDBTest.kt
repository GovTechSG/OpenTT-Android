package sg.gov.tech.bluetrace.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import sg.gov.tech.bluetrace.database.utils.LiveDataTestUtil
import sg.gov.tech.bluetrace.status.persistence.StatusRecord
import sg.gov.tech.bluetrace.status.persistence.StatusRecordDao
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*

@RunWith(AndroidJUnit4::class)
class StatusRecordDBTest {

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private var statusRecordDao: StatusRecordDao? = null
        private lateinit var statusRecord: StatusRecord
        private const val msg = "A mock message"

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
            StreetPassRecordDatabase.TEST_MODE = true
            statusRecordDao = StreetPassRecordDatabase.getDatabase(context).statusDao()
            statusRecord = StatusRecord(msg)
        }
    }

    @After
    fun clearData() {
        statusRecordDao?.nukeDb()
    }

    @Test
    fun insertAndGetRecord() = runBlocking {
        // insert record
        insertRecord()

        // get record
        val getRecord = getRecord()
        // check insert record & get record test results
        assertEquals(getRecord?.msg, msg)
    }

    @Test
    fun insertAndGetCurrentRecords() = runBlocking {
        // get currentRecords
        val getCurrentRecordsEmpty = getCurrentRecords()

        // insert record
        insertRecord()

        // get currentRecords
        val getCurrentRecords = getCurrentRecords()
        // check insert record & get currentRecords record test results
        assertTrue(getCurrentRecordsEmpty.isNullOrEmpty())
        getCurrentRecords?.let {
            assertEquals(it[0].msg, msg)
        }
        assertNotNull(getCurrentRecords)
    }

    @Test
    fun insertAndDeleteOldRecords() = runBlocking {
        insertRandomRecords()
        // get allRecords
        val allRecords = getCurrentRecords()
        assertEquals(allRecords?.size, 10)
        // delete oldRecords
        deleteOldRecords(System.currentTimeMillis())
        // check insert record & delete oldRecords test results
        val deletedRecords = getCurrentRecords()
        assertTrue(deletedRecords.isNullOrEmpty())
    }

    @Test
    fun insertAndDeleteOldRecords2() = runBlocking {
        insertRandomRecords()
        // get allRecords
        val allRecords = getCurrentRecords()
        assertEquals(allRecords?.size, 10)
        // delete oldRecords
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        deleteOldRecords(cal.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getCurrentRecords()
        assertEquals(deletedRecords?.size, 2)
    }

    @Test
    fun insertAndDeleteOldRecords3() = runBlocking {
        insertRandomRecords()
        // get allRecords
        val allRecords = getCurrentRecords()
        assertEquals(allRecords?.size, 10)
        // delete oldRecords
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        deleteOldRecords(cal.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getCurrentRecords()
        assertEquals(deletedRecords?.size, 6)
    }

    @Test
    fun insertAndDeleteOldRecords4() = runBlocking {
        insertRandomRecords()
        // get allRecords
        val allRecords = getCurrentRecords()
        assertEquals(allRecords?.size, 10)
        // delete oldRecords
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -15)
        deleteOldRecords(cal.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getCurrentRecords()
        assertEquals(deletedRecords?.size, 8)
    }

    private suspend fun insertRecord() {
        statusRecordDao?.insert(statusRecord)
    }

    private fun getRecord(): StatusRecord? {
        return statusRecordDao?.getMostRecentRecord(msg)?.let { LiveDataTestUtil.getValue(it) }
    }

    private fun getCurrentRecords(): List<StatusRecord>? {
        return statusRecordDao?.getCurrentRecords()
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
        statusRecord.timestamp = timeInMillis
        statusRecordDao?.insert(statusRecord)
    }

    private suspend fun deleteOldRecords(before: Long) {
        statusRecordDao?.purgeOldRecords(before)
    }
}