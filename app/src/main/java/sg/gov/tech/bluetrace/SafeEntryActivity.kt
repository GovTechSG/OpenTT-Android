package sg.gov.tech.bluetrace

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import sg.gov.tech.bluetrace.fragment.SafeEntryFragment
import sg.gov.tech.bluetrace.permissions.FeatureChecker
import sg.gov.tech.bluetrace.qrscanner.QrScannerFragmentV2

class SafeEntryActivity : TranslatableActivity() {

    private lateinit var mLoadingView: View
    private var misBackEnable: Boolean = true
    private var pageNum: Int = -1

    companion object {
        var ACTION_FINISH = 1002
        var REQUEST_ACTION = 3002
        const val QR_FRAGMENT = 0
        const val FAV_FRAGMENT = 1
        const val ID_FRAGMENT = 2
        const val IS_FROM_GROUP_CHECK_IN = "IS_FROM_GROUP_CHECK_IN"
        const val IS_FROM_SHORT_CUT = "IS_FROM_SHORT_CUT"
        const val INTENT_EXTRA_PAGE_NUMBER = "pageNum"
    }

    var isFromGroupCheckIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_entry)

        if (intent.hasExtra(IS_FROM_GROUP_CHECK_IN))
            isFromGroupCheckIn = intent.getBooleanExtra(IS_FROM_GROUP_CHECK_IN, false)

        mLoadingView = findViewById(R.id.view_loading)

        if (intent.hasExtra(INTENT_EXTRA_PAGE_NUMBER))
            pageNum = intent.getIntExtra(INTENT_EXTRA_PAGE_NUMBER, -1)

        if (intent.hasExtra(IS_FROM_SHORT_CUT)) {
            val isFromShortcut = intent.getBooleanExtra(IS_FROM_SHORT_CUT, false)
            if (isFromShortcut) {
                Preference.putshouldShowOptionalUpdateDialog(this, true)
                Preference.putShouldShowPrivacyPolicy(this, true)
            }
        }

        insertSafeEntryFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        isFromGroupCheckIn = false
    }

    fun setLoadingEnable(isLoading: Boolean) {
        setLoading(isLoading)
        misBackEnable = !isLoading
    }

    private fun setLoading(show: Boolean) {
        if (show) {
            mLoadingView.visibility = View.VISIBLE
        } else {
            mLoadingView.visibility = View.INVISIBLE
        }
    }

    private fun insertSafeEntryFragment() {
        val fragmentTransaction: FragmentTransaction =
            supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            R.animator.window_fadein, R.animator.window_fadeout,
            R.animator.window_fadein, R.animator.window_fadeout
        )

        val frag = SafeEntryFragment.newInstance(pageNum)
        fragmentTransaction.replace(R.id.content, frag, "SafeEntryFragment")
        fragmentTransaction.commit()
    }

    internal fun updateScanView(allowQRScanning: Boolean) {
        val fragment = supportFragmentManager.findFragmentByTag("SafeEntryFragment")
        if (fragment is SafeEntryFragment && fragment.mViewPager?.currentItem == 0) {
            val viewPagerAdapter = fragment.mViewPager?.adapter
            if (viewPagerAdapter is SafeEntryFragment.ViewPagerAdapter) {
                val qrFragment = viewPagerAdapter.getItem(0)
                if (qrFragment is QrScannerFragmentV2) {
                    qrFragment.updateScanView(allowQRScanning)
                }
            }
        }
    }

    internal fun checkForAppPaused() {
        val fragment = supportFragmentManager.findFragmentByTag("SafeEntryFragment")
        if (fragment is SafeEntryFragment && fragment.mViewPager?.currentItem == 0) {
            val viewPagerAdapter = fragment.mViewPager?.adapter
            if (viewPagerAdapter is SafeEntryFragment.ViewPagerAdapter) {
                val qrFragment = viewPagerAdapter.getItem(0)
                if (qrFragment is QrScannerFragmentV2) {
                    qrFragment.checkForAppPaused()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FeatureChecker.REQUEST_ACCESS_LOCATION,
            FeatureChecker.REQUEST_ENABLE_BLUETOOTH,
            FeatureChecker.REQUEST_IGNORE_BATTERY_OPTIMISER,
            FeatureChecker.REQUEST_APP_SETTINGS -> {
                val fragment = supportFragmentManager.findFragmentById(R.id.content)
                if (fragment is SafeEntryFragment) {
                    fragment.enablePermission()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val fragment = supportFragmentManager.findFragmentById(R.id.content)
        if (fragment is SafeEntryFragment) {
            fragment.featurePermissionCallback(requestCode, permissions, grantResults)
        }
    }

    override fun onBackPressed() {
        // back
        if (!misBackEnable) {
            return
        } else
            super.onBackPressed()
    }
}
