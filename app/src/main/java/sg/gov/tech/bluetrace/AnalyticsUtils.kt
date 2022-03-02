package sg.gov.tech.bluetrace

import android.app.Activity
import android.os.Bundle
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys

class AnalyticsUtils {
    fun isProductionRelease(): Boolean {
        return BuildConfig.BUILD_TYPE.equals("release", ignoreCase = true)
    }

    fun screenAnalytics(activity: Activity, screenName: String?) {
        if (isProductionRelease()) {
            TracerApp.mFirebaseAnalytics?.setCurrentScreen(activity, screenName, null)
        }
    }

    fun eventAnalytics(screenName: String) {
        if (isProductionRelease()) {
            val logEvent = TracerApp.mFirebaseAnalytics?.logEvent(screenName, null)
        }
    }

    fun eventAnalytics(
        screenName: String,
        bundle: Bundle?
    ) {
        if (isProductionRelease()) {
            TracerApp.mFirebaseAnalytics?.logEvent(screenName, bundle)
        }
    }

    fun trackEvent(screenName: String, eventName: String, value: String) {
        if (isProductionRelease()) {
            val bundle = Bundle()
            bundle.putString(AnalyticsKeys.SCREEN_NAME, screenName)
            bundle.putString(AnalyticsKeys.VALUE, value)
            TracerApp.mFirebaseAnalytics?.logEvent(eventName, bundle)
        }
    }

    fun exceptionEventAnalytics(key: String, tag: String, msg: String){
        var bundle = Bundle()
        bundle.putString(AnalyticsKeys.TAG, tag)
        bundle.putString(AnalyticsKeys.ERROR_MSG, msg)
        TracerApp.mFirebaseAnalytics?.logEvent(key, bundle)
    }

    fun setUserProperty(
        tag: String,
        value: String?
    ) {
        if (isProductionRelease()) {
            TracerApp.mFirebaseAnalytics?.setUserProperty(tag, value)
        }
    }

}
