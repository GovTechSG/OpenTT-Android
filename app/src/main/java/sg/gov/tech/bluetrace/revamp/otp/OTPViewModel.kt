package sg.gov.tech.bluetrace.revamp.otp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.revamp.requestModel.CreateUserRequestModel
import sg.gov.tech.bluetrace.revamp.requestModel.OTPRequestModel

class OTPViewModel(val api: ApiHandler) : ViewModel() {
    private var disposables = CompositeDisposable()
    private val TAG = "OTPViewModel"
    var otpResponseData: MutableLiveData<ApiResponseModel<out Any>> =
        MutableLiveData<ApiResponseModel<out Any>>()
    var createUserResponseData: MutableLiveData<ApiResponseModel<out Any>> =
        MutableLiveData<ApiResponseModel<out Any>>()

    fun getOTP(otpRequestData: OTPRequestModel) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val result = api.getOTP(otpRequestData)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                DBLogger.e(
                    DBLogger.LogType.USERDATAREGISTERATION,
                    loggerTAG,
                    "TT GetOtp API Call Error: ${e.message}",
                    null
                )
                CentralLog.e(loggerTAG, "TT GetOtp API Call Error: ${e.message}")
                otpResponseData.postValue(ApiResponseModel(false, e.message))
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                otpResponseData.postValue(data as ApiResponseModel<out Any>?)
            }

        })

        disposables.addAll(disposable)
    }

    fun createUser(createUserRequestData: CreateUserRequestModel)
    {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val result = api.createUser(createUserRequestData)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                CentralLog.e(loggerTAG, "Failed to create user: ${e.message}")
                DBLogger.e(DBLogger.LogType.USERDATAREGISTERATION, loggerTAG, "Failed to create user: ${e.message}", null)
                createUserResponseData.postValue(ApiResponseModel(false, e.message))
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                createUserResponseData.postValue(data as ApiResponseModel<out Any>?)
            }

        })

        disposables.addAll(disposable)
    }

    fun clearOTPResponseLiveData(){
        otpResponseData = MutableLiveData<ApiResponseModel<out Any>>()
    }

    fun clearCreateUserResponseLiveData(){
        createUserResponseData = MutableLiveData<ApiResponseModel<out Any>>()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}
