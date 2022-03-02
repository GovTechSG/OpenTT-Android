package sg.gov.tech.bluetrace.revamp.register

import android.content.Context
import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import sg.gov.tech.bluetrace.RemoteConfigUtils
import sg.gov.tech.bluetrace.api.ApiResponseHandler
import sg.gov.tech.bluetrace.extentions.onApiError
import sg.gov.tech.bluetrace.fragment.model.PrivacyPolicyValidationModel
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.UpdateUserInfoWithPolicyVersion
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.revamp.requestModel.TempIdRequestModel
import sg.gov.tech.revamp.responseModel.PassportStatus
import sg.gov.tech.revamp.responseModel.RegisterModel
import java.util.*

const val UNABLE_TO_REACH_SERVER = "UNABLE_TO_REACH_SERVER"

class ProfileHoldingViewModel(val apiHandler: ApiHandler) : ViewModel() {

    var registrationResponse: MutableLiveData<ApiResponseModel<out Any>> = MutableLiveData()
    private val responseHandler by lazy { ApiResponseHandler() }
    private var disposables = CompositeDisposable()
    private val passportStatus = "passportStatus"
    private val TAG = "ProfileHoldingViewModel"

    fun registerUser(context: Context, userData: RegisterUserData) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val privacyStatementConsentVal = if (PrivacyPolicyValidationModel().isDateFormatCorrect(RemoteConfigUtils.getRemoteConfigPrivacyStatementPublishDate()))
            RemoteConfigUtils.getRemoteConfigPrivacyStatementPublishDate()
        else
            null
        var registeringUserData =
            UpdateUserInfoWithPolicyVersion(
                idTypeEnumUserInfo = IdentityType.PASSPORT,
                id = userData.id,
                idDateOfIssue = userData.idDateOfIssue,
                dateOfBirth = userData.dateOfBirth,
                model = userData.model,
                postalCodeUserInfo = "",
                cardSerialNumber = userData.cardSerialNumber,
                name = userData.name,
                nationality = getCountryCode(userData.nationality) ?: "",
                idDateOfApplication = userData.idDateOfApplication,
                consentedPrivacyStatementVersion = privacyStatementConsentVal
            )

        val result = apiHandler.registerPassportUser(registeringUserData)
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
                registrationResponse.postValue(
                    ApiResponseModel(
                        error.isSuccess,
                        error.message,
                        error.code
                    )
                )
            }

            override fun onSuccess(response: ApiResponseModel<out Any>) {
                response.result?.let { result ->
                    if (result.toString().contains(passportStatus)) {
                        val registerModel = result as RegisterModel
                        when (registerModel.passportStatus) {
                            PassportStatus.MATCH.tag -> {
                                if (!registerModel.correctedPassport.isNullOrBlank()) {
                                    registeringUserData = UpdateUserInfoWithPolicyVersion(
                                        idTypeEnumUserInfo = IdentityType.PASSPORT_VERIFIED,
                                        id = registerModel.correctedPassport
                                            ?: registeringUserData.id,
                                        idDateOfIssue = registeringUserData.idDateOfIssue,
                                        dateOfBirth = registeringUserData.dateOfBirth,
                                        model = registeringUserData.model,
                                        postalCodeUserInfo = "",
                                        cardSerialNumber = registeringUserData.cardSerialNumber,
                                        name = registeringUserData.name,
                                        nationality = registeringUserData.nationality,
                                        idDateOfApplication = registeringUserData.idDateOfApplication,
                                        consentedPrivacyStatementVersion = registeringUserData.consentedPrivacyStatementVersion
                                    )
                                }
                                else
                                    registeringUserData.setPassportToVerified()

                                responseHandler.afterRegistration(
                                    registerModel,
                                    registeringUserData
                                )
                                getTempId(context) {
                                    registrationResponse.value =
                                        ApiResponseModel(true, PassportStatus.MATCH)
                                }
                            }
                            PassportStatus.MATCH_SGR.tag -> {
                                registrationResponse.value =
                                    ApiResponseModel(false, PassportStatus.MATCH_SGR)
                            }
                            else -> {
                                registrationResponse.value =
                                    ApiResponseModel(false, PassportStatus.NO_MATCH)
                            }
                        }
                    } else
                        registrationResponse.value = ApiResponseModel(false, UNABLE_TO_REACH_SERVER)
                } ?: run {
                    registrationResponse.value = ApiResponseModel(false, UNABLE_TO_REACH_SERVER)
                }
            }
        })
        disposables.addAll(disposable)
    }

    private fun getCountryCode(countryName: String): String? {
        return Locale.getISOCountries().find {
            val locale = Locale(Locale.getDefault().language, it)
            (locale.getDisplayCountry(Locale.ENGLISH) == countryName ||
                    locale.getDisplayCountry(locale) == countryName)
        }
    }

    private fun getTempId(context: Context, onComplete: (ApiResponseModel<out Any>) -> Unit) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val tempIdRequestData = TempIdRequestModel.getTempIdRequestData(context)
        val tempIdResult = apiHandler.getTempID(tempIdRequestData)
        val disposable = tempIdResult.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                CentralLog.e(loggerTAG, "getTempId call failed")
                DBLogger.e(DBLogger.LogType.USERDATAREGISTERATION, loggerTAG, "Failed to get tempId: ${e.message}", null)
                onComplete.invoke(ApiResponseModel(false, e.message))
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                onComplete.invoke(data)
            }
        })
        disposables.addAll(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }

    fun clearRegistrationResponseData() {
        registrationResponse = MutableLiveData()
    }
}
