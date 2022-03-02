package sg.gov.tech.bluetrace.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryDao
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*

@RunWith(AndroidJUnit4::class)
class SafeEntryDBTest {

    private val venueName = "A mock venue"
    private val tenantName = " A mock tenant"
    private val postalCode = "123456"
    private val address = "A mock address"
    private lateinit var safeEntryRecord: SafeEntryRecord

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private var safeEntryDao: SafeEntryDao? = null

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
            StreetPassRecordDatabase.TEST_MODE = true
            safeEntryDao = StreetPassRecordDatabase.getDatabase(context).safeEntryDao()
        }
    }

    @After
    fun clearData() {
        safeEntryDao?.nukeDb()
    }

    @Test
    fun insertAndGetRecord() = runBlocking {
        val venueId = "venue_11111"
        val tenantId = "tenant_11111"
        // insert record/ check-in
        insertRecord(venueId, tenantId)

        // get record
        val getRecord = getRecord(venueId, tenantId)
        // check insert record & get record test results
        assertEquals(getRecord?.venueId, venueId)
        assertEquals(getRecord?.tenantId, tenantId)
        assertEquals(getRecord?.checkOutTimeMS, 0L)
    }

    @Test
    fun insertAndGetCurrentUnexitedRecord() = runBlocking {
        val venueId = "venue_55555"
        val tenantId = "tenant_55555"
        // insert record/ check-in
        insertRecord(venueId, tenantId)

        // get currentUnexited record
        val getRecord = getRecord(venueId, tenantId)
        val getCurrentUnexitedEntryRecord = getCurrentUnexitedEntryRecord()
        // check insert record & get currentUnexited record test results
        assertEquals(getRecord?.id, getCurrentUnexitedEntryRecord?.id)
    }

    @Test
    fun insertAndUpdateRecord() = runBlocking {
        val venueId = "venue_99999"
        val tenantId = "tenant_99999"
        // insert record/ check-in
        insertRecord(venueId, tenantId)

        // update record/ check-out
        val outTime = System.currentTimeMillis()
        val getCurrentUnexitedEntryRecord = getCurrentUnexitedEntryRecord()
        getCurrentUnexitedEntryRecord?.id?.let { updateRecord(it, outTime) }
        val getRecord = getRecord(venueId, tenantId)
        // check update record test result
        assertEquals(getRecord?.checkOutTimeMS, outTime)
    }

    @Test
    fun isRecordsInRangeValid1() = runBlocking {
        insertRandomRecords()
        // get allRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        val getAllRecordsInRange =
            getAllRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getAllRecordsInRange?.size, 2)
    }

    @Test
    fun isRecordsInRangeValid2() = runBlocking {
        insertRandomRecords()
        // get allRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        val getAllRecordsInRange =
            getAllRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getAllRecordsInRange?.size, 6)
    }

    @Test
    fun isRecordsInRangeValid3() = runBlocking {
        insertRandomRecords()
        // get allRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -15)
        val getAllRecordsInRange =
            getAllRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getAllRecordsInRange?.size, 8)
    }

    @Test
    fun isRecordsInRangeValid4() = runBlocking {
        insertRandomRecords()
        // get allRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -20)
        val getAllRecordsInRange =
            getAllRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getAllRecordsInRange?.size, 9)
    }

    @Test
    fun isRecordsInRangeValid5() = runBlocking {
        insertRandomRecords()
        // get allRecordsInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -25)
        val getAllRecordsInRange =
            getAllRecordsInRange(cal.timeInMillis, System.currentTimeMillis())
        // check recordsInRange test result
        assertEquals(getAllRecordsInRange?.size, 10)
    }

    private suspend fun insertRecord(venueId: String, tenantId: String) {
        safeEntryRecord = SafeEntryRecord(
            venueName,
            venueId,
            tenantName,
            tenantId,
            postalCode,
            address,
            System.currentTimeMillis()
        )
        safeEntryDao?.insert(safeEntryRecord)
    }

    private fun getRecord(venueId: String, tenantId: String): SafeEntryRecord? {
        return safeEntryDao?.getSafeEntryRecordById(venueId, tenantId)
    }

    private fun getCurrentUnexitedEntryRecord(): SafeEntryRecord? {
        return safeEntryDao?.getCurrentUnexitedEntryRecords()?.get(0)
    }

    private fun updateRecord(id: Int, outTime: Long) {
        safeEntryDao?.exitVenue(id, outTime)
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
        safeEntryRecord = SafeEntryRecord(
            venueName,
            "venue_12345",
            tenantName,
            "tenant_12345",
            postalCode,
            address,
            timeInMillis
        )
        safeEntryDao?.insert(safeEntryRecord)
    }

    private fun getAllRecordsInRange(startTime: Long, endTime: Long): List<SafeEntryRecord>? {
        return safeEntryDao?.getAllRecordsInRange(startTime, endTime)
    }
}