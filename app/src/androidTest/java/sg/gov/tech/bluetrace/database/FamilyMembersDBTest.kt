package sg.gov.tech.bluetrace.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersDao
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase

@RunWith(AndroidJUnit4::class)
class FamilyMembersDBTest {

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private var familyMembersDao: FamilyMembersDao? = null
        private lateinit var familyMembersRecord: FamilyMembersRecord
        private const val nric = "G5996561T"
        private const val nickName = "Mock nickName"

        @BeforeClass
        @JvmStatic
        fun setup() {
            // things to execute once and keep around for the class
            StreetPassRecordDatabase.TEST_MODE = true
            familyMembersDao = StreetPassRecordDatabase.getDatabase(context).familyMemberDao()
            familyMembersRecord = FamilyMembersRecord(nric, nickName)
        }
    }

    @After
    fun clearData() {
        familyMembersDao?.nukeDb()
    }

    @Test
    fun insertAndGetRecord() = runBlocking {
        // get allMembers
        val getAllMembersEmpty = getAllMembers()

        // insert member
        insertRecord()

        // get allMembers
        val getAllMembers = getAllMembers()
        // check insert member & get allMembers test results
        assertTrue(getAllMembersEmpty.isNullOrEmpty())
        getAllMembers?.let {
            assertEquals(it[0].nric, nric)
            assertEquals(it[0].nickName, nickName)
        }
        assertNotNull(getAllMembers)
    }

    @Test
    fun insertAndDeleteRecord() = runBlocking {
        // insert member
        insertRecord()

        val getAllMembers = getAllMembers()
        // delete member
        getAllMembers?.let {
            deleteRecord(it[0].nric)
        }
        // check insert member & delete member test results
        val emptyRecords = getAllMembers()
        assertNotNull(getAllMembers)
        getAllMembers?.isNotEmpty()?.let { assertTrue(it) }
        assertEquals(emptyRecords?.size, 0)
    }

    @Test
    fun insertAndGetMembersCount() = runBlocking {
        // get membersCount
        val getEmptyMembersCount = getMembersCount()

        // insert member
        insertRecord()
        // get membersCount
        val getMembersCount1 = getMembersCount()
        // check insert member & get membersCount test results
        assertEquals(getEmptyMembersCount, 0)
        assertNotNull(getMembersCount1)
        assertEquals(getMembersCount1, 1)

        // insert members
        insertRecord()
        insertRecord()
        insertRecord()
        // get membersCount
        val getMembersCount2 = getMembersCount()
        assertNotNull(getMembersCount2)
        assertEquals(getMembersCount2, 4)
    }

    private suspend fun insertRecord() {
        familyMembersDao?.insert(familyMembersRecord)
    }

    private fun getAllMembers(): List<FamilyMembersRecord>? {
        return familyMembersDao?.getAllMembers()
    }

    private fun getMembersCount(): Int? {
        return familyMembersDao?.getFamilyMembersCount()
    }

    private fun deleteRecord(nric: String) {
        familyMembersDao?.removeFamilyMember(nric)
    }
}