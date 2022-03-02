package sg.gov.tech.bluetrace

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main_new.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.BuildConfig.FIREBASE_REGION
import sg.gov.tech.bluetrace.fragment.BluetoothHistoryPagerFragment
import sg.gov.tech.bluetrace.fragment.UploadFlowControllerFragment
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.AppUpdatedV2Activity
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.OnboardExistingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.permissions.FeatureChecker
import sg.gov.tech.bluetrace.revamp.home.HomeFragmentV3
import sg.gov.tech.bluetrace.revamp.settings.SettingsFragmentV2
import sg.gov.tech.bluetrace.utils.ConnectionStateMonitor
import sg.gov.tech.safeentry.selfcheck.HealthStatusApi
import sg.gov.tech.safeentry.selfcheck.model.HealthStatusApiData

class MainActivity : TranslatableActivity() { //, ConnectivityReceiver.ConnectivityReceiverListener

    private val TAG = "MainActivity"
    private lateinit var mHomeFragment: HomeFragmentV3
    private lateinit var mHistoryFrgament: BluetoothHistoryPagerFragment
    private lateinit var mUploadFragment: UploadFlowControllerFragment
    private lateinit var mSettingsFragment: SettingsFragmentV2
    var healthStatusApiStatus = HealthStatusApi.healthStatusApiStatus
    var isSettings = true
    private val errorHandler: ErrorHandler by inject { parametersOf(this) }
    var navigateToPE = false

    // navigation
    var LAYOUT_MAIN_ID = 0
    private var selected = 0

    private val context = this

    private var displayedFragment: MainActivityFragment? = null

    private var sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Preference.PAUSE_UNTIL -> {
                    val fragment = supportFragmentManager.findFragmentById(R.id.content)
                    if (fragment is HomeFragmentV3)
                        fragment.checkDisplayPauseOverlay()
                }
            }
        }

    companion object {
        const val GO_TO_HISTORY = "GO_TO_HISTORY"
        const val GO_TO_HOME = "GO_TO_HOME"
    }

    private fun checkIfReonboarded() {
        if (!Preference.onBoardedWithIdentity(this)) {
            if (Preference.isOnBoarded(this)) {
                //existing user but not onboarded with identity
                startActivity(Intent(this, OnboardExistingActivity::class.java))
            } else {
                //new user or incomplete user
                if (Preference.getCheckpoint(this) == -1) {
                    startActivity(Intent(this, LoveLetterActivity::class.java))
                } else {
                    startActivity(Intent(this, MainOnboardingActivity::class.java))
                }
            }
        } else {
            if (Preference.getLastAppUpdatedShown(this).equals(0f)) {
                startActivity(Intent(this, AppUpdatedV2Activity::class.java))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)
        registerConnectivityLister()

        checkIfReonboarded()

        LAYOUT_MAIN_ID = R.id.content
        val mOnNavigationItemSelectedListener =
            BottomNavigationView.OnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_home -> {
                        if (selected == R.id.navigation_home && mHomeFragment.isAdded) {
                            mHomeFragment.didProcessBack()
                        } else {
                            mHomeFragment = HomeFragmentV3()
                            openFragment(
                                LAYOUT_MAIN_ID, mHomeFragment
                            )
                        }
                        selected = R.id.navigation_home
                        return@OnNavigationItemSelectedListener true
                    }
                    R.id.navigation_history -> {
                        if (selected == R.id.navigation_history && mHistoryFrgament.isAdded) {
                            mHistoryFrgament.didProcessBack()
                        } else {
                            mHistoryFrgament = BluetoothHistoryPagerFragment()
                            mHistoryFrgament.navigateToPETab = navigateToPE
                            openFragment(
                                LAYOUT_MAIN_ID, mHistoryFrgament
                            )
                            navigateToPE = false
                        }
                        selected = R.id.navigation_history
                        return@OnNavigationItemSelectedListener true
                    }

                    R.id.navigation_upload -> {
                        if (selected == R.id.navigation_upload && mUploadFragment.isAdded) {
                            mUploadFragment.didProcessBack()
                        } else {
                            mUploadFragment = UploadFlowControllerFragment()
                                .also { openFragment(LAYOUT_MAIN_ID, it) }
                        }

                        selected = R.id.navigation_upload
                        return@OnNavigationItemSelectedListener true
                    }
                    /*R.id.navigation_activity -> {
                        if (selected != R.id.navigation_activity) {
                            openFragment(
                                LAYOUT_MAIN_ID, ActivityMapFragment()
                            )
                        }
                        selected = R.id.navigation_activity
                        return@OnNavigationItemSelectedListener true
                    }*/

                    R.id.navigation_setting -> {
                        if (selected == R.id.navigation_setting && mSettingsFragment.isAdded) {
                            mSettingsFragment.didProcessBack()
                        }
                        if (selected != R.id.navigation_setting) {
                            mSettingsFragment = SettingsFragmentV2.newInstance()
                                .also { openFragment(LAYOUT_MAIN_ID, it) }
                        }
                        selected = R.id.navigation_setting
                        return@OnNavigationItemSelectedListener true
                    }
                }
                false
            }

        nav_view.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        /*Added for deep linking*/
        if (intent.data != null && !intent.data?.query.isNullOrBlank()) {
            if (intent.data?.getQueryParameter(getString(R.string.deep_link_query_param_link)) == getString(
                    R.string.deep_link_history
                )
            )
                goToHistory()
            else
                goToHome()
        } else if (intent.getBooleanExtra(GO_TO_HISTORY, false))
            goToHistory()
        else goToHome()

        getFCMToken()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.let {
            /*Added for deep linking*/
            if (intent.data != null && !intent.data?.query.isNullOrBlank()) {
                if (intent.data?.getQueryParameter(getString(R.string.deep_link_query_param_link)) == getString(
                        R.string.deep_link_history
                    )
                )
                    goToHistory()
                else
                    goToHome()
            } else if (it.getBooleanExtra(GO_TO_HISTORY, false))
                goToHistory()
            else if (it.getBooleanExtra(GO_TO_HOME, false))
                goToHome()
        }
    }

    fun handleSelfCheckApi() {
        if (!RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(this))) {
            if (Utils.doesHealthStatusNeedRefresh() || !hasHealthStatusData()) {
                healthStatusApiStatus.postValue(HealthStatusApiData.loading())
                errorHandler.handleSelfCheckNetworkConnection {
                    if (it) callHeathStatusApi()
                    else {
                        if (Utils.doesHealthStatusNeedRefresh() || !hasHealthStatusData())
                            healthStatusApiStatus.postValue(HealthStatusApiData.noNetwork())
                    }
                }
            }
        }
    }

    private fun callHeathStatusApi() {

        if (HealthStatusApi.inProgress) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val decryptedUserData = Preference.getEncryptedUserData(context)
            decryptedUserData?.let { userData ->
                if (!RegisterUserData.isInvalidPassportOrInvalidUser(userData)) {
                    userData.id.let {
                        HealthStatusApi.fetchHealthStatus(
                                it,
                                Preference.getTtID(context),
                                Preference.getPreferredLanguageCode(context),
                                FirebaseFunctions.getInstance(FIREBASE_REGION)
                        )
                    }
                }
            }
        }
    }

    private fun getFCMToken() {

        CentralLog.i(TAG, "Firebase user: ${FirebaseInstanceId.getInstance().id}")
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    CentralLog.w(TAG, "failed to get fcm token ${task.exception}")
                    return@addOnCompleteListener
                } else {
                    // Get new Instance ID token
                    val token = task.result?.token
                    token?.let {
                        Utils.registerFCMToken(
                            it,
                            applicationContext,
                            FirebaseFunctions.getInstance(FIREBASE_REGION)
                        )
                    }
                    CentralLog.d(TAG, "FCM token: $token")
                }
            }
    }

    fun goToHome() {
        nav_view.selectedItemId = R.id.navigation_home
    }

    private fun goToHistory() {
        nav_view.selectedItemId = R.id.navigation_history
    }

    fun goToPossibleExposure() {
        mHomeFragment.isTutorialDialogSeen = false
        navigateToPE = true
        nav_view.selectedItemId = R.id.navigation_history
    }

    override fun onBackPressed() {
        displayedFragment?.didProcessBack()?.let { backProcessed ->
            if (!backProcessed) {
                if (!this::mHomeFragment.isInitialized)
                    mHomeFragment = HomeFragmentV3()
                if (!mHomeFragment.isAdded) {
                    openFragment(LAYOUT_MAIN_ID, mHomeFragment)
                    nav_view.selectedItemId = R.id.navigation_home
                } else
                    super.onBackPressed()
            }
        }
    }

    fun openFragment(
        containerViewId: Int,
        fragment: MainActivityFragment
    ) {
        try { // pop all fragments
            supportFragmentManager.popBackStackImmediate(
                LAYOUT_MAIN_ID,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            val transaction =
                supportFragmentManager.beginTransaction()
            transaction.replace(containerViewId, fragment, fragment.customTag)
            transaction.commit()
            displayedFragment = fragment
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        Preference.purgeUserDetaislData(this)
        checkIfReonboarded()
        Preference.registerListener(this, sharedPreferenceChangeListener)
        Utils.startBluetoothMonitoringService(this)
    }

    override fun onPause() {
        super.onPause()
        Preference.unregisterListener(this, sharedPreferenceChangeListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FeatureChecker.REQUEST_ACCESS_LOCATION,
            FeatureChecker.REQUEST_ENABLE_BLUETOOTH,
            FeatureChecker.REQUEST_IGNORE_BATTERY_OPTIMISER,
            FeatureChecker.REQUEST_APP_SETTINGS -> {
                val fragment = supportFragmentManager.findFragmentById(R.id.content)
                if (fragment is HomeFragmentV3) {
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
        if (fragment is HomeFragmentV3) {
            fragment.featurePermissionCallback(requestCode, permissions, grantResults)
        }
    }

    fun refreshBottomNav() {
        for (i in 0 until nav_view.menu.size()) {
            val mi: MenuItem = nav_view.menu.getItem(i)
            when (mi.itemId) {
                R.id.navigation_home -> {
                    mi.title = getString(R.string.title_home)
                }
                R.id.navigation_history -> {
                    mi.title = getString(R.string.title_history)
                }
                R.id.navigation_upload -> {
                    mi.title = getString(R.string.title_upload)
                }
                R.id.navigation_setting -> {
                    mi.title = getString(R.string.title_more)
                }
            }
        }
    }

    private fun registerConnectivityLister() {
        val connectionStateMonitor = ConnectionStateMonitor(this)
        connectionStateMonitor.observe(this, Observer {
            if (!RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(this))) {
                errorHandler.handleSelfCheckNetworkConnection {
                    when (it) {
                        true -> {
                            if (Utils.doesHealthStatusNeedRefresh() || !hasHealthStatusData()) {
                                healthStatusApiStatus.postValue(HealthStatusApiData.loading())
                                callHeathStatusApi()
                            }
                        }
                        false -> {
                            if (Utils.doesHealthStatusNeedRefresh() || !hasHealthStatusData())
                                healthStatusApiStatus.postValue(HealthStatusApiData.noNetwork())
                        }
                    }
                }
            }
        })
    }

    private fun hasHealthStatusData(): Boolean {
        return healthStatusApiStatus.value?.data != null
    }
}