package sg.gov.tech.bluetrace.revamp.splash

import android.content.Context
import android.os.Handler
import androidx.lifecycle.ViewModel
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.utils.VersionChecker
import sg.gov.tech.bluetrace.utils.VersionChecker.isSameVersionThan

class SplashViewModel : ViewModel() {
    private val SPLASH_TIME: Long = 500
    private val mHandler: Handler by lazy { Handler() }

    fun resetCheckPointIfAppIsUpdated(context: Context) {
        val localAppVersion =
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.split("-")[0]
        var appVersion = VersionChecker.getCompleteVersionName(localAppVersion)
        if (!appVersion.isSameVersionThan(VersionChecker.getCompleteVersionName(Preference.getAppVersion(context)))) {
            Preference.putCheckpoint(context, -1)
            Preference.putAppVersion(context, localAppVersion)
        }
    }

    fun delayNextScreenNavigation(onDelayed: () -> Unit) {
        mHandler.postDelayed({
            onDelayed.invoke()
        }, SPLASH_TIME)
    }
}
