package sg.gov.tech.bluetrace.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteDao
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase

@RunWith(AndroidJUnit4::class)
class FavouriteDBTest {

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private var favouriteDao: FavouriteDao? = null
        private lateinit var favouriteRecord: FavouriteRecord
        private const val venueId = "venue_12345"
        private const val tenantId = "tenant_12345"

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
            StreetPassRecordDatabase.TEST_MODE = true
            favouriteDao = StreetPassRecordDatabase.getDatabase(context).favouriteDao()
            favouriteRecord =
                FavouriteRecord(
                    venueId,
                    "A mock venue",
                    tenantId,
                    "A mock tenant",
                    "123456",
                    "A mock address"
                )
        }
    }

    @Test
    fun insertAndGetRecord() = runBlocking {
        // insert record
        insertRecord()

        // get record
        val getRecord = getRecord()
        // check insert record & get record test results
        assertEquals(getRecord?.venueId, venueId)
        assertEquals(getRecord?.tenantId, tenantId)
    }

    @Test
    fun insertAndDeleteRecord() = runBlocking {
        // insert record
        insertRecord()

        val getRecord = getRecord()
        // delete record
        getRecord?.let { deleteRecord(it) }
        // check insert record & delete record test results
        val nullRecord = getRecord()
        assertNotNull(getRecord)
        assertNull(nullRecord)
    }

    private suspend fun insertRecord() {
        favouriteDao?.insert(favouriteRecord)
    }

    private fun getRecord(): FavouriteRecord? {
        return favouriteDao?.getFavouriteRecordById(venueId, tenantId)
    }

    private fun deleteRecord(getRecord: FavouriteRecord) {
        favouriteDao?.deleteRecord(getRecord.venueId, getRecord.tenantId)
    }
}