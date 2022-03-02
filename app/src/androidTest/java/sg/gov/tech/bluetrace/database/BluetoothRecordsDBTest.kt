package sg.gov.tech.bluetrace.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDao
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class BluetoothRecordsDBTest {

    private val msg =
        "OLREAKXy7i/H906U/1NOdAASc76zT4jgDX2kdpwLReZ+auHwwCSOTxBEAY5eCYmzbzKlaoieOfrvYWp3GqZTUwu003du003d"
    private val org = "GovTech"
    private val modelP = "MI MAX 3"
    private val modelC = "Pixel 3 XL"
    private lateinit var bluetoothRecord: StreetPassRecord

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private var bluetoothRecordDao: StreetPassRecordDao? = null

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
            StreetPassRecordDatabase.TEST_MODE = true
            bluetoothRecordDao = StreetPassRecordDatabase.getDatabase(context).recordDao()
        }
    }

    @After
    fun clearData() {
        bluetoothRecordDao?.nukeDb()
    }

    @Test
    fun insertAndGetLastRecord() = runBlocking {
        val v = 3
        val rssi = Random.nextInt()
        val txPower = Random.nextInt()
        // insert record
        insertRecord(v, rssi, txPower)

        // get lastRecord
        val getLastRecord = getLastRecord()
        // check insert record & get lastRecord test results
        assertNotNull(getLastRecord)
        assertEquals(getLastRecord?.v, v)
        assertEquals(getLastRecord?.rssi, rssi)
        assertEquals(getLastRecord?.txPower, txPower)
    }

    @Test
    fun insertAndGetAllRecords() = runBlocking {
        insertRandomRecords()
        // get allBTRecords
        val getAllRecords = getAllRecords()
        getAllRecords?.let {
            assertTrue(it.isNotEmpty())
        }
        assertEquals(getAllRecords?.size, 10)
    }

    @Test
    fun insertAndDeleteOldRecords1() = runBlocking {
        insertRandomRecords()
        // get allBTRecords
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
        // get allBTRecords
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
        // get allBTRecords
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
        // get allBTRecords
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
        // get btRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -5)
        val getBTRecordsCountInRange =
            getBTRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btRecordsCountInRange test result
        assertEquals(getBTRecordsCountInRange, 2)
    }

    @Test
    fun isRecordsInRangeValid2() = runBlocking {
        insertRandomRecords()
        // get btRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -10)
        val getBTRecordsCountInRange =
            getBTRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btRecordsCountInRange test result
        assertEquals(getBTRecordsCountInRange, 6)
    }

    @Test
    fun isRecordsInRangeValid3() = runBlocking {
        insertRandomRecords()
        // get btRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -15)
        val getBTRecordsCountInRange =
            getBTRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btRecordsCountInRange test result
        assertEquals(getBTRecordsCountInRange, 8)
    }

    @Test
    fun isRecordsInRangeValid4() = runBlocking {
        insertRandomRecords()
        // get btRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -20)
        val getBTRecordsCountInRange =
            getBTRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btRecordsCountInRange test result
        assertEquals(getBTRecordsCountInRange, 9)
    }

    @Test
    fun isRecordsInRangeValid5() = runBlocking {
        insertRandomRecords()
        // get btRecordsCountInRange
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -25)
        val getBTRecordsCountInRange =
            getBTRecordsCountInRange(cal.timeInMillis, System.currentTimeMillis())
        // check btRecordsCountInRange test result
        assertEquals(getBTRecordsCountInRange, 10)
    }

    private suspend fun insertRecord(v: Int, rssi: Int, txPower: Int) {
        bluetoothRecord = StreetPassRecord(v, msg, org, modelP, modelC, rssi, txPower)
        bluetoothRecordDao?.insert(bluetoothRecord)
    }

    private fun getLastRecord(): StreetPassRecord? {
        return bluetoothRecordDao?.getLastRecord()
    }

    private fun getAllRecords(): List<StreetPassRecord>? {
        return bluetoothRecordDao?.getCurrentRecords()
    }

    private suspend fun deleteOldRecords(before: Long) {
        bluetoothRecordDao?.purgeOldRecords(before)
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
        bluetoothRecord = StreetPassRecord(5, msg, org, modelP, modelC, rssi, txPower)
        bluetoothRecord.timestamp = timeInMillis
        bluetoothRecordDao?.insert(bluetoothRecord)
    }

    private fun getBTRecordsCountInRange(startTime: Long, endTime: Long): Int? {
        return bluetoothRecordDao?.countBTRecordsInRange(startTime, endTime)
    }
}