package sg.gov.tech.bluetrace.qrscanner

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler

class QrScannerModel(val api: ApiHandler) : ViewModel() {

    private var disposables = CompositeDisposable()
    private var TAG = "QRScannerModel"

    var responseData: MutableLiveData<ApiResponseModel<out Any>> =
        MutableLiveData<ApiResponseModel<out Any>>()

    fun validateQrCode(ulr: String) {
        var result = api.validateQrCode(ulr)
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"

        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                CentralLog.e(loggerTAG, "Failed to validate QR: ${e.message}")
                DBLogger.e(
                    DBLogger.LogType.SAFEENTRY,
                    loggerTAG,
                    "Failed to validate QR: ${e.message}",
                    null
                )
                responseData.postValue(ApiResponseModel(false, e.message))
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                responseData.postValue(data)
            }

        })
        disposables.addAll(disposable)

    }

    fun clearResponseLiveData(){
        responseData = MutableLiveData()
    }

}
