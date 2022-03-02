package sg.gov.tech.bluetrace.api

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.UpdateUserInfoWithPolicyVersion
import sg.gov.tech.revamp.responseModel.RegisterModel

class ApiResponseHandler {
    private val context by lazy { TracerApp.AppContext }
    private val TAG = "ApiResponseHandler"

    fun afterRegistration(result: RegisterModel, registerUserData: RegisterUserData) {
        val ttId = result.ttId
        Preference.putTtID(context, ttId)
        Preference.putUserRegistrationDate(context)
        Preference.saveEncryptedUserData(context, registerUserData)
        if(registerUserData is UpdateUserInfoWithPolicyVersion){
            val privacyPolicyConsent = registerUserData.consentedPrivacyStatementVersion
            if(privacyPolicyConsent != null){
                Preference.putPrivacyPolicyPolicyVersion(context, privacyPolicyConsent)
                Preference.putConsentPrivacyPolicyApiSuccess(context, privacyPolicyConsent)
                Preference.putShouldShowPrivacyPolicy(context,false)
            }
        }
        CentralLog.d(TAG, "updateUserInfo success: " + result.toString())
        //Save phone number for users who reonboard but did not go through phone number
        var auth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser != null) {
            currentUser.phoneNumber?.let { phoneNum ->
                Preference.saveEncryptedPhoneNumber(context, phoneNum)
            }
        }
        //Update sign up event
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "P1234")
        bundle.putString(
            FirebaseAnalytics.Param.ITEM_NAME,
            "Onboard Completed for Android Device"
        )
        var firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)
    }

}