package sg.gov.tech.bluetrace.revamp.onboarding

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import kotlin.math.floor

class OnboardingOTPViewModel : ViewModel() {
    private val TAG = "OnboardingOTPViewModel"

    fun validateOTP(otpValue: String, onComplete: (Boolean) -> Unit) {
        if (TextUtils.isEmpty(otpValue) || otpValue.length < 6)
            onComplete.invoke(false)
        else
            onComplete.invoke(true)
    }

    fun calculateNumOfSecLeft(millisUntilFinished: Long): String{
        val numberOfMins = floor((millisUntilFinished * 1.0) / 60000)
        val numberOfMinsInt = numberOfMins.toInt()
        val numberOfSeconds = floor((millisUntilFinished / 1000.0) % 60)
        val numberOfSecondsInt = numberOfSeconds.toInt()
        return if (numberOfSecondsInt < 10) {
            "0$numberOfSecondsInt"
        } else {
            "$numberOfSecondsInt"
        }
    }

    fun getResendCountDownText(resendText: String, secondsInString: String): String {
        return resendText + " " + secondsInString + "s"
    }
}
