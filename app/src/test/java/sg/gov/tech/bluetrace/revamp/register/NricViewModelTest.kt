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
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.UpdateUserInfoWithPolicyVersion
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.NricViewModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.revamp.api.IApiRepository
import sg.gov.tech.bluetrace.revamp.di.modules.apiModule
import sg.gov.tech.bluetrace.revamp.di.modules.dbModule
import sg.gov.tech.bluetrace.revamp.di.modules.utilsModule
import sg.gov.tech.bluetrace.revamp.di.modules.viewModelModule
import sg.gov.tech.revamp.responseModel.RegisterModel


class NricViewModelTest : KoinTest {

    private val nricVm by inject<NricViewModel>()
    var registrationData = MutableLiveData<ApiResponseModel<out Any>>()

    @Rule
    @JvmField
    var instantTaskExecutorRule: InstantTaskExecutorRule = InstantTaskExecutorRule()

    @ObsoleteCoroutinesApi
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

//    @get:Rule
//    val koinTestRule = KoinTestRule.create {
//        modules(listOf(apiModule, utilsModule, dbModule, viewModelModule))
//    }
//    @get:Rule
//    val mockProvider = MockProviderRule.create { clazz ->
//        Mockito.mock(clazz.java)
//    }

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
        nricVm.postValueToValidateCause(
            NricViewModel.NRIC,
            "S3188211G"
        ) { fvModel -> assert(fvModel.isValid) }
    }

    @Test
    fun `test post value Name`() {
        nricVm.postValue(NricViewModel.NAME, "Sam") { assert(it) }
    }

    @Test
    fun `test post value DOI`() {
        nricVm.postValue(NricViewModel.DATE_ISSUED, 1577808000000) { assert(it) }
    }

    @Test
    fun `test post value DOB`() {
        nricVm.postValue(NricViewModel.DOB, 895593600000) { assert(it) }
    }

    @Test
    fun `test is form incomplete`() {
        var hashValues: HashMap<String, Boolean> = HashMap()
        nricVm.isFormComplete(hashValues) { assert(!it) }
    }

    @Test
    fun `test is form complete`() {
        var hashValues: HashMap<String, Boolean> = HashMap()
        hashValues.put("0", true)
        hashValues.put("1", true)
        hashValues.put("2", true)
        hashValues.put("3", true)
        hashValues.put("4", true)
        nricVm.isFormComplete(hashValues) { assert(it) }
    }

    @Test
    fun `test is all field valid`() {
        nricVm.postValue(NricViewModel.NAME, "Sam") { assert(it) }
        nricVm.postValue(NricViewModel.DOB, 895593600000) { assert(it) }
        nricVm.postValue(NricViewModel.DATE_ISSUED, 1577808000000) { assert(it) }
        nricVm.postValue(NricViewModel.NRIC, "S3188211G") { assert(!it) }
    }

    @Test
    fun `test is all field invalid`() {
        nricVm.postValue(NricViewModel.NAME, "") { assert(!it) }
        nricVm.postValue(NricViewModel.DOB, 1643377068000) { assert(!it) }
        nricVm.postValue(NricViewModel.DATE_ISSUED, 1643377068000) { assert(!it) }
        nricVm.postValue(NricViewModel.NRIC, "S38211G") { assert(!it) }
    }

    @Test
    fun `test is minor`() {
        assert(!nricVm.isMinor(895593600000))
    }

    @Test
    fun `test register user`() = runBlockingTest {
        val paramString = Gson().toJson(registerUserData1, RegisterUserData::class.java)
        val data1 = JSONObject(paramString)
        Dispatchers.setMain(mainThreadSurrogate)
        val c = Mockito.mock(IApiRepository::class.java)
        `when`(c.callSingle(data1, ApiHandler.REGISTER, RegisterModel::class.java)).thenReturn(
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

        nricVm.registerUser(registerUserData1)
    }

    @Test
    fun `test register user in valid body`() {
        val paramString = Gson().toJson(registerUserData1, RegisterUserData::class.java)
        val data1 = JSONObject(paramString)
        Dispatchers.setMain(mainThreadSurrogate)
        val c = Mockito.mock(IApiRepository::class.java)
        `when`(c.callSingle(data1, ApiHandler.REGISTER, RegisterModel::class.java)).thenReturn(
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
        nricVm.registerUser(registerUserData1)

    }

    val registerUserData1 = UpdateUserInfoWithPolicyVersion(
        IdentityType.NRIC,
        "S3188211G",
        "09-01-2020",
        "20-05-1998",
        "",
        "",
        "",
        "Test Name",
        consentedPrivacyStatementVersion = null
    )
}