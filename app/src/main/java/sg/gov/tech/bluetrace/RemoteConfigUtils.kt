package sg.gov.tech.bluetrace

import android.app.Activity
import android.content.Context

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import sg.gov.tech.bluetrace.fragment.model.PrivacyPolicyValidationModel
import sg.gov.tech.bluetrace.fragment.model.PrivacyStatementModel
import com.google.firebase.functions.BuildConfig
import sg.gov.tech.bluetrace.BuildConfig.*

object RemoteConfigUtils {
    const val REMOTE_CONFIG_ANDROID_LATEST_VERSION = "android_latest_version"
    const val REMOTE_CONFIG_ANDROID_MIN_VERSION = "android_min_version"
    const val REMOTE_CONFIG_SHARE_TEXT = "ShareText"
    const val REMOTE_CONFIG_ANNOUNCEMENT = "Announcement_Android"
    const val REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE = "TogglePossibleExposure"
    const val REMOTE_CONFIG_USE_TT_OTP = "UseTTOTP"
    const val REMOTE_CONFIG_PRIVACY_STATEMENT = "Privacy_Statement_Android"

    var remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    var remoteConfigSetUp = false

    /**
     * set the default values for remote config parameters
     */
    fun getDefaultRemoteConfigValues(context: Context): Map<String, String> {
        return mapOf(
            REMOTE_CONFIG_ANDROID_LATEST_VERSION to BuildConfig.VERSION_NAME.split("-")[0],
            REMOTE_CONFIG_ANDROID_MIN_VERSION to BuildConfig.VERSION_NAME.split("-")[0],
            REMOTE_CONFIG_SHARE_TEXT to context.getString(R.string.share_message),
            REMOTE_CONFIG_ANNOUNCEMENT to  REMOTE_CONFIG_ANNOUNCEMENT_DEFAULT_VALUE,
            REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE to  REMOTE_CONFIG_TOGGLE_PE_DEFAULT_VALUE,
            REMOTE_CONFIG_USE_TT_OTP to REMOTE_CONFIG_USE_TT_OTP_DEFAULT_VALUE,
            REMOTE_CONFIG_PRIVACY_STATEMENT to REMOTE_CONFIG_PRIVACY_STATEMENT_DEFAULT_VALUE
        )
    }

    fun getDefaultValue(context: Context, key: String): String? {
        return getDefaultRemoteConfigValues(context)[key]
    }

    fun setUpRemoteConfig(act: Activity): FirebaseRemoteConfig {
        if (!remoteConfigSetUp) {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.setDefaultsAsync(
                getDefaultRemoteConfigValues(
                    act as Context
                )
            )
            remoteConfigSetUp = true
        }

        return remoteConfig
    }

    fun getRemoteConfigPrivacyStatementPublishDate(): String {
        val privacyStatementRemoteConfig =
            FirebaseRemoteConfig.getInstance()
                .getString(REMOTE_CONFIG_PRIVACY_STATEMENT)

        if (privacyStatementRemoteConfig == getDefaultValue(
                TracerApp.AppContext,
                REMOTE_CONFIG_PRIVACY_STATEMENT
            )
            || !PrivacyPolicyValidationModel().isValidJSON(privacyStatementRemoteConfig)
        )
            return ""

        val gson = Gson()
        return try{
            val privacyStatementModel: PrivacyStatementModel =
                gson.fromJson(privacyStatementRemoteConfig, PrivacyStatementModel::class.java)
            privacyStatementModel.policyVersion ?: ""
        } catch (e: JsonSyntaxException) {
            ""
        }
    }
}