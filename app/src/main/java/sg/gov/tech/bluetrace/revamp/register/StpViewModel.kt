package sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import sg.gov.tech.bluetrace.RemoteConfigUtils
import sg.gov.tech.bluetrace.extentions.onApiError
import sg.gov.tech.bluetrace.fragment.model.PrivacyPolicyValidationModel
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.UpdateUserInfoWithPolicyVersion
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.revamp.utils.IDValidationModel
import sg.gov.tech.revamp.utils.FieldValidationsV2

class StpViewModel(val api: ApiHandler, val fv: FieldValidationsV2) : ViewModel() {

    companion object {
        private const val TAG = "DpViewModel"
        const val NAME = "name"
        const val STP = "stp"
        const val DECLARATION = "declaration"
        const val ISSUED_DATE = "date_issued"

    }

    private var hashSize = 4
    private var disposables = CompositeDisposable()
    lateinit var registrationData: MutableLiveData<ApiResponseModel<out Any>>
    var mapEnable = HashMap<String, Boolean>()
    var checksIsRegisterEnable: MutableLiveData<HashMap<String, Boolean>> =
        MutableLiveData<HashMap<String, Boolean>>()

    fun postValue(key: String, value: Any, isValid: (Boolean) -> Unit) {
        isValidaFieldValue(key, value) { valid ->
            addHash(key, valid)
            isValid.invoke(valid)
        }

    }

    fun postValueToValidateCause(key: String, value: Any,isForce:Boolean = false, isValid: (IDValidationModel) -> Unit) {
        var cause = fv.validNRICWithCause(value as String, isNric = false, isFin = true, forceCheck = isForce)
        addHash(key, cause.isValid)
        isValid.invoke(cause)
    }

    private fun isValidaFieldValue(key: String, value: Any, isValid: (Boolean) -> Unit) {
        var valid = when (key) {
            NAME -> fv.isValidName(value as String)
            ISSUED_DATE -> fv.isValidDate(value as Long)
            else -> false
        }
        isValid.invoke(valid)
    }

    fun addHash(key: String, value: Boolean) {
        mapEnable.putAll(hashMapOf(key to value))
        checksIsRegisterEnable.value = mapEnable
    }

    fun registerUser(registerUserData: UpdateUserInfoWithPolicyVersion) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        registrationData = MutableLiveData<ApiResponseModel<out Any>>()
        var result = api.registerUser(registerUserData)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
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

    fun isFormComplete(hash: HashMap<String, Boolean>, onDo: (Boolean) -> Unit) {
        if (hash.size == hashSize && !hash.containsValue(false)) {
            onDo.invoke(true)
        } else
            onDo.invoke(false)
    }

    fun isAllFieldValid(onDo: (Boolean) -> Unit) {
        var hash = checksIsRegisterEnable.value
        if (hash != null) {
            if (hash.size == hashSize && !hash.containsValue(false))
                onDo.invoke(true)
            else
                onDo.invoke(false)
        } else
            onDo.invoke(false)
    }

    fun getRegisterRequestData(
        finString: String,
        dateOfIssue: String?,
        postalCodeString: String,
        userName: String
    ): UpdateUserInfoWithPolicyVersion {
        val privacyStatementConsentVal =
            if (PrivacyPolicyValidationModel().isDateFormatCorrect(RemoteConfigUtils.getRemoteConfigPrivacyStatementPublishDate()))
                RemoteConfigUtils.getRemoteConfigPrivacyStatementPublishDate()
            else
                null
        return UpdateUserInfoWithPolicyVersion(
            IdentityType.FIN_STP,
            finString,
            dateOfIssue,
            "",
            Build.MODEL,
            postalCodeString,
            "",
            name = userName,
            consentedPrivacyStatementVersion = privacyStatementConsentVal
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}






