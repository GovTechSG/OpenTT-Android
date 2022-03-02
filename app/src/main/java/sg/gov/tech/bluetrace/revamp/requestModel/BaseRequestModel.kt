package sg.gov.tech.bluetrace.revamp.requestModel

import android.content.Context
import android.os.Build
import android.provider.Settings
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.Utils

open class BaseRequestModel {
    var deviceId: String
    var ttId: String
    var appVersion: String
    var osVersion: String
    var btLiteVersion: String
    var model: String
    var os: String

    init {
        val context: Context = TracerApp.AppContext
        deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""
        ttId = Preference.getTtID(context)
        appVersion = Utils.getAppVersion(context)
        osVersion = Build.VERSION.RELEASE ?: ""
        btLiteVersion = "2.0"
        model = Utils.getDeviceName()
        os = "android"
    }
}