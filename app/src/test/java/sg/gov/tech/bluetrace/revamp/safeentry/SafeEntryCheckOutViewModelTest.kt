package sg.gov.tech.bluetrace.revamp.safeentry

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.mockk.mockk
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.revamp.api.IApiRepository
import sg.gov.tech.bluetrace.revamp.di.modules.apiModule
import sg.gov.tech.bluetrace.revamp.di.modules.dbModule
import sg.gov.tech.bluetrace.revamp.di.modules.utilsModule
import sg.gov.tech.bluetrace.revamp.di.modules.viewModelModule
import sg.gov.tech.bluetrace.revamp.requestModel.CheckOutRequestModel
import sg.gov.tech.bluetrace.revamp.responseModel.CheckOutResponseModel

class SafeEntryCheckOutViewModelTest : KoinTest {

    private val checkOutVm by inject<SafeEntryCheckOutViewModel>()
    private var checkOutApiResponse = MutableLiveData<ApiResponseModel<out Any>>()

    private val userData = RegisterUserData(
        IdentityType.NRIC,
        "S3188211G",
        "01-01-2020",
        "01-01-2000",
        "",
        "",
        "",
        "A mock Name"
    )

    private val venueData = QrResultDataModel(
        "A mock venue",
        "venue_11111",
        "A mock tenant",
        "tenant_11111",
        "123456",
        "A mock address",
        0,
        0,
        0
    )

    private val checkOutRequestModel = CheckOutRequestModel(
        venueData.venueId,
        venueData.tenantId,
        venueData.venueName,
        userData.id
    )

    @Rule
    @JvmField
    var instantTaskExecutorRule: InstantTaskExecutorRule = InstantTaskExecutorRule()

    @ObsoleteCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            TracerApp.AppContext = mockk(relaxed = true)
        }
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Before
    fun before() {
        startKoin {
            androidContext(TracerApp.AppContext)
            modules(module {
                modules(listOf(apiModule, utilsModule, dbModule, viewModelModule))
            })
        }
    }

    @After
    fun after() {
        stopKoin()
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    @Test
    fun testValidCheckOut() {
        val paramString = Gson().toJson(checkOutRequestModel, CheckOutRequestModel::class.java)
        val data = JSONObject(paramString)
        Dispatchers.setMain(mainThreadSurrogate)
        val repository = Mockito.mock(IApiRepository::class.java)
        Mockito.`when`(
            repository.callSingle(
                data,
                ApiHandler.CHECK_OUT,
                CheckOutResponseModel::class.java
            )
        )
            .thenReturn(
                Single.just(
                    ApiResponseModel(
                        true, CheckOutResponseModel("", "Success")
                    )
                )
            )
        checkOutApiResponse.observeForever { }
        checkOutApiResponse.value?.let {
            assert(it.isSuccess)
        }
        checkOutVm.postSEEntryCheckOut(userData, arrayListOf(), venueData)
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    @Test
    fun testInvalidCheckOut() {
        val paramString = Gson().toJson(checkOutRequestModel, CheckOutRequestModel::class.java)
        val data = JSONObject(paramString)
        Dispatchers.setMain(mainThreadSurrogate)
        val repository = Mockito.mock(IApiRepository::class.java)
        Mockito.`when`(
            repository.callSingle(
                data,
                ApiHandler.CHECK_OUT,
                CheckOutResponseModel::class.java
            )
        )
            .thenReturn(
                Single.just(
                    ApiResponseModel(
                        false, CheckOutResponseModel("", "Success")
                    )
                )
            )
        checkOutApiResponse.observeForever { }
        checkOutApiResponse.value?.let {
            assert(!it.isSuccess)
        }
        checkOutVm.postSEEntryCheckOut(userData, arrayListOf(), venueData)
    }
}