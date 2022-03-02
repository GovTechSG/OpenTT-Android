package sg.gov.tech.bluetrace.revamp.register

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import sg.gov.tech.bluetrace.RemoteConfigUtils
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.revamp.utils.PhoneNumberValidationModel
import sg.gov.tech.revamp.utils.FieldValidationsV2

class OnboardingVerifyNumberViewModel(val fv: FieldValidationsV2) : ViewModel() {
    private val TAG: String = "OnboardingVerifyNumberViewModel"

    fun validatePhoneNumber(countryNameCode: String, phoneNumber: String, onComplete: (PhoneNumberValidationModel) -> Unit) {
        val result: PhoneNumberValidationModel = fv.isValidPhoneNumber(
            phoneNumber,
            countryNameCode
        )
        onComplete.invoke(result)
    }

    //To update remote config for UseTTOTP
    fun updateRemoteConfig(activity: Activity)
    {
        val remoteConfig: FirebaseRemoteConfig = RemoteConfigUtils.setUpRemoteConfig(activity)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    CentralLog.d(TAG, "Remote config fetch - success: $updated")
                } else {
                    CentralLog.d(TAG, "Remote config fetch - failed")
                }
            }
    }

}






