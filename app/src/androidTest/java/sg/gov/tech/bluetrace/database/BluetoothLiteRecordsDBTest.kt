package sg.gov.tech.bluetrace.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteDao
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class BluetoothLiteRecordsDBTest {

    private val msg =
        "OLREAKXy7i/H906U/1NOdAASc76zT4jgDX2kdpwLReZ+auHwwCSOTxBEAY5eCYmzbzKlaoieOfrvYWp3GqZTUwu003du003d"
    private lateinit var bluetoothLiteRecord: StreetPassLiteRecord

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private var bluetoothLiteDao: StreetPassLiteDao? = null

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
            StreetPassRecordDatabase.TEST_MODE = true
            bluetoothLiteDao = StreetPassRecordDatabase.getDatabase(context).bleRecordDao()
        }
    }

    @After
    fun clearData() {
        bluetoothLiteDao?.nukeDb()
    }

    @Test
    fun insertAndGetLastRecord() = runBlocking {
        val rssi = Random.nextInt()
        val txPower = Random.nextInt()
        // insert record
        insertRecord(rssi, txPower)

        // get lastRecord
        val getLastRecord = getLastRecord()
        // check insert record & get lastRecord test results
        assertNotNull(getLastRecord)
        assertEquals(getLastRecord?.rssi, rssi)
        assertEquals(getLastRecord?.txPower, txPower)
    }

    @Test
    fun insertAndGetAllRecords() = runBlocking {
        insertRandomRecords()
        // get allBTLRecords
        val getAllRecords = getAllRecords()
        getAllRecords?.let {
            assertTrue(it.isNotEmpty())
        }
        assertEquals(getAllRecords?.size, 10)
    }

    @Test
    fun insertAndDeleteOldRecords1() = runBlocking {
        insertRandomRecords()
        // get allBTLRecords
        val getAllRecords = getAllRecords()
        assertEquals(getAllRecords?.size, 10)
        // delete oldRecords
        deleteOldRecords(System.currentTimeMillis())
        // check insert record & delete oldRecords test results
        val deletedRecords = getAllRecords()
        assertTrue(deletedRecords.isNullOrEmpty())
    }

    @Test
    fun insertAndDeleteOldRecords2() = runBlocking {
        insertRandomRecords()
        // get allBTLRecords
        val getAllRecords = getAllRecords()
        assertEquals(getAllRecords?.size, 10)
        // delete oldRecords
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        deleteOldRecords(cal.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getAllRecords()
        assertEquals(deletedRecords?.size, 2)
    }

    @Test
    fun insertAndDeleteOldRecords3() = runBlocking {
        insertRandomRecords()
        // get allBTLRecords
        val getAllRecords = getAllRecords()
        assertEquals(getAllRecords?.size, 10)
        // delete oldRecords
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        deleteOldRecords(cal.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getAllRecords()
        assertEquals(deletedRecords?.size, 6)
    }

    @Test
    fun insertAndDeleteOldRecords4() = runBlocking {
        insertRandomRecords()
        // get allBTLRecords
        val getAllRecords = getAllRecords()
        assertEquals(getAllRecords?.size, 10)
        // delete oldRecords
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -15)
        deleteOldRecords(cal.timeInMillis)
        // check insert record & delete oldRecords test results
        val deletedRecords = getAllRecords()
        assertEquals(deletedRecords?.size, 8)
    }

    @Test
    fun isRecordsInRangeValid1() = runBlocking {
        insertRandomRecords()
        // get btlRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        val getBTLRecordsCountInRange =
            getBTLRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btlRecordsCountInRange test result
        assertEquals(getBTLRecordsCountInRange, 2)
    }

    @Test
    fun isRecordsInRangeValid2() = runBlocking {
        insertRandomRecords()
        // get btlRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        val getBTLRecordsCountInRange =
            getBTLRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btlRecordsCountInRange test result
        assertEquals(getBTLRecordsCountInRange, 6)
    }

    @Test
    fun isRecordsInRangeValid3() = runBlocking {
        insertRandomRecords()
        // get btlRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -15)
        val getBTLRecordsCountInRange =
            getBTLRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btlRecordsCountInRange test result
        assertEquals(getBTLRecordsCountInRange, 8)
    }

    @Test
    fun isRecordsInRangeValid4() = runBlocking {
        insertRandomRecords()
        // get btlRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -20)
        val getBTLRecordsCountInRange =
            getBTLRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btlRecordsCountInRange test result
        assertEquals(getBTLRecordsCountInRange, 9)
    }

    @Test
    fun isRecordsInRangeValid5() = runBlocking {
        insertRandomRecords()
        // get btlRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -25)
        val getBTLRecordsCountInRange =
            getBTLRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btlRecordsCountInRange test result
        assertEquals(getBTLRecordsCountInRange, 10)
    }

    private suspend fun insertRecord(rssi: Int, txPower: Int) {
        bluetoothLiteRecord = StreetPassLiteRecord(msg, rssi, txPower)
        bluetoothLiteDao?.insert(bluetoothLiteRecord)
    }

    private fun getLastRecord(): StreetPassLiteRecord? {
        return bluetoothLiteDao?.getLastRecord()
    }

    private fun getAllRecords(): List<StreetPassLiteRecord>? {
        return bluetoothLiteDao?.getCurrentRecords()
    }

    private suspend fun deleteOldRecords(before: Long) {
        bluetoothLiteDao?.purgeOldRecords(before)
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
        val rssi = Random.nextInt()
        val txPower = Random.nextInt()
        bluetoothLiteRecord = StreetPassLiteRecord(msg, rssi, txPower)
        bluetoothLiteRecord.timestamp = timeInMillis
        bluetoothLiteDao?.insert(bluetoothLiteRecord)
    }

    private fun getBTLRecordsCountInRange(startTime: Long, endTime: Long): Int? {
        return bluetoothLiteDao?.countRecordsInRange(startTime, endTime)
    }
}