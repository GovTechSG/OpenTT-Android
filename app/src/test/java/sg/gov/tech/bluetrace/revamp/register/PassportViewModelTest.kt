package sg.gov.tech.bluetrace.revamp.register

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.MockitoAnnotations
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.PassportViewModel
import sg.gov.tech.bluetrace.revamp.di.modules.apiModule
import sg.gov.tech.bluetrace.revamp.di.modules.dbModule
import sg.gov.tech.bluetrace.revamp.di.modules.utilsModule
import sg.gov.tech.bluetrace.revamp.di.modules.viewModelModule

class PassportViewModelTest : KoinTest {


    private val passVm by inject<PassportViewModel>()

    @Rule
    @JvmField
    var instantTaskExecutorRule: InstantTaskExecutorRule = InstantTaskExecutorRule()

    @ObsoleteCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }


    @Before
    fun before() {
        val startKoin = startKoin {
            modules(module {
                modules(listOf(apiModule, utilsModule, dbModule, viewModelModule))
            })
        }
    }

    @After
    fun after() {
        stopKoin()
    }


    @Test
    fun `test post value Name`() {
        passVm.postValue(PassportViewModel.NAME, "Sam") { assert(it) }
    }

    @Test
    fun `test post value nationality`() {
        passVm.postValue(PassportViewModel.NATIONALITY, "India") { assert(it) }
    }

    @Test
    fun `test post value DOB`() {
        passVm.postValue(PassportViewModel.DOB, 1588636800000) { assert(it) }
    }

    @Test
    fun `test post value passport number`() {
        passVm.postValue(PassportViewModel.PASSPORT, "S23456T") { assert(it) }
    }


    @Test
    fun `test is form incomplete`() {
        var hashValues: HashMap<String, Boolean> = HashMap()
        passVm.isFormComplete(hashValues) { assert(!it) }
    }

    @Test
    fun `test is form complete`() {
        var hashValues: HashMap<String, Boolean> = HashMap()
        hashValues.put("0", true)
        hashValues.put("1", true)
        hashValues.put("2", true)
        hashValues.put("3", true)
        hashValues.put("4", true)
        passVm.isFormComplete(hashValues) { assert(it) }
    }

    @Test
    fun `test is all field valid`() {
        passVm.postValue(PassportViewModel.NAME, "Sam") { assert(it) }
        passVm.postValue(PassportViewModel.NATIONALITY, "British Indian Ocean Territory") {
            assert(
                it
            )
        }
        passVm.postValue(PassportViewModel.PASSPORT, "GT4235345") { assert(it) }
        passVm.postValue(PassportViewModel.DOB, 1588636800000) { assert(it) }
    }

    @Test
    fun `test is all field invalid`() {
        passVm.postValue(PassportViewModel.NAME, "") { assert(!it) }
        passVm.postValue(PassportViewModel.NATIONALITY, "") { assert(!it) }
        passVm.postValue(PassportViewModel.PASSPORT, " ") { assert(!it) }
        passVm.postValue(PassportViewModel.DOB, 1588636800000) { assert(it) }
    }

    @Test
    fun `test get countrycode`() {
        passVm.postValue(PassportViewModel.NATIONALITY, "British Indian Ocean Territory") {
            assert(
                it
            )
        }
    }
}