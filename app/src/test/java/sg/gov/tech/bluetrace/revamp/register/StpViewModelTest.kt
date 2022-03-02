package sg.gov.tech.bluetrace.revamp.register

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.UpdateUserInfoWithPolicyVersion
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.StpViewModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.revamp.api.IApiRepository
import sg.gov.tech.bluetrace.revamp.di.modules.apiModule
import sg.gov.tech.bluetrace.revamp.di.modules.dbModule
import sg.gov.tech.bluetrace.revamp.di.modules.utilsModule
import sg.gov.tech.bluetrace.revamp.di.modules.viewModelModule
import sg.gov.tech.revamp.responseModel.RegisterModel

class StpViewModelTest : KoinTest {
    private val stpVm by inject<StpViewModel>()
    var registrationData = MutableLiveData<ApiResponseModel<out Any>>()

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
        startKoin {
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
    fun `test post value to ValidateCause`() {
        stpVm.postValueToValidateCause(
            StpViewModel.STP,
            "F2476745X"
        ) { fvModel -> assert(fvModel.isValid) }
    }

    @Test
    fun `test post value Name`() {
        stpVm.postValue(StpViewModel.NAME, "Sam") { assert(it) }
    }

    @Test
    fun `test post value DOI`() {
        stpVm.postValue(StpViewModel.ISSUED_DATE, 1577808000000) { assert(it) }
    }


    @Test
    fun `test is form incomplete`() {
        var hashValues: HashMap<String, Boolean> = HashMap()
        stpVm.isFormComplete(hashValues) { assert(!it) }
    }

    @Test
    fun `test is form complete`() {
        var hashValues: HashMap<String, Boolean> = HashMap()
        hashValues.put("0", true)
        hashValues.put("1", true)
        hashValues.put("2", true)
        hashValues.put("3", true)
        stpVm.isFormComplete(hashValues) { assert(it) }
    }

    @Test
    fun `test is all field valid`() {
        stpVm.postValue(StpViewModel.NAME, "Sam") { assert(it) }
        stpVm.postValue(StpViewModel.ISSUED_DATE, 895593600000) { assert(it) }
        stpVm.postValue(StpViewModel.STP, "S3188211G") { assert(!it) }
    }

    @Test
    fun `test is all field invalid`() {
        stpVm.postValue(StpViewModel.NAME, "") { assert(!it) }
        stpVm.postValue(StpViewModel.ISSUED_DATE, 1643377068000) { assert(!it) }
        stpVm.postValue(StpViewModel.STP, "S38211G") { assert(!it) }
    }

    @Test
    fun `test register user`() = runBlockingTest {
        val paramString = Gson().toJson(registerUserData1, RegisterUserData::class.java)
        val data1 = JSONObject(paramString)
        Dispatchers.setMain(mainThreadSurrogate)
        val c = Mockito.mock(IApiRepository::class.java)
        Mockito.`when`(c.callSingle(data1, ApiHandler.REGISTER, RegisterModel::class.java))
            .thenReturn(
                Single.just(
                    ApiResponseModel(
                        true, RegisterModel("", "Success")
                    )
                )
            )
        registrationData.observeForever { }
        registrationData.value?.let {
            assert(it.isSuccess)
        }

        stpVm.registerUser(registerUserData1)
    }

    @Test
    fun `test register user in valid body`() {
        val paramString = Gson().toJson(registerUserData1, RegisterUserData::class.java)
        val data1 = JSONObject(paramString)
        Dispatchers.setMain(mainThreadSurrogate)
        val c = Mockito.mock(IApiRepository::class.java)
        Mockito.`when`(c.callSingle(data1, ApiHandler.REGISTER, RegisterModel::class.java))
            .thenReturn(
                Single.just(
                    ApiResponseModel(
                        false, RegisterModel("", "Success")
                    )
                )
            )
        registrationData.observeForever { }
        registrationData.value?.let {
            assert(!it.isSuccess)
        }
        stpVm.registerUser(registerUserData1)

    }

    val registerUserData1 = UpdateUserInfoWithPolicyVersion(
        IdentityType.FIN_STP,
        "S3188211G",
        "09-01-2020",
//                    dob,
        "",
        "",
        "",
        "",
        "Test Name",
        consentedPrivacyStatementVersion = null
    )

}
