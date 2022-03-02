package sg.gov.tech.bluetrace.revamp.requestModel

import android.content.Context
import android.os.Build
import sg.gov.tech.bluetrace.Utils

class CreateUserRequestModel(
    var requestId: String = "",
    var mobileNumber: String = "",
    var otp: String = "",
    var appVersion: String = "",
    var model: String = "",
    var os: String = "",
    var osVersion: String = ""
) {

    companion object {
        fun getCreateUserRequestData(context: Context, mRequestId: String, mPhoneNumber: String, mOtp: String): CreateUserRequestModel {
            return CreateUserRequestModel(
                requestId = mRequestId,
                mobileNumber= mPhoneNumber,
                otp = mOtp,
                appVersion = Utils.getAppVersion(context),
                model = Utils.getDeviceName(),
                os = "android",
                osVersion = Build.VERSION.RELEASE
            )
        }
    }
}
