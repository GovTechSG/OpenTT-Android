package sg.gov.tech.bluetrace.revamp.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import org.koin.android.viewmodel.ext.android.viewModel
import com.google.firebase.crashlytics.internal.common.CommonUtils
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.fragment.JailbrokenDialogFragment
import sg.gov.tech.bluetrace.onboarding.newOnboard.AppUpdatedV2Activity
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.OnboardExistingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.passport.PassportProfileActivity

class SplashActivity : TranslatableActivity(), JailbrokenDialogCallback {

    var needToUpdateApp = false
    val vm: SplashViewModel by viewModel()
    private lateinit var mHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        mHandler = Handler()
        vm.resetCheckPointIfAppIsUpdated(this)
        showDialogIfJailBroken()
        Preference.putShouldShowPrivacyPolicy(this, true)
        Preference.putshouldShowOptionalUpdateDialog(this, true)
        //check if the intent was from notification and its a update notification
        intent.extras?.let {
            val notifyEvent: String? = it.getString("event", null)
            notifyEvent?.let {
                if (it.equals("update")) {
                    needToUpdateApp = true
                    Utils.redirectToPlayStore(this)
                    finish()
                }
            }
            val notifyEventCommand: Int = it.getInt("command", 4)
            notifyEventCommand.let {
                Utils.startBluetoothMonitoringServiceViaIndex(this, notifyEventCommand)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun showDialogIfJailBroken() {
        if (CommonUtils.isRooted(this)) {
            JailbrokenDialogFragment(this).show(supportFragmentManager, "jailbroken")
        }
    }


    override fun onResume() {
        super.onResume()
        if (!needToUpdateApp && !CommonUtils.isRooted(this)) {
            delayNextScreenNavigation()
        }
    }

    fun delayNextScreenNavigation() {
        vm.delayNextScreenNavigation {
            goToNextScreen()
            finish()
        }
    }

    private fun goToNextScreen() {
        when (Preference.onBoardedWithIdentity(this)) {
            true -> registeredAlready()
            false -> notRegisteredYet()

        }
    }

    private fun registeredAlready() {
        when {
            (Preference.getLastAppUpdatedShown(this).equals(0f)) ->
                startActivity(Intent(this, AppUpdatedV2Activity::class.java))
            else ->
                if (RegisterUserData.isInvalidPassportUser(
                        Preference.getUserIdentityType(this)
                    )
                ) {
                    startActivity(Intent(this, PassportProfileActivity::class.java))
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }

        }
    }

    private fun notRegisteredYet() {
        when {
            Preference.isOnBoarded(this) ->
                startActivity(Intent(this, OnboardExistingActivity::class.java))
            (Preference.getCheckpoint(this) == -1) ->
                startActivity(Intent(this, LoveLetterActivity::class.java))
            else ->
                startActivity(Intent(this, MainOnboardingActivity::class.java))
        }
    }

    override fun onJaibrokenDialogClosed() {
        delayNextScreenNavigation()
    }
}
