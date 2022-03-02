package sg.gov.tech.bluetrace

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.status.persistence.StatusRecord
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random


object DBMockDataHelper {
    /**
     * helper function for saving mock test data in the database
     */
    fun saveMockDataInDB(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val cal = Calendar.getInstance()
            //StreetPassRecordStorage(context).purgeOldRecords(cal.timeInMillis)
            saveMockBTRecords(context)
            //StreetPassLiteRecordLiteStorage(context).purgeOldRecords(cal.timeInMillis)
            saveMockBTLRecords(context)
            //StatusRecordStorage(context).purgeOldRecords(cal.timeInMillis)
            saveMockStatusRecords(context)
        }
    }

    fun getSEData(day: Int, hr: Int = 0, min: Int = 0): SafeEntryRecord {
        val format1 = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, day)
        cal.set(Calendar.HOUR_OF_DAY, hr)
        cal.set(Calendar.MINUTE, min)
        cal.set(Calendar.SECOND, 0)
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

    fun saveMockCheckinData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val cal2 = Calendar.getInstance()
            cal2.add(Calendar.DATE, -16)
            var hr = cal2.get(Calendar.HOUR_OF_DAY)
            var min = cal2.get(Calendar.MINUTE)
            val seDao = StreetPassRecordDatabase.getDatabase(context).safeEntryDao()
            seDao.insert(getSEData(0))
            seDao.insert(getSEData(-1))
            seDao.insert(getSEData(-5))
            seDao.insert(getSEData(-11))
            seDao.insert(getSEData(-12))
            seDao.insert(getSEData(-16, hr, min))
            seDao.insert(getSEData(-16, hr, min + 1))
            seDao.insert(getSEData(-16, hr, min - 1))
            seDao.insert(getSEData(-16))
            seDao.insert(getSEData(-19))
            seDao.insert(getSEData(-21))
            seDao.insert(getSEData(-24))
        }
    }

    fun saveMockHistory(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {

            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -14)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val seDao = StreetPassRecordDatabase.getDatabase(context).safeEntryDao()
            seDao.insert(
                SafeEntryRecord(
                    "a fake location",
                    "venue1721",
                    "Shoppaa : -14",
                    "Shoppaa @ 123456",
                    "123456",
                    "Addressed to customers",
                    cal.timeInMillis
                )
            )

            cal.add(Calendar.DAY_OF_YEAR, -1)
            seDao.insert(
                SafeEntryRecord(
                    "a fake location",
                    "venue1721",
                    "Shoppaa : -15",
                    "Shoppaa @ 123456",
                    "123456",
                    "Addressed to customers",
                    cal.timeInMillis
                )
            )

            cal.add(Calendar.DAY_OF_YEAR, -1)
            seDao.insert(
                SafeEntryRecord(
                    "a fake location",
                    "venue1721",
                    "Shoppaa: -16",
                    "Shoppaa @ 123456",
                    "123456",
                    "Addressed to customers",
                    cal.timeInMillis
                )
            )

            cal.add(Calendar.DAY_OF_YEAR, -1)
            seDao.insert(
                SafeEntryRecord(
                    "a fake location",
                    "venue1721",
                    "Shoppaa: -17",
                    "Shoppaa @ 123456",
                    "123456",
                    "Addressed to customers",
                    cal.timeInMillis
                )
            )
        }
    }

    /**
     * TESTING purpose
     * saves mock blue trace lite data into DB
     *
     */
    private suspend fun saveMockBTLRecords(context: Context) {

        val streetPassLiteDaoImpl = StreetPassRecordDatabase.getDatabase(context).bleRecordDao()
        for (i in 0 until (10000)) {
            val record = StreetPassLiteRecord(
                "m9zpb1i/qAeCfqkCXxhwkAXFyyA\\u003d",
                Random.nextInt(),
                Random.nextInt()
            )
            streetPassLiteDaoImpl.insert(record)
        }
    }

    /**
     * TESTING purpose
     * saves mock blue trace data into DB
     */
    private suspend fun saveMockBTRecords(context: Context) {
        val streetPassDaoImpl = StreetPassRecordDatabase.getDatabase(context).recordDao()
        for (i in 0 until (10000)) {
            val record = StreetPassRecord(
                v = 2,
                msg = "OLREAKXy7i/H906U/1NOdAASc76zT4jgDX2kdpwLReZ+auHwwCSOTxBEAY5eCYmzbzKlaoieOfrvYWp3GqZTUwu003du003d",
                org = "SG_MOH",
                modelP = "MI MAX 3",
                modelC = "Pixel 3a XL",
                rssi = Random.nextInt(),
                txPower = Random.nextInt()
            )
            streetPassDaoImpl.insert(record)
        }
    }

    /**
     * saves mock status data into DB
     */
    private suspend fun saveMockStatusRecords(context: Context) {
        val streetPassDaoImpl = StreetPassRecordDatabase.getDatabase(context).statusDao()
        for (i in 0 until (1000)) {
            val record = StatusRecord(
                msg = "stop scanning"
            )
            streetPassDaoImpl.insert(record)
        }
    }

    /**
     * method to insert dummy records for testing
     */
    fun insertTestDummyLogRecords() {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        CoroutineScope(Dispatchers.IO).launch {
            for (i in 0 until 10000) {
                DBLogger.i(
                    DBLogger.LogType.SETTINGS,
                    loggerTAG,
                    "this is a dummy test data"
                )
            }
        }
    }
}

