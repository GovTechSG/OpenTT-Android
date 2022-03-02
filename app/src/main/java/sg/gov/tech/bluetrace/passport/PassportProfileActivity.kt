package sg.gov.tech.bluetrace.passport

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.ErrorHandler
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.BaseActivity
import sg.gov.tech.bluetrace.revamp.requestModel.GetPassportStatusRequestModel
import sg.gov.tech.bluetrace.revamp.responseModel.GetPassportStatusResponseModel
import sg.gov.tech.bluetrace.zendesk.WebViewZendeskSupportFragment

enum class Fragments(val id: Int, val tag: String, val make: (() -> Fragment)) {
    PP_PROFILE_BLOCKED(1, "PP_PROFILE_BLOCKED", { PassportProfileBlockedFragment() }),
    PP_PROFILE_NETWORK_ISSUE(2, "PP_PROFILE_NETWORK_ISSUE", { PassportProfileErrorFragment() }),
    PP_PROFILE_SERVER_DOWN(3, "PP_PROFILE_SERVER_DOWN", { PassportProfileErrorFragment() }),
    PP_PROFILE_HELP_WEBVIEW(4, "PP_PROFILE_HELP_WEBVIEW", { WebViewZendeskSupportFragment() });

    override fun toString(): String {
        return tag
    }
}

class PassportProfileActivity : BaseActivity() {
    private val TAG: String = "PassportProfileActivity"

    private lateinit var errorHandler: ErrorHandler
    private val vm: PassportStatusViewModel by viewModel()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        errorHandler = ErrorHandler(this)
        passportCheck()
    }

    fun passportCheck()
    {
        setLoadingEnable(true)
        errorHandler.handleSelfCheckNetworkConnection {
            if (it) {
                checkPassportStatus()
            }
            else {
                goToPPProfileNetworkIssueFragment()
                setLoadingEnable(false)
            }
        }
    }

    private fun checkPassportStatus()
    {
        if (vm.getPassportStatusResponseData.hasActiveObservers())
            vm.clearGetPassportStatusResponseLiveData()

        vm.getPassportStatusResponseData.observe(this, Observer { response ->
            val result = response.result
            if (response.isSuccess) {
                if (result is GetPassportStatusResponseModel)
                {
                    result.message?.let {message ->
                        if (result.status == "SUCCESS" && message) {
                            vm.setPassportUserToVerified(this, result.correctedPassport)
                            goToMainActivityNow()
                        }
                        else {
//                            goToPPProfileBlockedFragment()
                            goToMainActivityNow()
                        }

                    }
                }
            } else {
                if (result is String) {
                    goToPPProfileServerDownFragment()
                }

            }
            setLoadingEnable(false)
        })
        vm.getPassportStatus(GetPassportStatusRequestModel.getPassportStatusRequestData(this))
    }

    private fun isFragmentedDisplayedNow(): Boolean {
        val lastIndex = supportFragmentManager.backStackEntryCount - 1
        if (lastIndex >= 0) {
            return true
        }
        return false
    }


    private fun goToMainActivityNow() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        this.finish()
    }

    private fun goToFragment(fragment: Fragments, frag: Fragment): Fragment {
        pushFragment(getContainer().id, frag, fragment.tag)
        return frag
    }

    private fun goToPPProfileBlockedFragment() {
        supportFragmentManager.popBackStack()
        val frag = Fragments.PP_PROFILE_BLOCKED.make() as PassportProfileBlockedFragment
        goToFragment(Fragments.PP_PROFILE_BLOCKED, frag)
    }

    private fun goToPPProfileNetworkIssueFragment() {
        supportFragmentManager.popBackStack()
        val frag = Fragments.PP_PROFILE_NETWORK_ISSUE.make() as PassportProfileErrorFragment
        goToFragment(Fragments.PP_PROFILE_NETWORK_ISSUE, frag)
        frag.errorType = PassportProfileErrorFragment.NETWORK_ISSUE
    }

    private fun goToPPProfileServerDownFragment() {
        supportFragmentManager.popBackStack()
        val frag = Fragments.PP_PROFILE_SERVER_DOWN.make() as PassportProfileErrorFragment
        goToFragment(Fragments.PP_PROFILE_SERVER_DOWN, frag)
        frag.errorType = PassportProfileErrorFragment.SERVER_DOWN
    }

    fun goToWebViewFragment(url: String) {
        val frag = (Fragments.PP_PROFILE_HELP_WEBVIEW.make() as WebViewZendeskSupportFragment)
        frag.setUrl(url)
        frag.setIsTermPrivacy()
        goToFragment(Fragments.PP_PROFILE_HELP_WEBVIEW, frag)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    fun setLoadingEnable(isLoading: Boolean) {
        if (!isFragmentedDisplayedNow()) {
            if (isLoading)
                setLoadingBackgroundWhite()
            else
                setLoadingBackgroundBlack()
        }

        setLoading(isLoading)
    }
}
