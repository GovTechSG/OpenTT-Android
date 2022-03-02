package sg.gov.tech.bluetrace.revamp.requestModel

import android.content.Context
import android.os.Build
import sg.gov.tech.bluetrace.Utils

class OTPRequestModel(
    var mobileNumber: String? = "",
    var appVersion: String? = "",
    var model: String? = "",
    var os: String? = "",
    var osVersion: String? = ""
) {

    companion object {
        fun getOTPRequestData(context: Context, mPhoneNumber: String): OTPRequestModel {
            return OTPRequestModel(
                mobileNumber = mPhoneNumber,
                appVersion = Utils.getAppVersion(context),
                model = Utils.getDeviceName(),
                os = "android",
                osVersion = Build.VERSION.RELEASE
            )
        }
    }
}
