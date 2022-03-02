package sg.gov.tech.bluetrace.fragment.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import java.text.SimpleDateFormat
import java.util.*

class PrivacyPolicyDialogViewModel(val api: ApiHandler) : ViewModel() {
    private val TAG = "PrivacyPolicyDialogViewModel"

    private var disposables = CompositeDisposable()
    var responseData: MutableLiveData<ApiResponseModel<out Any>> =
        MutableLiveData<ApiResponseModel<out Any>>()

    fun isTodayAfterRemindMeDate(remindMeDateInString: String): Boolean{
        if (remindMeDateInString == "")
            return false

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val remindMeDate: Date? = sdf.parse(remindMeDateInString)
        val todayDate: Date? = sdf.parse(sdf.format(Date()))
        return if (remindMeDate != null && todayDate != null) {
            todayDate > remindMeDate
        } else
            false
    }

    fun isPolicyAccepted(policyVersionInString: String, savedPolicyVersionInString: String) : Boolean {
        if (policyVersionInString == "" || savedPolicyVersionInString == "")
            return false

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val policyVersionDate: Date? = sdf.parse(policyVersionInString)
        val savedPolicyVersionDate: Date? = sdf.parse(savedPolicyVersionInString)
        return if (policyVersionDate != null && savedPolicyVersionDate != null) {
            policyVersionDate == savedPolicyVersionDate
        } else
            false
    }

    fun updateBackendPrivacyStatement(requestData: ConsentPrivacyStatementRequestModel) {
        val result = api.updateBackendConsentPrivacyStatement(requestData)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                CentralLog.e(TAG, "Failed to update consent privacy statement: ${e.message}")
                DBLogger.e(DBLogger.LogType.USERDATAREGISTERATION, javaClass.simpleName, "Failed to update consent privacy statement: ${e.message}", null)
                responseData.postValue(ApiResponseModel(false, e.message))
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                responseData.postValue(data as ApiResponseModel<out Any>?)
            }
        })

        disposables.addAll(disposable)
    }

    fun clearResponseLiveData(){
        responseData = MutableLiveData<ApiResponseModel<out Any>>()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}