package sg.gov.tech.bluetrace.revamp.api

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.gson.Gson
import io.reactivex.Single
import org.json.JSONObject
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.api.ApiResponseHandler
import sg.gov.tech.bluetrace.fragment.model.ConsentPrivacyStatementRequestModel
import sg.gov.tech.bluetrace.fragment.model.ConsentPrivacyStatementResponseModel
import sg.gov.tech.bluetrace.idmanager.TempIDManager
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.UpdateUserInfoWithPolicyVersion
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.revamp.requestModel.*
import sg.gov.tech.bluetrace.revamp.responseModel.*
import sg.gov.tech.revamp.requestModel.TempIdRequestModel
import sg.gov.tech.revamp.responseModel.RegisterModel
import sg.gov.tech.revamp.responseModel.TempIdModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class ApiHandler(val apiRepo: IApiRepository, private val responseHandler: ApiResponseHandler) {
    private val context by lazy { TracerApp.AppContext }
    private val TAG = "ApiHandler"


    companion object {
        val REGISTER = "updateUserInfo"
        private val TEMP_ID = "getTempIDsV3"
        private val GET_OTP = "getOtp"
        private val CREATE_USER = "createUser"
        private val GET_SE_VENUE = "getSEVenue"
        private val FIREBASE_UPLOAD_LOGS_URL = "gs://${BuildConfig.FIREBASE_UPLOAD_LOGS_BUCKET}"
        private val FIREBASE_LOGS_SUBFOLDER_NAME = "androidLogRecords"
        private val GET_PASSPORT_STATUS = "getPassportStatus"
        private val CONSENT_PRIVACY_STATEMENT = "consentPrivacyStatement"
        val CHECK_IN = "postSEEntry"
        val CHECK_OUT = "postSEEntry"
    }

    fun registerUser(
        registerUserData: UpdateUserInfoWithPolicyVersion
    ): Single<out ApiResponseModel<out Any>> {
        val paramString = Gson().toJson(registerUserData, UpdateUserInfoWithPolicyVersion::class.java)
        val data = JSONObject(paramString)
        return apiRepo
            .callSingle(data, REGISTER, RegisterModel::class.java)
            .flatMap { res ->
                res.result?.let { responseHandler.afterRegistration(it, registerUserData) }
                if (res.isSuccess) {
                    val tempIdRequestData = TempIdRequestModel.getTempIdRequestData(context)
                    getTempID(tempIdRequestData)
                } else {
                    Single.just(res)
                }
            }
    }

    fun registerPassportUser(registerUserData: UpdateUserInfoWithPolicyVersion): Single<out ApiResponseModel<out Any>> {
        val paramString = Gson().toJson(registerUserData, UpdateUserInfoWithPolicyVersion::class.java)
        val data = JSONObject(paramString)
        return apiRepo.callSingle(data, REGISTER, RegisterModel::class.java)
            .flatMap { response ->
                Single.just(response)
            }
    }

    fun getOTP(
        otpRequestData: OTPRequestModel
    ): Single<out ApiResponseModel<out Any>> {
        val paramString = Gson().toJson(otpRequestData, OTPRequestModel::class.java)
        val data = JSONObject(paramString)
        return apiRepo.callSingle(data, GET_OTP, OTPResponseModel::class.java)
            .doOnSuccess {
                CentralLog.d(TAG, "Get OTP Api call success")
            }
    }

    fun createUser(
        createUserRequestData: CreateUserRequestModel
    ): Single<out ApiResponseModel<out Any>> {
        val paramString = Gson().toJson(createUserRequestData, CreateUserRequestModel::class.java)
        val data = JSONObject(paramString)
        return apiRepo.callSingle(data, CREATE_USER, CreateUserResponseModel::class.java)
            .doOnSuccess {
                CentralLog.d(TAG, "Create User Api call success")
            }
    }

    fun getTempID(
        tempIdRequestData: TempIdRequestModel, onComplete: () -> Unit = {}
    ): Single<out ApiResponseModel<out Any>> {
        val paramString = Gson().toJson(tempIdRequestData, TempIdRequestModel::class.java)
        val data = JSONObject(paramString)
        return apiRepo.callSingle(data, TEMP_ID, TempIdModel::class.java)
            .doOnSuccess { tempIdRes ->
                TempIDManager.onTempIdResponse(tempIdRes)
                onComplete()
                CentralLog.d(TAG, "getTempId call success")
            }
    }

    fun validateQrCode(url: String): Single<out ApiResponseModel<out Any>> {
        var seVenue = SeVenueModel(url)
        val paramString = Gson().toJson(seVenue, SeVenueModel::class.java)
        val data = JSONObject(paramString)
        return apiRepo
            .callSingleList(data, GET_SE_VENUE, SeVenueList::class.java, 5)
            .doOnSuccess {
                CentralLog.d(TAG, "validateQrCode call success")
            }
            .doOnError {
                CentralLog.e(TAG, "validateQrCode call failed")
            }
    }

    fun getPassportStatus(
        getPassportStatusRequestData: GetPassportStatusRequestModel, onComplete: () -> Unit = {}
    ): Single<out ApiResponseModel<out Any>> {
        val paramString =
            Gson().toJson(getPassportStatusRequestData, GetPassportStatusRequestModel::class.java)
        val data = JSONObject(paramString)
        return apiRepo.callSingle(
            data,
            GET_PASSPORT_STATUS,
            GetPassportStatusResponseModel::class.java
        )
            .doOnSuccess {
                onComplete()
                CentralLog.d(TAG, "getPassportStatus call success")
            }
    }


    fun uploadLogToCloudStorage(context: Context, fileToUpload: File): UploadTask {
        val storage = FirebaseStorage.getInstance(FIREBASE_UPLOAD_LOGS_URL)
        var storageRef = storage.getReferenceFromUrl(FIREBASE_UPLOAD_LOGS_URL)
        val dateString = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date())
        var streetPassRecordsRef =
            storageRef.child("$FIREBASE_LOGS_SUBFOLDER_NAME/$dateString/${fileToUpload.name}")
        val fileUri = Uri.fromFile(fileToUpload)
        return streetPassRecordsRef.putFile(fileUri)
    }

    fun checkInUser(
        checkInRequestModel: CheckInRequestModel
    ): Single<out ApiResponseModel<out Any>> {
        val paramString =
            if (checkInRequestModel is CheckInRequestWithGroupMember) {
                Gson().toJson(checkInRequestModel, CheckInRequestWithGroupMember::class.java)
            } else {
                Gson().toJson(checkInRequestModel, CheckInRequestModel::class.java)
            }
        val data = JSONObject(paramString)
        return apiRepo.callSingle(data, CHECK_IN, CheckInResponseModel::class.java)
            .doOnSuccess {
                CentralLog.d(TAG, "SE CHECK-IN success")
            }
    }

    fun checkOutUser(
        checkOutRequestData: CheckOutRequestModel
    ): Single<out ApiResponseModel<out Any>> {

        val paramString = if(checkOutRequestData is CheckOutRequestModel.CheckOutRequestWithGroupMember)
            Gson().toJson(checkOutRequestData, CheckOutRequestModel.CheckOutRequestWithGroupMember::class.java)
        else
            Gson().toJson(checkOutRequestData, CheckOutRequestModel::class.java)
        val data = JSONObject(paramString)
        return apiRepo.callSingle(data, CHECK_OUT, CheckOutResponseModel::class.java)
            .doOnSuccess {
                CentralLog.d(TAG, "SE CHECK-OUT success")
            }

    }

    fun updateBackendConsentPrivacyStatement(
        consentPrivacyStatementRequestData: ConsentPrivacyStatementRequestModel
    ): Single<out ApiResponseModel<out Any>> {
        val paramString = Gson().toJson(consentPrivacyStatementRequestData, ConsentPrivacyStatementRequestModel::class.java)
        val data = JSONObject(paramString)
        return apiRepo.callSingle(data, CONSENT_PRIVACY_STATEMENT, ConsentPrivacyStatementResponseModel::class.java)
            .doOnSuccess {
                CentralLog.d(TAG, "Consent Privacy Statement Api call success")
            }
    }
}
