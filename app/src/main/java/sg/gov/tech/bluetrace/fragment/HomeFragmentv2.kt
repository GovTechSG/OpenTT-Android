package sg.gov.tech.bluetrace.fragment

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.github.amlcurran.showcaseview.ShowcaseView
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_home_ttv2.*
import kotlinx.android.synthetic.main.non_passport_user_layout.*
import kotlinx.android.synthetic.main.passport_user_layout.*
import kotlinx.coroutines.*
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.Utils.withComma
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.debugger.PeekActivity
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.groupCheckIn.safeEntry.GroupSafeEntryActivity
import sg.gov.tech.bluetrace.home.PassportUserOverlayDialogFragment
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.revamp.safeentry.SafeEntryCheckInOutActivityV2
import sg.gov.tech.bluetrace.status.persistence.StatusRecord
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryDao
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import sg.gov.tech.bluetrace.utils.VersionChecker.isVersionGreaterOrEqual
import sg.gov.tech.bluetrace.zendesk.WebViewZendeskSupportFragment
import sg.gov.tech.safeentry.selfcheck.model.ConnectionState
import java.util.*
import java.util.concurrent.TimeUnit

private const val REQUEST_ENABLE_BT = 123
private const val PERMISSION_REQUEST_ACCESS_LOCATION = 456

//interval for fetching  Bluetooth exchanges and devices nearby in minutes
private const val REFRESH_INTERVAL_FOR_EXCHANGES = 5

//range interval for devices nearby range
private const val DEVICES_NEARBY_RANGE_INTERVAL = 5

//get the nearby devices since past time (in minutes)
private const val DEVICES_NEARBY_SINCE_PAST_TIME = 5

//interval to switch the display of Bluetooth exchanges and device nearby in Millisecond
private const val BT_DISPLAY_REFRESH_INTERVAL: Long = 7000L

class HomeFragmentv2 : MainActivityFragment("HomeFragmentv2"),
    SafeEntryOnboardDialogFragment.Callback {
    private val TAG = "HomeFragmentv2"
    private lateinit var seDao: SafeEntryDao

    lateinit var mContext: Context
    private var counter = 0

    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var lastKnownRecord: LiveData<StreetPassRecord?>
    private lateinit var lastKnownScanningStarted: LiveData<StatusRecord?>
    private lateinit var bannerContainer: ConstraintLayout
    var isTutorialDialogSeen = true
    lateinit var seRecords: List<SafeEntryRecord>
    private lateinit var mShowCaseView: ShowcaseView
    private lateinit var btExchangesJob: Job
    private var btExchangeCount = 0
    private var btExchangeDevices = 0

    private lateinit var tvBtText: AppCompatTextView
    private lateinit var tvBtTimeLapsed: AppCompatTextView
    private lateinit var ivBtInfo: AppCompatImageView

    // For displaying of Bluetooth Exchange & Device Nearby
    var btDisplayTimerHandler: Handler = Handler(Looper.getMainLooper())
    var updateBtTextTask: Runnable = object : Runnable {
        override fun run() {
            timeLapsed += BT_DISPLAY_REFRESH_INTERVAL
            if (currentViewIsDeviceNearby) {
                btViewForTotalExchanges()
            }
            else {
                setUpTimeLapsed(timeLapsed)
                btViewForDevicesNearby()
            }
            btDisplayTimerHandler.postDelayed(this, BT_DISPLAY_REFRESH_INTERVAL)
        }
    }
    var btDevicesNearbyText: String = ""
    var btTotalExchangesText: String = ""
    var currentViewIsDeviceNearby: Boolean = false
    var timeLapsed: Long = -BT_DISPLAY_REFRESH_INTERVAL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViews(view)

        if (!RegisterUserData.isInvalidPassportOrInvalidUser(
                Preference.getUserIdentityType(
                    TracerApp.AppContext
                )
            )
        ) {
            observeSeApi()
        }
        getBTExchanges()
        seDao = StreetPassRecordDatabase.getDatabase(view.context).safeEntryDao()
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_HOME_PAGE)
        mContext = view.context
        bannerContainer = view.findViewById(R.id.banner_container)

        if (RegisterUserData.isInvalidPassportOrInvalidUser(
                Preference.getUserIdentityType(view.context)
            )
        ) {
            non_passport_user_layout.visibility = View.GONE
            passport_user_layout.visibility = View.VISIBLE

            if (RegisterUserData.isInvalidPassportUser(
                    Preference.getUserIdentityType(view.context)
                )
            ) {
                cl_calendar.visibility = View.VISIBLE
                message_text_view.text = HtmlCompat.fromHtml(
                    getString(R.string.passport_user_message),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

            } else if (RegisterUserData.isInvalidUser(Preference.getUserIdentityType(view.context))) {
                message_text_view.text = getString(R.string.invalid_user_message)
                check_eligibility_text_view.visibility = View.GONE
                left_empty_view.visibility = View.VISIBLE
                right_empty_view.visibility = View.VISIBLE
            }
        } else if (RegisterUserData.isValidPassportUser(Preference.getUserIdentityType(view.context))
        ) {
            cl_calendar.visibility = View.VISIBLE
        }

        ib_qr_code.setOnClickListener {
            startActivity(Intent(activity, SafeEntryActivity::class.java))
        }

        ib_fav.setOnClickListener {
            val intent = Intent(activity, SafeEntryActivity::class.java)
            intent.putExtra(
                SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,
                SafeEntryActivity.FAV_FRAGMENT
            )
            startActivity(intent)
        }

        ib_group.setOnClickListener {
            //When clicked on the Group button Icon
            navigateToGroupSafeEntry()
        }

        tv_view_pass.setOnClickListener {
            val intent = Intent(activity, SafeEntryCheckInOutActivityV2::class.java)
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_FRAGMENT_VALUE,
                SafeEntryCheckInOutActivityV2.SE_VIEW_PASS_VALUE
            )
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_VENUE,
                convertQrResultToSeEntryRecord(seRecords[0])
            )
            startActivity(intent)
        }

        val db = StreetPassRecordDatabase.getDatabase(view.context)
        lastKnownRecord = db.recordDao().getMostRecentRecord()
        lastKnownRecord.observe(viewLifecycleOwner,
            Observer { records ->

                if (records != null) {
                    //prevent animation from potentially going haywire
//                    if (!animation_view_cheering.isAnimating) {
//                        animation_view_cheering.playAnimation()
//                    }
                }
            })

        lastKnownScanningStarted = db.statusDao().getMostRecentRecord("Scanning Started")
        lastKnownScanningStarted.observe(viewLifecycleOwner,
            Observer { record ->
                if (record != null) {
//                    tv_last_update.visibility = View.GONE
//                    tv_last_update.text =
//                        getString(R.string.last_updated, Utils.getTime(record.timestamp))
                }
            })

        btnPauseContactTracing.setOnClickListener {
            pauseTraceTogether()
        }

        cl_share_app.setOnClickListener {
            shareThisApp()
        }

        b_check_out.setOnClickListener {
            val intent = Intent(activity, SafeEntryCheckInOutActivityV2::class.java)
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_FRAGMENT_VALUE,
                SafeEntryCheckInOutActivityV2.SE_CHECK_OUT_VALUE
            )
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_VENUE,
                convertQrResultToSeEntryRecord(seRecords[0])
            )
            startActivity(intent)
        }

        check_eligibility_text_view.setOnClickListener {
            val fragment = WebViewZendeskSupportFragment()
            fragment.setUrl(BuildConfig.ZENDESK_WHY_CANT_I_USE_URL)
            childFragmentManager.beginTransaction()
                .addToBackStack(fragment.customTag)
                .replace(R.id.f_child_content, fragment)
                .commit()
        }

        re_register_button.setOnClickListener {
            showReRegistrationDialog()
        }

        cl_calendar.setOnClickListener {
            openPassportUserOverlayScreen()
        }

        getUnexitedEntry()
    }

    private fun setViews(view: View) {
        tvBtText = view.findViewById(R.id.tv_bt_text)
        tvBtTimeLapsed = view.findViewById(R.id.tv_bt_time_lapsed)
        ivBtInfo = view.findViewById(R.id.iv_bt_info)

        /*
        TODO Use findviewbyid instead for all other views
         */
    }


    private fun getUnexitedEntry() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR, -24)
        val twentyFourHoursAgo = cal.timeInMillis
        seDao.getUnexitedEntryRecords(twentyFourHoursAgo)
            .observe(viewLifecycleOwner, Observer<List<SafeEntryRecord>> { records ->
                seRecords = records
                if (seRecords.isNotEmpty()) {
                    hideShowSafeEntryCheckOutSection(true)
                    tv_last_venue.text =
                        if (seRecords[0].tenantName.isEmpty()) seRecords[0].venueName else seRecords[0].tenantName
                } else {
                    hideShowSafeEntryCheckOutSection(false)
                }
            })
    }

    /**
     * Get BTExchanges count every 60 seconds
     */
    private fun getBTExchanges() {

        btExchangesJob = CoroutineScope(Dispatchers.Main).launch {
            val db = StreetPassRecordDatabase.getDatabase(activity as Context)
            val recordDao = db.recordDao()
            while (true) {
                val now = System.currentTimeMillis()
                btExchangeCount = withContext(Dispatchers.IO) {
                    recordDao.liveCountRecordsInRange(
                        DateTools.getStartOfDay(now).timeInMillis,
                        DateTools.getEndOfDay(now).timeInMillis
                    )
                }
                btExchangeDevices = withContext(Dispatchers.IO) {
                    recordDao.countUniqueBTnBTLTempId(
                        DateTools.getTimeMinutesAgo(DEVICES_NEARBY_SINCE_PAST_TIME),
                        Calendar.getInstance().timeInMillis
                    )
                }
                updateBtCount()
                val timeInMillis =
                    TimeUnit.MINUTES.toMillis(REFRESH_INTERVAL_FOR_EXCHANGES.toLong())
                setUpBtTimer()
                delay(timeInMillis)
            }
        }

    }

    fun showShimmer() {
        unhappy_box.visibility = View.GONE
        if (FirebaseRemoteConfig.getInstance()
                .getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE)
        )
            shimmer_view_container.visibility = View.VISIBLE
        else
            shimmer_view_container?.visibility = View.GONE
        possible_exposure_box?.visibility = View.GONE
    }

    fun hideCloseContact() {
        shimmer_view_container?.visibility = View.GONE
        possible_exposure_box?.visibility = View.GONE
        unhappy_box?.visibility = View.GONE
    }

    fun showUnHappyBox(isServerDown: Boolean) {
        if (isServerDown)
            unhappy_title.text = getString(R.string.generic_unavailable)
        else
            unhappy_title.text = getString(R.string.network_issue_text)
        possible_exposure_box.visibility = View.GONE
        shimmer_view_container.visibility = View.GONE
        if (FirebaseRemoteConfig.getInstance()
                .getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE)
        )
            unhappy_box.visibility = View.VISIBLE
        else
            unhappy_box.visibility = View.GONE
    }

    fun showPossibleExposed(isExposed: Boolean) {
        unhappy_box.visibility = View.GONE
        shimmer_view_container.visibility = View.GONE
        if (FirebaseRemoteConfig.getInstance()
                .getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE)
        )
            possible_exposure_box.visibility = View.VISIBLE
        else
            possible_exposure_box.visibility = View.GONE
        updateCloseContactView(isExposed)
    }

    fun updateCloseContactView(isExposed: Boolean) {
        if (isExposed) {
            tv_cc_title.text = getString(R.string.possible_ex)
            tv_cc_dis.text = getImageText(getString(R.string.you_were_at))
            iv_cc_icon.background = ContextCompat.getDrawable(mContext, R.drawable.ic_exla)
            cl_close_contact.setBackgroundColor(ContextCompat.getColor(mContext, R.color.pink))
        } else {
            tv_cc_title.text = getString(R.string.you_re_okay)
            tv_cc_dis.text = getImageText(getString(R.string.based_on_al))
            iv_cc_icon.background = ContextCompat.getDrawable(mContext, R.drawable.ic_ok)
            cl_close_contact.setBackgroundColor(
                ContextCompat.getColor(
                    mContext,
                    R.color.close_box_bg
                )
            )
        }
        tv_cc_dis.makeLinks(
            Pair(getString(R.string.see_details), View.OnClickListener {
                (activity as MainActivity?)?.goToPossibleExposure()
            })
        )
    }

    private fun observeSeApi() {
        (activity as MainActivity).healthStatusApiStatus.observe(viewLifecycleOwner,
            Observer { state ->
                when (state.state) {
                    ConnectionState.Loading -> {
                        showShimmer()
                    }
                    ConnectionState.Done -> {
                        var response = state.data?.selfCheck
                        if (response == null) showUnHappyBox(true)
                        else showPossibleExposed(response.count > 0)
                    }
                    ConnectionState.Error -> {
                        showUnHappyBox(true)
                    }
                    ConnectionState.NoNetwork -> {
                        showUnHappyBox(false)
                    }
                    else -> {

                    }
                }
            })
    }

    private fun convertQrResultToSeEntryRecord(seRecord: SafeEntryRecord): QrResultDataModel {
        return QrResultDataModel(
            seRecord.venueName,
            seRecord.venueId,
            seRecord.tenantName,
            seRecord.tenantId,
            seRecord.postalCode,
            seRecord.address,
            seRecord.id,
            seRecord.checkInTimeMS,
            seRecord.groupMembersCount,
            seRecord.groupMembers
        )
    }

    private fun hideShowSafeEntryCheckOutSection(show: Boolean) {
        if (show)
            cl_last_check_in.visibility = View.VISIBLE
        else
            cl_last_check_in.visibility = View.GONE
    }

    fun pauseTraceTogether() {
        AnalyticsUtils().screenAnalytics(
            activity as Activity,
            AnalyticsKeys.SCREEN_NAME_HOME_TT_PAUSED
        )
        val radioBtnView = layoutInflater.inflate(R.layout.dialog_pause_tt, null)
        val radioGroup = radioBtnView.findViewById<RadioGroup>(R.id.pause_radio_group)

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.pause_tt_title)
            .setCancelable(true)
            .setView(radioBtnView)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(R.string.ok) { dialog, _ ->

                var timeToPauseFor: Long = 0

                when (radioGroup.checkedRadioButtonId) {
                    R.id.pause_30m -> {
                        CentralLog.i(TAG, "pause for 30m")
//                        timeToPauseFor = 10 * 1000
                        timeToPauseFor = 30 * 60 * 1000
                    }
                    R.id.pause_2h -> {
                        CentralLog.i(TAG, "pause for 2h")
                        timeToPauseFor = 2 * 60 * 60 * 1000
                    }
                    R.id.pause_8h -> {
                        CentralLog.i(TAG, "pause for 8h")
                        timeToPauseFor = 8 * 60 * 60 * 1000
                    }
                    else -> {
                        CentralLog.i(TAG, "pause for XXX - invalid option")
                    }
                }

                val pauseUntil = System.currentTimeMillis() + timeToPauseFor
                //pause the service
//                Preference.putPauseUntil(TracerApp.AppContext, pauseUntil)
                Utils.pauseBluetoothMonitoringService(TracerApp.AppContext, pauseUntil)
            }

        val dialog = builder.create()
        dialog.show()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_ttv2, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        updateBtCount()

        shimmer_view_container.startShimmerAnimation()
        animation_view_cheering.setOnClickListener {
            if (BuildConfig.DEBUG && ++counter == 2) {
                counter = 0
                val intent = Intent(context, PeekActivity::class.java)
                context?.startActivity(intent)
            }
        }

        remoteConfig = RemoteConfigUtils.setUpRemoteConfig(activity as Activity)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(activity as Activity) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    if (isTutorialDialogSeen && isAdded)
                        checkForAnnouncement()
                    checkPossibleExposureDisplay()
                    CentralLog.d(TAG, "Remote config fetch - success: $updated")
                } else {
                    CentralLog.d(TAG, "Remote config fetch - failed")
                }
            }
    }

    /**
     * checks whether to display the self check box or not
     * depending upon remote config toggle
     */
    private fun checkPossibleExposureDisplay() {
        if (RegisterUserData.isInvalidPassportOrInvalidUser(
                Preference.getUserIdentityType(mContext)
            ) ||
            !FirebaseRemoteConfig.getInstance()
                .getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE)
        ) {
            hideCloseContact()
        }
    }

    private fun checkForAnnouncement() {
        announcement_close.setOnClickListener {
            Preference.setAnnouncementSeen(activity as Context, true)
            showHideAnnouncement(false)
        }
        val announcement =
            FirebaseRemoteConfig.getInstance()
                .getString(RemoteConfigUtils.REMOTE_CONFIG_ANNOUNCEMENT)

        if (announcement == RemoteConfigUtils.getDefaultValue(
                mContext,
                RemoteConfigUtils.REMOTE_CONFIG_ANNOUNCEMENT
            )
        )
            return

        val gson = Gson()
        val announcementModel: AnnouncementModel =
            gson.fromJson(announcement, AnnouncementModel::class.java)
        val annc_text = announcementModel.getAnnouncementMsg()
        announcement_cl.setOnClickListener {
            if (!announcementModel.url.isNullOrBlank()) {
                openWebView(announcementModel.url)
            }
        }
        announcement_title.text = annc_text
        val versionComparisonCheck = displayAnnouncementAppVersionCheck(
            announcementModel.minAppVersion,
            announcementModel.maxAppVersion
        )
        if (!annc_text.isNullOrBlank() && versionComparisonCheck) {
            when {
                (announcementModel.id > Preference.getAnnouncementVersion(activity as Context)) -> {
                    Preference.putAnnouncementVersion(
                        activity as Context,
                        announcementModel.id
                    )
                    Preference.setAnnouncementSeen(activity as Context, false)
                    showHideAnnouncement(true)
                    return
                }
                (!Preference.getAnnouncementSeen(activity as Context)) -> {
                    showHideAnnouncement(true)
                    return
                }
                else ->
                    showHideAnnouncement(false)
            }
        } else
            showHideAnnouncement(false)
    }

    /**
     * checks whether to display the announcements for current app version or not
     */
    private fun displayAnnouncementAppVersionCheck(
        minAppVersion: String?,
        maxAppVersion: String?
    ): Boolean {
        var currentAppVersion =
            activity?.let { Utils.getCurrentAppVersionWithoutSuffix(it) } ?: return false
        return if (!minAppVersion.isNullOrBlank() && !maxAppVersion.isNullOrBlank()) {
            //checks the condition (minAppVersion <= currentAppVersion <=maxAppVersion)
            (currentAppVersion.isVersionGreaterOrEqual(minAppVersion)
                    && maxAppVersion.isVersionGreaterOrEqual(currentAppVersion))
        } else if (!minAppVersion.isNullOrBlank() && maxAppVersion.isNullOrBlank()) {
            //checks the condition minAppVersion <= currentAppVersion while maxAppVersion is not mentioned
            currentAppVersion.isVersionGreaterOrEqual(minAppVersion)
        } else if (minAppVersion.isNullOrBlank() && !maxAppVersion.isNullOrBlank()) {
            //checks the condition maxAppVersion >= currentAppVersion while minAppVersion is not mentioned
            maxAppVersion.isVersionGreaterOrEqual(currentAppVersion)
        } else {
            false
        }
    }

    private fun showHideAnnouncement(isShow: Boolean) {
        announcement_cl.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        checkPossibleExposureDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lastKnownRecord.removeObservers(viewLifecycleOwner)
        lastKnownScanningStarted.removeObservers(viewLifecycleOwner)
        btExchangesJob.cancel()
        btDisplayTimerHandler.removeCallbacks(updateBtTextTask)
    }

    private fun shareThisApp() {
        val newIntent = Intent(Intent.ACTION_SEND)
        newIntent.type = "text/plain"
        newIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        val shareMessage =
            remoteConfig.getString(RemoteConfigUtils.REMOTE_CONFIG_SHARE_TEXT)
        newIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(newIntent, "choose one"))
    }

    private fun openPassportUserOverlayScreen() {
        PassportUserOverlayDialogFragment().show(childFragmentManager, "HomeFragmentv2")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            (activity as MainActivity).getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private fun enableBluetooth() {
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        bluetoothAdapter?.let {
            if (it.isDisabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        CentralLog.d(TAG, "[onRequestPermissionsResult]requestCode $requestCode")
        when (requestCode) {
            PERMISSION_REQUEST_ACCESS_LOCATION -> {
            }
        }
    }

    override fun didProcessBack(): Boolean {
        if (isAdded) {
            return if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStackImmediate()
                true
            } else false
        }
        return false
    }

    private fun updateBtCount() {
        var connectedDevicesRange = getConnectedDeviceRange(btExchangeDevices)
        btDevicesNearbyText = getString(
            R.string.trace_together_devices_nearby,
            connectedDevicesRange
        )

        btTotalExchangesText = getString(
            R.string.total_exchanges_today,
            withComma(btExchangeCount)
        )

        ivBtInfo.setOnClickListener {
            val fragment = WebViewZendeskSupportFragment()
            fragment.setUrl(BuildConfig.ZENDESK_HOME_ALONE_URL)

            childFragmentManager.beginTransaction()
                .addToBackStack(fragment.customTag)
                .replace(R.id.f_child_content, fragment)
                .commit()
        }
    }

    private fun btViewForDevicesNearby() {
        currentViewIsDeviceNearby = true
        tvBtText.text = btDevicesNearbyText
        tvBtTimeLapsed.visibility = View.VISIBLE
    }

    private fun btViewForTotalExchanges() {
        currentViewIsDeviceNearby = false
        tvBtText.text = btTotalExchangesText
        tvBtTimeLapsed.visibility = View.GONE
    }

    /*
    <1 min = ""
    1 min = "1 min ago"
    >1 min = "X mins ago"
     */
    private fun setUpTimeLapsed(timeInMillis: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        var timeLapsed = ""
        if (minutes == 1L)
            timeLapsed = "$minutes min ago"
        else if (minutes > 1L)
            timeLapsed = "$minutes mins ago"

        tvBtTimeLapsed.text = timeLapsed
    }

    private fun setUpBtTimer() {
        //Reset Time and boolean value
        timeLapsed = -BT_DISPLAY_REFRESH_INTERVAL
        currentViewIsDeviceNearby = false

        btDisplayTimerHandler.removeCallbacks(updateBtTextTask)
        btDisplayTimerHandler.post(updateBtTextTask)
    }

    /**
     * get the range for the connected devices
     */
    fun getConnectedDeviceRange(btDevicesNearby: Int): String {
        when (btDevicesNearby) {
            0 -> {
                return "0"
            }
            else -> {
                val upperRangeVal: Int
                val bottomRangeVal: Int

                var n = btDevicesNearby / DEVICES_NEARBY_RANGE_INTERVAL
                //Handle situations where btDevicesNearby is a multiple of 5
                if (btDevicesNearby % DEVICES_NEARBY_RANGE_INTERVAL == 0)
                    n--

                upperRangeVal = (n * DEVICES_NEARBY_RANGE_INTERVAL) + 1
                bottomRangeVal = (n + 1) * DEVICES_NEARBY_RANGE_INTERVAL

                return "$upperRangeVal-$bottomRangeVal"
            }
        }
    }

    override fun onDismissed() {
        mShowCaseView.hide()
        isTutorialDialogSeen = true
        checkForAnnouncement()

    }

    private fun openWebView(url: String) {
        val fragment = WebViewZendeskSupportFragment()
        fragment.setFabInvisible()
        fragment.setUrl(url)
        childFragmentManager.beginTransaction()
            .addToBackStack(fragment.customTag)
            .replace(R.id.f_child_content, fragment)
            .commit()
    }

    fun getImageText(str: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        builder.append(
            HtmlCompat.fromHtml(
                str,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        )
            .append(" ", activity?.let { ImageSpan(it, R.drawable.keyboard_backspace) }, 0)

        return builder
    }

    private fun navigateToGroupSafeEntry() {
        val intent = Intent(mContext, GroupSafeEntryActivity::class.java)
        startActivity(intent)
    }

    private fun showReRegistrationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        var message = getString(R.string.passport_user_re_registration_detail_text)
        if (RegisterUserData.isInvalidUser(Preference.getUserIdentityType(mContext)))
            message = getString(R.string.invalid_user_re_registration_detail_text)
        builder.setTitle(getString(R.string.re_registration_title))
            .setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(getString(R.string.proceed)) { dialog, _ ->
                dialog.cancel()
                clearAppData()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(false)
        builder.create().show()
    }

    private fun clearAppData() {
        progress_bar_layout.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            Utils.clearDataAndStopBTService(mContext)
            navigateToOnBoarding()
        }
    }

    private fun navigateToOnBoarding() {
        startActivity(Intent(mContext, MainOnboardingActivity::class.java))
        activity?.finish()
    }
}
