package sg.gov.tech.revamp.requestModel

import android.content.Context
import android.os.Build
import android.provider.Settings
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.Utils

class TempIdRequestModel(
    val deviceId: String,
    val ttId: String,
    val appVersion: String,
    val osVersion: String,
    val btLiteVersion: String,
    val model: String,
    val os: String
) {

    companion object {
        fun getTempIdRequestData(context: Context): TempIdRequestModel {
            return TempIdRequestModel(
                deviceId = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                ),
                ttId = Preference.getTtID(context),
                appVersion = Utils.getAppVersion(context),
                osVersion = Build.VERSION.RELEASE,
                btLiteVersion = "2.0",
                model = Utils.getDeviceName(),
                os = "android"
            )
        }
    }
}
