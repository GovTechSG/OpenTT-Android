package sg.gov.tech.bluetrace.passport

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.revamp.requestModel.GetPassportStatusRequestModel

class PassportStatusViewModel(val api: ApiHandler) : ViewModel() {
    private var disposables = CompositeDisposable()
    private val TAG = "PassportStatusViewModel"
    var getPassportStatusResponseData: MutableLiveData<ApiResponseModel<out Any>> =
        MutableLiveData<ApiResponseModel<out Any>>()

    fun getPassportStatus(getPassportStatusRequestData: GetPassportStatusRequestModel) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val result = api.getPassportStatus(getPassportStatusRequestData)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                DBLogger.e(DBLogger.LogType.PASSPORT_VALIDATION, loggerTAG, "GetPassportStatus API Error: ${e.message}", null)
                CentralLog.e(loggerTAG, "GetPassportStatus API Error: ${e.message}")
                getPassportStatusResponseData.postValue(ApiResponseModel(false, e.message))
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                getPassportStatusResponseData.postValue(data as ApiResponseModel<out Any>?)
            }

        })

        disposables.addAll(disposable)
    }

    fun setPassportUserToVerified(context: Context, correctId: String?) {
        val decryptedUserData = Preference.getEncryptedUserData(context)
        decryptedUserData?.let { userData ->
            userData.setPassportToVerified()
            if (!correctId.isNullOrEmpty())
                userData.id = correctId
            Preference.saveEncryptedUserData(context, userData)
            Preference.userIdType = IdentityType.PASSPORT_VERIFIED.tag
        }
    }

    fun clearGetPassportStatusResponseLiveData(){
        getPassportStatusResponseData = MutableLiveData<ApiResponseModel<out Any>>()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }
}
