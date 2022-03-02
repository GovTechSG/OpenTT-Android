package sg.gov.tech.bluetrace.revamp.popUp

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.fragment.*
import sg.gov.tech.bluetrace.fragment.model.*
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.permissions.FeatureChecker
import sg.gov.tech.bluetrace.revamp.settings.PermissionViewModel
import sg.gov.tech.bluetrace.utils.VersionChecker.isAGreaterVersionThan
import sg.gov.tech.bluetrace.utils.VersionChecker.isSameVersionThan

abstract class PopUpFragment : MainActivityFragment("HomeFragmentV3") {
    /**
     * Should pop fragment when it has back stack and return true, otherwise just return false.
     */
    abstract fun didProcessBackFragment(): Boolean

    private val TAG = javaClass.simpleName
    private lateinit var mContext: Context
    internal lateinit var activityFragmentManager: ActivityFragmentManager
    private lateinit var featureChecker: FeatureChecker
    private val bluetoothReceiver = BluetoothStatusReceiver()
    private var unhappyDialogFragment: UnhappyDialogFragment? = null
    private val permissionVM: PermissionViewModel by viewModel()
    private val privacyVM: PrivacyPolicyDialogViewModel by viewModel()
    private var featureCheckerId: String? = null
    internal var isUpdateAppPopUpShown = false
    private var isPauseOverlayPopUpShown = false
    private var isPermissionPopUpShown = false
    internal var isPrivacyPolicyPopUpShown = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        activityFragmentManager = ActivityFragmentManager(requireActivity())
        featureChecker = FeatureChecker(
            (requireActivity() as AppCompatActivity),
            FeatureChecker.REQUEST_ACCESS_LOCATION,
            FeatureChecker.REQUEST_ENABLE_BLUETOOTH,
            FeatureChecker.REQUEST_IGNORE_BATTERY_OPTIMISER
        )
        checkForUpdates()
        checkDisplayPauseOverlay()
        checkFeature()
        checkForPrivacyPolicy()
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        activity?.registerReceiver(bluetoothReceiver, intentFilter)
        checkFeature()
    }

    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(bluetoothReceiver)
    }

    override fun onStop() {
        super.onStop()
        permissionVM.clearFeaturesChecker(featureChecker)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityFragmentManager.dismissAll()
    }

    override fun didProcessBack(): Boolean {
        return didProcessBackFragment()
    }

    private fun checkForUpdates() {
        val remoteConfig = RemoteConfigUtils.setUpRemoteConfig(requireActivity())
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    initUpdateDialog()
                    CentralLog.d(TAG, "Remote config fetch - success: $updated")
                } else {
                    CentralLog.d(TAG, "Remote config fetch - failed")
                }
            }
    }

    private fun initUpdateDialog() {
        val localAppVersion = Utils.getCurrentAppVersionWithoutSuffix(mContext)
        val minimumAppVersion = FirebaseRemoteConfig.getInstance()
            .getString(RemoteConfigUtils.REMOTE_CONFIG_ANDROID_MIN_VERSION)
        val latestAppVersion = FirebaseRemoteConfig.getInstance()
            .getString(RemoteConfigUtils.REMOTE_CONFIG_ANDROID_LATEST_VERSION)

        when {
            minimumAppVersion.isAGreaterVersionThan(localAppVersion) -> {
                showUpdateDialog(true)
            }
            (localAppVersion.isSameVersionThan(minimumAppVersion) || localAppVersion.isAGreaterVersionThan(
                minimumAppVersion
            )) && latestAppVersion.isAGreaterVersionThan(localAppVersion) -> {
                showUpdateDialog(false)
            }
        }
    }

    private fun showUpdateDialog(isForceUpdate: Boolean) {
        if (!isForceUpdate && !Preference.shouldShowOptionalUpdateDialog(mContext))
            return

        val updateDialogFragment = UpdateDialogFragment(isForceUpdate)
        isUpdateAppPopUpShown = true
        updateAllowQRScanning()
        activityFragmentManager.show(updateDialogFragment.getFragmentTag(), updateDialogFragment)
    }

    fun checkDisplayPauseOverlay() {
        if (Preference.shouldBePaused(mContext)) {
            val dialog = PauseDialogFragment()
            val pauseDialogDismissListener = object : PauseDialogDismissListener {
                override fun onDialogDismiss() {
                    isPauseOverlayPopUpShown = false
                    updateAllowQRScanning()
                    activity?.let { activity ->
                        if (activity is SafeEntryActivity) {
                            activity.checkForAppPaused()
                        }
                    }
                }
            }
            dialog.setDialogDismissListener(pauseDialogDismissListener)
            dialog.isCancelable = false
            dialog.activityFragmentManager = activityFragmentManager
            isPauseOverlayPopUpShown = true
            updateAllowQRScanning()
            activityFragmentManager.show("PDF", dialog)
        }
    }

    private fun checkFeature() {
        permissionVM.checkFeatures(featureChecker) {
            if (it)
                dismissUnhappyDialog()
        } ?: return

        permissionVM.checkResult { resultSuccess ->
            if (resultSuccess) {
                dismissUnhappyDialog()
            } else {
                featureCheckerId = permissionVM.getCheckID()
                unhappyDialogFragment = UnhappyDialogFragment()
                unhappyDialogFragment?.let {
                    it.points = getFeaturePoints(permissionVM.getArrayOfChecks())
                    it.buttonListener {
                        enablePermission()
                    }
                    it.isCancelable = false
                    isPermissionPopUpShown = true
                    updateAllowQRScanning()
                    activityFragmentManager.show("unhappy", it)
                }
            }
        }

        activity?.let { activity ->
            if (activity is MainActivity) {
                activity.handleSelfCheckApi()
            }
        }
    }

    private fun dismissUnhappyDialog() {
        isPermissionPopUpShown = false
        updateAllowQRScanning()
        activityFragmentManager.dismiss("unhappy")
        permissionVM.clearFeaturesChecker(featureChecker)
        featureCheckerId = null
    }

    private fun getFeaturePoints(checks: Array<Boolean>): Array<String> {
        val cs = mutableListOf<String>()
        if (!checks[0])
            cs.add(
                UnhappyDialogFragment.LOCATION_PERMISSION(
                    mContext
                )
            )
        if (!checks[1])
            cs.add(
                UnhappyDialogFragment.BLUETOOTH(
                    mContext
                )
            )
        if (!checks[2])
            cs.add(
                UnhappyDialogFragment.IGNORE_BATTERY_OPTIMIZATION(
                    mContext
                )
            )
        return cs.toTypedArray()
    }

    /**
     * Should show privacy policy dialog once per app launch
     * Check if JSON value retrieved from remote config is valid. Popup will not appear if not valid
     * If policy is not accepted (policyVersion and saved policyVersion is different), open dialog
     * If policy is accepted (policyVersion and saved policyVersion is same), check if consent privacy statement api already return success.
     * Execute the api every time until a success is return
     */
    private fun checkForPrivacyPolicy() {
        if (Preference.shouldShowPrivacyPolicy(mContext)) {
            val privacyStatementRemoteConfig =
                FirebaseRemoteConfig.getInstance()
                    .getString(RemoteConfigUtils.REMOTE_CONFIG_PRIVACY_STATEMENT)

            if (privacyStatementRemoteConfig == RemoteConfigUtils.getDefaultValue(
                    mContext,
                    RemoteConfigUtils.REMOTE_CONFIG_ANNOUNCEMENT
                )
                || !PrivacyPolicyValidationModel().isValidJSON(privacyStatementRemoteConfig)
            )
                return

            val privacyStatementModel: PrivacyStatementModel =
                Gson().fromJson(privacyStatementRemoteConfig, PrivacyStatementModel::class.java)
            val policyVersion = privacyStatementModel.policyVersion ?: ""
            if (!privacyVM.isPolicyAccepted(
                    policyVersion,
                    Preference.getPrivacyPolicyPolicyVersion(mContext)
                )
            ) {
                val privacyPolicyDialogFragment = PrivacyPolicyDialogFragment(privacyStatementModel)
                isPrivacyPolicyPopUpShown = true
                updateAllowQRScanning()
                activityFragmentManager.show(
                    privacyPolicyDialogFragment.getFragmentTag(),
                    privacyPolicyDialogFragment
                )
            } else {
                //If policy is accepted. Check if Consent Privacy Statement API is done successfully
                checkIfConsentPrivacyPolicyApiSuccess(
                    ConsentPrivacyStatementRequestModel.getConsentPrivacyStatementRequestData(
                        mContext,
                        policyVersion
                    )
                )
            }
        }
    }

    internal fun checkIfConsentPrivacyPolicyApiSuccess(requestModel: ConsentPrivacyStatementRequestModel) {
        if (Preference.getConsentPrivacyPolicyApiSuccess(mContext) != requestModel.consentedPrivacyStatementVersion) {
            updateBackendPrivacyStatement(requestModel)
        }
    }

    internal fun updateAllowQRScanning() {
        activity?.let { activity ->
            if (activity is SafeEntryActivity) {
                val allowQRScanning =
                    !isUpdateAppPopUpShown && !isPauseOverlayPopUpShown && !isPermissionPopUpShown && !isPrivacyPolicyPopUpShown
                updateScanView(allowQRScanning)
            }
        }
    }

    private fun updateScanView(allowQRScanning: Boolean) {
        activity?.let { activity ->
            if (activity is SafeEntryActivity) {
                activity.updateScanView(allowQRScanning)
            }
        }
    }

    private fun updateBackendPrivacyStatement(consentPrivacyStatementRequestData: ConsentPrivacyStatementRequestModel) {
        if (privacyVM.responseData.hasActiveObservers())
            privacyVM.clearResponseLiveData()

        privacyVM.responseData.observe(this, Observer { response ->
            val result = response.result
            if (response.isSuccess) {
                if ((result is ConsentPrivacyStatementResponseModel) && result.status == "SUCCESS") {
                    CentralLog.d(TAG, "Consent Privacy Statement API: Success")
                    result.consentedPrivacyStatementVersion?.let {
                        Preference.putConsentPrivacyPolicyApiSuccess(mContext, it)
                    }
                }
            } else {
                CentralLog.d(TAG, "Consent Privacy Statement API: Fail")
            }
        })
        privacyVM.updateBackendPrivacyStatement(consentPrivacyStatementRequestData)
    }

    internal fun enablePermission() {
        permissionVM.enableFeatures(featureChecker, featureCheckerId)
    }

    internal fun featurePermissionCallback(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionVM.featurePermissionCallback(
            featureChecker,
            requestCode,
            permissions,
            grantResults
        )
    }

    inner class BluetoothStatusReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action?.equals(BluetoothAdapter.ACTION_STATE_CHANGED) == true)
                update()
        }

        private fun update() {
            if (featureCheckerId != null) {
                permissionVM.simpleCheck(featureChecker) {
                    if (it)
                        dismissUnhappyDialog()
                    else {
                        isPermissionPopUpShown = true
                        updateAllowQRScanning()
                        unhappyDialogFragment?.points =
                            getFeaturePoints(permissionVM.getArrayOfChecks())
                        if (unhappyDialogFragment?.isAdded == true) {
                            unhappyDialogFragment?.updatePoints()
                        }
                    }
                }
            } else
                checkFeature()
        }
    }
}