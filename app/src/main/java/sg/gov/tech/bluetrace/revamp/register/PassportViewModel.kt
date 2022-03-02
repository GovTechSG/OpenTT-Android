package sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import sg.gov.tech.bluetrace.extentions.onApiError
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.CountriesLocalDataProvider
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.UpdateUserInfoWithPolicyVersion
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.revamp.utils.FieldValidationsV2
import java.util.*
import kotlin.collections.HashMap

class PassportViewModel(val api: ApiHandler, val fv: FieldValidationsV2) : ViewModel() {

    companion object {
        private const val TAG = "PassportViewModel"
        const val DOB = "dob"
        const val NAME = "name"
        const val NATIONALITY = "nationality"
        const val PASSPORT = "passport"
        const val DECLARATION = "declaration"
    }

    private var disposables = CompositeDisposable()
    lateinit var registrationData: MutableLiveData<ApiResponseModel<out Any>>
    val countries: MutableList<String> by lazy {
        CountriesLocalDataProvider().provideFiltered(arrayOf("Singapore"))
    }
    var mapEnable = HashMap<String, Boolean>()
    var checksIsRegisterEnable: MutableLiveData<HashMap<String, Boolean>> =
        MutableLiveData<HashMap<String, Boolean>>()

    fun postValue(key: String, value: Any, isValid: (Boolean) -> Unit) {
        isValidaFieldValue(key, value) { valid ->
            addHash(key, valid)
            isValid.invoke(valid)
        }

    }

    private fun isValidaFieldValue(key: String, value: Any, isValid: (Boolean) -> Unit) {
        var valid = when (key) {
            NATIONALITY -> fv.isValidNationality(value as String)
            PASSPORT -> fv.isValidPassportNumber(value as String)
            DOB -> fv.isValidDateOfBirth(value as Long)
            NAME -> fv.isValidName(value as String)
            else -> false
        }
        isValid.invoke(valid)
    }

    fun addHash(key: String, value: Boolean) {
        mapEnable.putAll(hashMapOf(key to value))
        checksIsRegisterEnable.value = mapEnable
    }

    fun registerUser(registerUserData: UpdateUserInfoWithPolicyVersion) {
        registrationData = MutableLiveData<ApiResponseModel<out Any>>()
        var result = api.registerUser(registerUserData)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                CentralLog.e(loggerTAG, "Failed to updateUserInfo: ${e.message}")
                DBLogger.e(
                    DBLogger.LogType.USERDATAREGISTERATION,
                    loggerTAG,
                    "Failed to updateUserInfo: ${e.message}",
                    null
                )
                var error = onApiError(e)
                registrationData.postValue(
                    ApiResponseModel(
                        error.isSuccess,
                        error.message,
                        error.code
                    )
                )
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                registrationData.postValue(data as ApiResponseModel<out Any>?)
            }

        })
        disposables.addAll(disposable)
    }

    fun isValidNationality(nationality: String, onCheck: (Boolean) -> Unit) {
        onCheck.invoke(fv.isValidNationality(nationality))
    }

    fun isFormComplete(hash: HashMap<String, Boolean>, onDo: (Boolean) -> Unit) {
        if (hash.size == 5 && !hash.containsValue(false)) {
            onDo.invoke(true)
        } else
            onDo.invoke(false)
    }

    fun isAllFieldValid(onDo: (Boolean) -> Unit) {
        var hash = checksIsRegisterEnable.value
        if (hash != null) {
            if (hash.size == 5 && !hash.containsValue(false))
                onDo.invoke(true)
            else
                onDo.invoke(false)
        } else
            onDo.invoke(false)
    }

    fun getCountryCode(countryName: String): String? {
        var countryCode = Locale.getISOCountries().find {
            val locale = Locale(Locale.getDefault().language, it)
            (locale.getDisplayCountry(Locale.ENGLISH) == countryName || locale.getDisplayCountry(
                Locale(Locale.getDefault().language)
            ) == countryName)
        }
        CentralLog.d(TAG, "Country code found: ${countryCode}")
        return countryCode
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}






