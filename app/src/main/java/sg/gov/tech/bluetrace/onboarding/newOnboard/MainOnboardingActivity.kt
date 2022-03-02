package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.permissions.FeatureChecker
import sg.gov.tech.bluetrace.revamp.onboarding.OnboardingOTPFragmentV2
import sg.gov.tech.bluetrace.revamp.otp.OTPViewModel
import sg.gov.tech.bluetrace.revamp.register.*
import sg.gov.tech.bluetrace.revamp.requestModel.CreateUserRequestModel
import sg.gov.tech.bluetrace.revamp.requestModel.OTPRequestModel
import sg.gov.tech.bluetrace.revamp.responseModel.CreateUserResponseModel
import sg.gov.tech.bluetrace.revamp.responseModel.OTPResponseModel
import sg.gov.tech.bluetrace.revamp.settings.PermissionViewModel
import sg.gov.tech.bluetrace.zendesk.WebViewZendeskSupportFragment
import sg.gov.tech.bluetrace.revamp.register.OnboardWithPassportFragmentV2
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


enum class Fragments(val id: Int, val tag: String, val make: (() -> Fragment)) {
    VERIFY_NUMBER(4, "VERIFY_NUMBER", { OnboardingVerifyNumberFragmentV2() }),
    OTP(5, "OTP", { OnboardingOTPFragmentV2() }),
    SELECT_ID_DOCUMENT(6, "SELECT_ID_DOCUMENT", { OnboardingSelectIdDocument() }),
    REGISTER_USER_NRIC(7, "REGISTER_USER_NRIC", { OnboardingRegisterUserNRICFragmentV2() }),
    REGISTER_USER_WP(8, "REGISTER_USER_WP", { OnboardingRegisterUserWPFragmentV2() }),
    PERMISSION_BLUETOOTH(9, "PERMISSION_BLUETOOTH", { OnBoardingPermissionBluetoothFragment() }),
    COMPLETE(11, "COMPLETE", { OnboardingCompletedFragment() }),
    WEBVIEW(12, "WEBVIEW", { WebViewZendeskSupportFragment() }),
    REGISTER_USER_STP(13, "REGISTER_USER_STP", { OnboardingRegisterUserStpFragmentV2() }),
    REGISTER_USER_LTVP(14, "REGISTER_USER_LTVP", { OnboardingRegisterUserLtvpFragmentV2() }),
    REGISTER_USER_PASSPORT(15, "REGISTER_USER_PASSPORT", { OnboardWithPassportFragmentV2() }),
    REGISTER_USER_DP(16, "REGISTER_USER_DP", { OnboardingRegisterUserDPFragmentV2() }),
    PASSPORT_HOLDING(17, "ACTIVATE_USER_PASSPORT", { ProfileHoldingFragment() });

    override fun toString(): String {
        return tag
    }

    companion object {
        private val types = Fragments.values().associate { it.id to it }
        private val names = Fragments.values().associate { it.toString() to it }

        fun findByValue(value: Int?): Fragments = types[value] ?: VERIFY_NUMBER
        fun findByName(value: String?): Fragments = names[value] ?: VERIFY_NUMBER
    }
}

class MainOnboardingActivity : BaseActivity() {
    private val TAG: String = "MainOnboardingActivity"

    private var credential: PhoneAuthCredential by Delegates.notNull()
    private var verificationId: String by Delegates.notNull()
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var bleSupported = false
    private var speedUp = false
    private var resendingCode = false
    private var sameNumberAgain = false
    private lateinit var mPhoneNumber: String
    private var mRequestId: String = ""
    private lateinit var mPostal: String
    private lateinit var mNric: String
    private var mIsResetup = false
    private var misBackEnable: Boolean = true
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val permissionVM: PermissionViewModel by viewModel()
    private val featureChecker = FeatureChecker(
        this,
        FeatureChecker.REQUEST_ACCESS_LOCATION,
        FeatureChecker.REQUEST_ENABLE_BLUETOOTH,
        FeatureChecker.REQUEST_IGNORE_BATTERY_OPTIMISER
    )
    private var featureCheckerId: String? = null
    private var mHandler: Handler = Handler()

    private val otpVM: OTPViewModel by viewModel()
    private lateinit var errorHandler: ErrorHandler

    var selectedCountryNameCode = "SG"

    /*
    For getBoolean(RemoteConfigUtils.REMOTE_CONFIG_USE_TT_OTP)
    True = use TT backend (Firebase API) with our own OTP service
    False = Use firebase Authentication
     */

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        errorHandler = ErrorHandler(this)

        val fm: FragmentManager = supportFragmentManager
        fm.popBackStackImmediate(0, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        val extras = intent.extras
        if (extras != null) {
            mIsResetup = true
            // check if user has finished re-onboarding.
            if (Preference.onBoardedWithIdentity(this)) {
                CentralLog.d(TAG, "User has been onboarded with identity")
                goToMainActivityNow()
                return
            }

            CentralLog.d(TAG, "User is not onboarded before: ${extras.getInt("page")}")
            var page = extras.getInt("page", Preference.getCheckpoint(this))
            if (page == -1) {
                CentralLog.d(TAG, "User is not onboarded before: going verify number")
                page = Fragments.VERIFY_NUMBER.id
                Preference.putCheckpoint(this, page)
                goToVerifyNumberFragment()
            } else {
                CentralLog.d(TAG, "User is not onboarded before: going to fragment")
                navigateTo(Fragments.findByValue(page))
            }
        } else {
            CentralLog.d(TAG, "Extras is null")
            val checkPoint = Preference.getCheckpoint(this)
            navigateTo(Fragments.findByValue(checkPoint))
        }
//        showIndicator(true)
//        checkActiveFragment(getCurrentDisplayedFragmentEnum())

    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }

    //get the current fragment
    private fun getCurrentDisplayedFragmentEnum(): Fragments? {
        //top of the backStack
        val lastIndex = supportFragmentManager.backStackEntryCount - 1
        if (lastIndex >= 0) {
            val lastFrag = supportFragmentManager.getBackStackEntryAt(lastIndex)
            return Fragments.findByName(lastFrag.name)
        }
        return null
    }

    fun navigateTo(fragment: Fragments) {
//        openFragment(
//            getContainer().id,
//            fragment.fragment,
//            fragment.toString()
//        )
        goToFragment(fragment, fragment.make())
        checkActiveFragment(fragment)
    }

    fun goToFragment(fragment: Fragments, frag: Fragment): Fragment {
        pushFragment(getContainer().id, frag, fragment.tag)
        return frag
    }

    open fun goToVerifyNumberFragment() {
        goToFragment(Fragments.VERIFY_NUMBER, Fragments.VERIFY_NUMBER.make())
        checkActiveFragment(Fragments.VERIFY_NUMBER)
        Preference.putCheckpoint(this, Fragments.VERIFY_NUMBER.id)

    }

    fun goToSelectIdDocumentFragment(otpAutoFilled: Boolean = false) {
        val frag = (Fragments.SELECT_ID_DOCUMENT.make() as OnboardingSelectIdDocument)
        frag.otpAutoFilled = otpAutoFilled
        goToFragment(Fragments.SELECT_ID_DOCUMENT, frag)
        Preference.putCheckpoint(this, Fragments.SELECT_ID_DOCUMENT.id)
        checkActiveFragment(Fragments.SELECT_ID_DOCUMENT)
    }

    fun goToRegisterUserWPFragment() {
        val frag = Fragments.REGISTER_USER_WP.make() as OnboardingRegisterUserWPFragmentV2
        goToFragment(Fragments.REGISTER_USER_WP, frag)
        checkActiveFragment(Fragments.REGISTER_USER_WP)
    }

    fun goToRegisterUserDPFragment() {
        val frag = Fragments.REGISTER_USER_DP.make() as OnboardingRegisterUserDPFragmentV2
        goToFragment(Fragments.REGISTER_USER_DP, frag)
        checkActiveFragment(Fragments.REGISTER_USER_DP)
    }

    open fun goToRegisterUserNRICFragment() {
        val frag = Fragments.REGISTER_USER_NRIC.make() as OnboardingRegisterUserNRICFragmentV2
        goToFragment(Fragments.REGISTER_USER_NRIC, frag)
        checkActiveFragment(Fragments.REGISTER_USER_NRIC)
    }

    fun goToRegisterUserStpFragment() {
        val frag = Fragments.REGISTER_USER_STP.make() as OnboardingRegisterUserStpFragmentV2
        goToFragment(Fragments.REGISTER_USER_STP, frag)
        checkActiveFragment(Fragments.REGISTER_USER_STP)
    }

    fun goToRegisterUserLtvpFragment() {
        val frag = Fragments.REGISTER_USER_LTVP.make() as OnboardingRegisterUserLtvpFragmentV2
        goToFragment(Fragments.REGISTER_USER_LTVP, frag)
        checkActiveFragment(Fragments.REGISTER_USER_LTVP)
    }

    fun goToRegisterUserPassportFragment() {
        val frag = (Fragments.REGISTER_USER_PASSPORT.make() as OnboardWithPassportFragmentV2)
        goToFragment(Fragments.REGISTER_USER_PASSPORT, frag)
        checkActiveFragment(Fragments.REGISTER_USER_PASSPORT)
    }

//    open fun goToDearFragment() {
//        goToFragment(Fragments.DEAR_SG, Fragments.DEAR_SG.make())
//        checkActiveFragment(Fragments.DEAR_SG)
//    }

    open fun goToWebViewFragment(url: String) {
        val frag = (Fragments.WEBVIEW.make() as WebViewZendeskSupportFragment)
        frag.setUrl(url)
        frag.setIsTermPrivacy()
        goToFragment(Fragments.WEBVIEW, frag)
        checkActiveFragment(Fragments.WEBVIEW)
    }


//    open fun goToHowItworkFragment() {
//        goToFragment(Fragments.HOW_IT_WORK, Fragments.HOW_IT_WORK.make())
//        checkActiveFragment(Fragments.HOW_IT_WORK)
//    }

    open fun goToPermissionBluetoothFragment() {
        goToFragment(Fragments.PERMISSION_BLUETOOTH, Fragments.PERMISSION_BLUETOOTH.make())
        checkActiveFragment(Fragments.PERMISSION_BLUETOOTH)
        Preference.putCheckpoint(this, Fragments.PERMISSION_BLUETOOTH.id)
    }

    open fun goToCompleteFragment() {
        goToFragment(Fragments.COMPLETE, Fragments.COMPLETE.make())
        checkActiveFragment(Fragments.COMPLETE)
        Preference.putCheckpoint(this, Fragments.COMPLETE.id)
    }

    fun goToPassportHoldingFragment() {
        goToFragment(Fragments.PASSPORT_HOLDING, Fragments.PASSPORT_HOLDING.make())
        checkActiveFragment(Fragments.PASSPORT_HOLDING)
        Preference.putCheckpoint(this, Fragments.PASSPORT_HOLDING.id)
    }
//    open fun goToConsentFragment() {
//        goToFragment(Fragments.CONSENT)
//        checkActiveFragment(Fragments.CONSENT)
//        Preference.putCheckpoint(this, Fragments.CONSENT.id)
//    }

    open fun goToOtpFragment() {
        val frag = (Fragments.OTP.make() as OnboardingOTPFragmentV2)
        frag.apply {
            arguments = Bundle().apply {
                putString("phone_number", mPhoneNumber)
                putString("country_name_code", selectedCountryNameCode)
            }
        }

        goToFragment(Fragments.OTP, frag)
        checkActiveFragment(Fragments.OTP)
    }

    fun checkActiveFragment(navigation: Fragments?) {

        if (navigation == null) {
            showIndicator(false)
            return
        }

        when (navigation) {
            Fragments.VERIFY_NUMBER -> {
                setProgressBar(5)
                showIndicator(true)
                return
            }
            Fragments.OTP -> {
                setProgressBar(5)
                showIndicator(true)
                return
            }
            Fragments.SELECT_ID_DOCUMENT -> {
                setProgressBar(33)
                showIndicator(true)
                return
            }
            Fragments.REGISTER_USER_WP -> {
                setProgressBar(33)
                showIndicator(true)
                return
            }
            Fragments.REGISTER_USER_DP -> {
                setProgressBar(33)
                showIndicator(true)
                return
            }
            Fragments.REGISTER_USER_NRIC -> {
                setProgressBar(33)
                showIndicator(true)
                return
            }

            Fragments.REGISTER_USER_LTVP -> {
                setProgressBar(33)
                showIndicator(true)
                return
            }

            Fragments.REGISTER_USER_STP -> {
                setProgressBar(33)
                showIndicator(true)
                return
            }

            Fragments.PERMISSION_BLUETOOTH -> {
                setProgressBar(66)
                showIndicator(true)
                return
            }
            Fragments.COMPLETE -> {
                setProgressBar(100)
            }

            else -> {
                showIndicator(false)
            }
        }
    }

    override fun onBackPressed() {
        // back
        if (!misBackEnable) {
            return
        }

        val currentDisplayedFragment = getCurrentDisplayedFragmentEnum()
        if (currentDisplayedFragment == Fragments.SELECT_ID_DOCUMENT
            || currentDisplayedFragment == Fragments.PERMISSION_BLUETOOTH
            || currentDisplayedFragment == Fragments.COMPLETE
            || currentDisplayedFragment == Fragments.OTP
        ) {
            return
        }
        if (currentDisplayedFragment == Fragments.REGISTER_USER_PASSPORT) {
            Preference.clearUserData(this)
            goToSelectIdDocumentFragment()
            return
        }
        if (currentDisplayedFragment == Fragments.PASSPORT_HOLDING) {
            supportFragmentManager.popBackStack()
            goToRegisterUserPassportFragment()
            return
        }
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    fun setLoadingEnable(isLoading: Boolean) {
        setLoading(isLoading)
        misBackEnable = !isLoading
    }

    fun requestForOTP(phoneNumber: String) {
        setLoadingEnable(true)
        resendingCode = false
        sameNumberAgain = this::mPhoneNumber.isInitialized && mPhoneNumber == phoneNumber

        mPhoneNumber = phoneNumber
        val sameNumberAgain = this::mPhoneNumber.isInitialized && mPhoneNumber == phoneNumber

        if (FirebaseRemoteConfig.getInstance()
                .getBoolean(RemoteConfigUtils.REMOTE_CONFIG_USE_TT_OTP)
        ) {
            getOTP(OTPRequestModel.getOTPRequestData(this, phoneNumber))
        } else {
            speedUp = false
            verifyPhoneNumber(phoneNumber, sameNumberAgain)
        }
    }

    private fun getOTP(otpRequestData: OTPRequestModel) {
        setLoadingEnable(true)
        if (otpVM.otpResponseData.hasActiveObservers())
            otpVM.clearOTPResponseLiveData()

        otpVM.otpResponseData.observe(this, Observer { response ->
            val result = response.result
            if (response.isSuccess) {
                if (result is OTPResponseModel) {
                    mRequestId = result.requestId.toString()
                    if (!resendingCode)
                        goToOtpFragment()
                }
            } else {
                if (result is String) {
                    errorHandler.unableToReachServer()
                }

            }
            setLoadingEnable(false)
        })
        otpVM.getOTP(otpRequestData)
    }

    fun createUser(createUserRequestData: CreateUserRequestModel) {
        if (otpVM.createUserResponseData.hasActiveObservers())
            otpVM.clearCreateUserResponseLiveData()

        otpVM.createUserResponseData.observe(this, Observer { response ->
            val result = response.result
            if (response.isSuccess) {
                if ((result is CreateUserResponseModel) && result.status == "SUCCESS")
                    signInWithCustomToken(result.token.toString())
            } else {
                setLoadingEnable(false)
                (supportFragmentManager.findFragmentByTag(Fragments.OTP.tag) as OnboardingOTPFragmentV2).updateOTPError(
                    getString(R.string.invalid_otp)
                )
            }
        })
        otpVM.createUser(createUserRequestData)
    }

    fun signInWithPhone(otp: String) {
        credential = PhoneAuthProvider.getCredential(
            verificationId,
            otp
        )
        setLoadingEnable(true)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithCustomToken(
        customToken: String,
        otpAutoFilled: Boolean = false
    ) {
        customToken.let {
            FirebaseAuth.getInstance().signInWithCustomToken(it)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        CentralLog.d(TAG, "signInWithCustomToken:success")
                        val user = task.result?.user
                        user?.let {
                            TracerApp.setUserIdentity(user.uid, "")
                            it.phoneNumber?.let { phoneNum ->
                                Preference.saveEncryptedPhoneNumber(this, phoneNum)
                            }

                        }

                        setLoadingEnable(false)
                        goToSelectIdDocumentFragment(otpAutoFilled)

                    } else {
                        // Sign in failed, display a message and update the UI
                        CentralLog.d(TAG, "signInWithCustomToken:failure", task.exception)
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            (supportFragmentManager.findFragmentByTag(Fragments.OTP.tag) as OnboardingOTPFragmentV2).updateOTPError(
                                getString(R.string.invalid_otp)
                            )
                        } else if (task.exception is FirebaseAuthInvalidUserException) {
                            alertDialog(getString(R.string.invalid_user))
                        }
                        setLoadingEnable(false)
                    }

                }
        }
    }

    fun resendCode(phoneNumber: String) {
        setLoadingEnable(true)
        resendingCode = true
        if (FirebaseRemoteConfig.getInstance()
                .getBoolean(RemoteConfigUtils.REMOTE_CONFIG_USE_TT_OTP)
        ) {
            getOTP(OTPRequestModel.getOTPRequestData(this, phoneNumber))
        } else {
            speedUp = false
            verifyPhoneNumber(phoneNumber, resendingCode)
        }
    }

    fun getRequestID(): String {
        return mRequestId
    }

    private val phoneNumberVerificationCallbacks =
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(receivedCredential: PhoneAuthCredential) {
                CentralLog.d(TAG, "onVerificationCompleted: $receivedCredential")
                mHandler.removeCallbacksAndMessages(null)
                credential = receivedCredential
                signInWithPhoneAuthCredential(credential, true)
                speedUp = true
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (e is FirebaseAuthInvalidCredentialsException) {
                    CentralLog.d(TAG, "FirebaseAuthInvalidCredentialsException", e)
//                    (Fragments.VERIFY_NUMBER.fragment as OnboardingVerifyNumberFragment).updatePhoneError(
//                        getString(R.string.invalid_number)
//                    )


                    (supportFragmentManager.findFragmentByTag(Fragments.VERIFY_NUMBER.tag) as OnboardingVerifyNumberFragmentV2).updatePhoneError(
                        getString(R.string.invalid_number)
                    )


                } else if (e is FirebaseTooManyRequestsException) {
                    CentralLog.d(TAG, "FirebaseTooManyRequestsException", e)
                    alertDialog(getString(R.string.too_many_requests))
                }
                CentralLog.d(TAG, "On Verification failure: ${e.message}")
                setLoadingEnable(false)
            }

            override fun onCodeSent(
                receivedVerificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                verificationId = receivedVerificationId
                resendToken = token

                CentralLog.d(TAG, "onCodeSent: $receivedVerificationId")
                //val runnableToken = 42
                mHandler.postDelayed({
                    if (!resendingCode) {
                        goToOtpFragment()
                        mHandler.removeCallbacksAndMessages(null)
                    }
                    setLoadingEnable(false)
                }, 5000)
            }
        }

    private fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential,
        otpAutoFilled: Boolean = false
    ) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    CentralLog.d(TAG, "signInWithCredential:success")
                    val user = task.result?.user
                    user?.let {
                        TracerApp.setUserIdentity(user.uid, "")
                        it.phoneNumber?.let { phoneNum ->
                            Preference.saveEncryptedPhoneNumber(this, phoneNum)
                        }

                    }


//                    if (BluetoothMonitoringService.broadcastMessage == null || TempIDManager.needToUpdate(
//                            applicationContext
//                        )
//                    ) {
//                        getTemporaryID()
//                    } else {
//                        setLoadingEnable(false)
//                        goToConsentFragment()
//                    }
                    setLoadingEnable(false)

//                    goToRegisterUserNRICFragment()
                    goToSelectIdDocumentFragment(otpAutoFilled)

                } else {
                    // Sign in failed, display a message and update the UI
                    CentralLog.d(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                        (supportFragmentManager.findFragmentByTag(Fragments.OTP.tag) as OnboardingOTPFragmentV2).updateOTPError(
                            getString(R.string.invalid_otp)
                        )
                    } else if (task.exception is FirebaseAuthInvalidUserException) {
                        alertDialog(getString(R.string.invalid_user))
                    }
                    setLoadingEnable(false)
                }

            }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    fun requestPermissions() {
        permissionVM.clearFeaturesChecker(featureChecker)
        permissionVM.checkFeatures(featureChecker) {
            if (it)
                goToCompleteFragment()
            else
                goToMainActivity()
        } ?: return

        permissionVM.checkResult {
            if (it) {
                goToCompleteFragment()
            } else {
                featureCheckerId = permissionVM.getCheckID()
                permissionVM.enableFeatures(featureChecker, featureCheckerId)
            }
        }

    }

    private fun goToMainActivity() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        CentralLog.d(TAG, "OnButtonClick 2")
        Preference.putCheckpoint(this, 0)
        Preference.putIsOnBoarded(this, true)
        Preference.putOnBoardedWithIdentity(this, true)
        Preference.putLastAppUpdatedShown(this, BuildConfig.LATEST_UPDATE)

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "P1234")
        bundle.putString(
            FirebaseAnalytics.Param.ITEM_NAME,
            "Onboard Completed for Android Device"
        )
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, bundle)

        goToMainActivityNow()
    }

    private fun goToMainActivityNow() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        this.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            FeatureChecker.REQUEST_ACCESS_LOCATION,
            FeatureChecker.REQUEST_ENABLE_BLUETOOTH,
            FeatureChecker.REQUEST_IGNORE_BATTERY_OPTIMISER,
            FeatureChecker.REQUEST_APP_SETTINGS -> {
                permissionVM.enableFeatures(featureChecker, featureCheckerId)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionVM.featurePermissionCallback(
            featureChecker,
            requestCode,
            permissions,
            grantResults
        )
    }

    private fun alertDialog(desc: String?) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(desc)
            .setCancelable(false)
            .setPositiveButton(
                getString(R.string.ok),
                DialogInterface.OnClickListener { dialog, id ->
                    dialog.dismiss()
                })
        val alert = dialogBuilder.create()
        alert.show()
    }

    fun verifyPhoneNumber(phoneNumber: String, resend: Boolean) {
        try {
            var options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)                 // Activity (for callback binding)
                .setCallbacks(phoneNumberVerificationCallbacks)          // OnVerificationStateChangedCallbacks

            resendToken?.let {
                if (resend) {
                    options.setForceResendingToken(it) // ForceResendingToken from callbacks
                }
            }

            PhoneAuthProvider.verifyPhoneNumber(options.build())
        } catch (e: Exception) {
            CentralLog.e(TAG, "Failed to run verifyPhoneNumber: ${e.message}")
            errorHandler.unableToReachServer()
            setLoadingEnable(false)
        }

    }
}
