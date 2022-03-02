package sg.gov.tech.bluetrace.revamp.home

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.facebook.shimmer.ShimmerFrameLayout
import com.github.amlcurran.showcaseview.ShowcaseView
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.Utils.withComma
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.debugger.PeekActivity
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.fragment.SafeEntryOnboardDialogFragment
import sg.gov.tech.bluetrace.groupCheckIn.safeEntry.GroupSafeEntryActivity
import sg.gov.tech.bluetrace.healthStatus.CovidHealthStatusFragment
import sg.gov.tech.bluetrace.home.PassportUserOverlayDialogFragment
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.revamp.popUp.PopUpFragment
import sg.gov.tech.bluetrace.revamp.safeentry.SafeEntryCheckInOutActivityV2
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord
import sg.gov.tech.bluetrace.utils.AndroidBus
import sg.gov.tech.bluetrace.zendesk.WebViewZendeskSupportFragment
import sg.gov.tech.safeentry.selfcheck.model.ConnectionState
import sg.gov.tech.safeentry.selfcheck.model.HealthStatus
import sg.gov.tech.safeentry.selfcheck.model.HealthStatusApiData
import sg.gov.tech.safeentry.selfcheck.model.SafeEntrySelfCheck

class HomeFragmentV3 : PopUpFragment(), SafeEntryOnboardDialogFragment.Callback,
    HealthStatusBarAdapter.Callback {

    private val TAG = "HomeFragmentV3"

    /*---------------------Views in fragment_home_ttv2.xml---------------------*/
    private lateinit var clAnnouncement: ConstraintLayout
    private lateinit var ivAnnouncementClose: AppCompatImageView
    private lateinit var tvAnnouncementTitle: AppCompatTextView

    private lateinit var tvBtText: AppCompatTextView
    private lateinit var tvBtTimeLapsed: AppCompatTextView
    private lateinit var ivBtInfo: AppCompatImageView

    private lateinit var bannerImageView: AppCompatImageView
    private lateinit var animationViewCheering: LottieAnimationView
    private lateinit var ivMoon: AppCompatImageView

    private lateinit var nonPassportUserLayout: ConstraintLayout
    private lateinit var passportUserLayout: ConstraintLayout

    private lateinit var clCalendar: ConstraintLayout
    private lateinit var clShareApp: ConstraintLayout

    private lateinit var btnPauseContactTracing: AppCompatTextView

    private lateinit var clProgressBar: ConstraintLayout

    /*---------------------Views in covid health status non_passport_user_layout.xml---------------------*/
    private lateinit var tvCovidHealthStatus: AppCompatTextView
    private lateinit var rvCovidHealthStatusBar: RecyclerView
    private lateinit var llTopLayout: LinearLayout

    /*---------------------Views in non_passport_user_layout.xml---------------------*/
    private lateinit var cvUnhappyBox: CardView
    private lateinit var tvUnhappyTitle: AppCompatTextView
    private lateinit var shimmerViewContainer: ShimmerFrameLayout
    private lateinit var cvPossibleExposureBox: CardView

    private lateinit var clCloseContact: ConstraintLayout
    private lateinit var ivCCIcon: AppCompatImageView
    private lateinit var tvCCTitle: AppCompatTextView
    private lateinit var tvCCDis: AppCompatTextView

    private lateinit var ibQRCode: AppCompatImageButton
    private lateinit var ibFav: AppCompatImageButton
    private lateinit var ibGroup: AppCompatImageButton

    private lateinit var safeEntryCard: CardView

    private lateinit var clLastCheckIn: ConstraintLayout
    private lateinit var tvLastVenue: AppCompatTextView
    private lateinit var tvViewPass: AppCompatTextView
    private lateinit var btnCheckOut: AppCompatButton

    /*---------------------Views in passport_user_layout.xml---------------------*/
    private lateinit var tvMessage: AppCompatTextView
    private lateinit var tvCheckEligibility: AppCompatTextView
    private lateinit var leftEmptyView: View
    private lateinit var btnReRegister: AppCompatButton
    private lateinit var rightEmptyView: View

    /*---------------------Variables---------------------*/

    private val homeVM: HomeViewModel by viewModel()

    lateinit var mContext: Context
    private var peekClickCounter = 0

    private var selectedMode = HomeViewModel.NOON

    private lateinit var remoteConfig: FirebaseRemoteConfig

    var isTutorialDialogSeen = true
    private lateinit var seRecords: List<SafeEntryRecord>
    private lateinit var mShowCaseView: ShowcaseView

    private var btExchangeCount = 0
    private var btExchangeDevices = 0

    private var btDevicesNearbyText: String = ""
    private var btTotalExchangesText: String = ""

    private lateinit var healthStatus: HealthStatus

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

        shimmerViewContainer.startShimmerAnimation()
        animationViewCheering.setOnClickListener {
            if (BuildConfig.DEBUG && ++peekClickCounter == 2) {
                peekClickCounter = 0
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_HOME_PAGE)
        initViews(view)

        if (!RegisterUserData.isInvalidPassportOrInvalidUser(
                Preference.getUserIdentityType(
                    TracerApp.AppContext
                )
            )
        )
            observeSeApi()

        mContext = view.context
        setupUI(view)
        checkIfHealthStatusIsNew()
        getBTExchanges()
        getUnExitedEntry()
    }

    private fun initViews(view: View) {
        clAnnouncement = view.findViewById(R.id.announcement_cl)
        ivAnnouncementClose = view.findViewById(R.id.announcement_close)
        tvAnnouncementTitle = view.findViewById(R.id.announcement_title)

        tvCovidHealthStatus = view.findViewById(R.id.tv_covid_status_text)
        rvCovidHealthStatusBar = view.findViewById(R.id.rv_health_status)
        llTopLayout = view.findViewById(R.id.top_layout)

        tvBtText = view.findViewById(R.id.tv_bt_text)
        tvBtTimeLapsed = view.findViewById(R.id.tv_bt_time_lapsed)
        ivBtInfo = view.findViewById(R.id.iv_bt_info)

        bannerImageView = view.findViewById(R.id.banner_iv)
        animationViewCheering = view.findViewById(R.id.animation_view_cheering)
        ivMoon = view.findViewById(R.id.iv_moon)

        nonPassportUserLayout = view.findViewById(R.id.non_passport_user_layout)
        passportUserLayout = view.findViewById(R.id.passport_user_layout)

        clCalendar = view.findViewById(R.id.cl_calendar)
        clShareApp = view.findViewById(R.id.cl_share_app)

        btnPauseContactTracing = view.findViewById(R.id.btnPauseContactTracing)

        clProgressBar = view.findViewById(R.id.progress_bar_layout)

        /*---------------------Views in non_passport_user_layout.xml---------------------*/
        cvUnhappyBox = view.findViewById(R.id.unhappy_box)
        tvUnhappyTitle = view.findViewById(R.id.unhappy_title)
        shimmerViewContainer = view.findViewById(R.id.shimmer_view_container)
        cvPossibleExposureBox = view.findViewById(R.id.possible_exposure_box)

        clCloseContact = view.findViewById(R.id.cl_close_contact)
        ivCCIcon = view.findViewById(R.id.iv_cc_icon)
        tvCCTitle = view.findViewById(R.id.tv_cc_title)
        tvCCDis = view.findViewById(R.id.tv_cc_dis)

        ibQRCode = view.findViewById(R.id.ib_qr_code)
        ibFav = view.findViewById(R.id.ib_fav)
        ibGroup = view.findViewById(R.id.ib_group)

        safeEntryCard = view.findViewById(R.id.card_safe_entry)

        clLastCheckIn = view.findViewById(R.id.cl_last_check_in)
        tvLastVenue = view.findViewById(R.id.tv_last_venue)
        tvViewPass = view.findViewById(R.id.tv_view_pass)
        btnCheckOut = view.findViewById(R.id.b_check_out)

        /*---------------------Views in passport_user_layout.xml---------------------*/
        tvMessage = view.findViewById(R.id.message_text_view)
        tvCheckEligibility = view.findViewById(R.id.check_eligibility_text_view)
        leftEmptyView = view.findViewById(R.id.left_empty_view)
        btnReRegister = view.findViewById(R.id.re_register_button)
        rightEmptyView = view.findViewById(R.id.right_empty_view)

        setListener()
    }

    private fun setListener() {
        ivBtInfo.setOnClickListener {
            openWebView(BuildConfig.ZENDESK_HOME_ALONE_URL)
        }

        ivAnnouncementClose.setOnClickListener {
            Preference.setAnnouncementSeen(activity as Context, true)
            showHideAnnouncement(false)
        }

        ibQRCode.setOnClickListener {
            startActivity(Intent(activity, SafeEntryActivity::class.java))
        }

        ibFav.setOnClickListener {
            val intent = Intent(activity, SafeEntryActivity::class.java)
            intent.putExtra(
                SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,
                SafeEntryActivity.FAV_FRAGMENT
            )
            startActivity(intent)
        }

        ibGroup.setOnClickListener {
            navigateToGroupSafeEntry()
        }

        tvViewPass.setOnClickListener {
            val intent = Intent(activity, SafeEntryCheckInOutActivityV2::class.java)
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_FRAGMENT_VALUE,
                SafeEntryCheckInOutActivityV2.SE_VIEW_PASS_VALUE
            )
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_VENUE,
                homeVM.convertQrResultToSeEntryRecord(seRecords[0])
            )
            startActivity(intent)
        }

        btnPauseContactTracing.setOnClickListener {
            pauseTraceTogether()
        }

        clShareApp.setOnClickListener {
            shareThisApp()
        }

        btnCheckOut.setOnClickListener {
            val intent = Intent(activity, SafeEntryCheckInOutActivityV2::class.java)
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_FRAGMENT_VALUE,
                SafeEntryCheckInOutActivityV2.SE_CHECK_OUT_VALUE
            )
            intent.putExtra(
                SafeEntryCheckInOutActivityV2.SE_VENUE,
                homeVM.convertQrResultToSeEntryRecord(seRecords[0])
            )
            startActivity(intent)
        }

        tvCheckEligibility.setOnClickListener {
            openWebView(BuildConfig.ZENDESK_WHY_CANT_I_USE_URL)
        }

        btnReRegister.setOnClickListener {
            showReRegistrationDialog()
        }

        clCalendar.setOnClickListener {
            openPassportUserOverlayScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPossibleExposureDisplay()
        val handler = Handler()
        handler.postDelayed(Runnable { checkUpdateSlotBanner() }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        homeVM.cancelBTExchangeJob()
        homeVM.removeUpdateBTTextTask()
    }

    override fun onDismissed() {
        mShowCaseView.hide()
        activityFragmentManager.dismiss("showcase_SE")
        isTutorialDialogSeen = true
        checkForAnnouncement()
    }

    override fun didProcessBackFragment(): Boolean {
        if (isAdded) {
            return if (childFragmentManager.backStackEntryCount > 0) {
                childFragmentManager.popBackStackImmediate()
                true
            } else false
        }
        return false
    }

    /**
     * Display UI depending on the user type (E.g. Valid or Invalid passport user, Invalid user)
     */
    private fun setupUI(view: View) {
        if (RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(view.context))) {
            nonPassportUserLayout.visibility = View.GONE
            passportUserLayout.visibility = View.VISIBLE

            if (RegisterUserData.isInvalidPassportUser(Preference.getUserIdentityType(view.context))) {
                clCalendar.visibility = View.VISIBLE
                tvMessage.text = HtmlCompat.fromHtml(
                    getString(R.string.passport_user_message),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

            } else if (RegisterUserData.isInvalidUser(Preference.getUserIdentityType(view.context))) {
                tvMessage.text = getString(R.string.invalid_user_message)
                tvCheckEligibility.visibility = View.GONE
                leftEmptyView.visibility = View.VISIBLE
                rightEmptyView.visibility = View.VISIBLE
            }
        } else if (RegisterUserData.isValidPassportUser(Preference.getUserIdentityType(view.context))) {
            clCalendar.visibility = View.VISIBLE
        }
    }

    /**
     * Get BTExchanges count every 5 minutes
     */
    private fun getBTExchanges() {
        //Create runnable task to display bt on top of the screen
        homeVM.createUpdateBtTextTask { currentViewIsDeviceNearby, timeLapsed ->
            if (currentViewIsDeviceNearby) {
                btViewForTotalExchanges()
            } else {
                setUpTimeLapsed(timeLapsed)
                btViewForDevicesNearby()
            }
        }

        //The actual job of getting the BT exchange values
        homeVM.doBTExchangeJob { btExchangeCount, btExchangeDevices ->
            this.btExchangeCount = btExchangeCount
            this.btExchangeDevices = btExchangeDevices
            updateBtCount()
            homeVM.setUpDisplayBtTask()
        }
    }

    private fun updateBtCount() {
        val connectedDevicesRange = homeVM.getConnectedDeviceRange(btExchangeDevices)

        btDevicesNearbyText = getString(
            R.string.trace_together_devices_nearby,
            connectedDevicesRange
        )

        btTotalExchangesText = getString(
            R.string.total_exchanges_today,
            withComma(btExchangeCount)
        )
    }

    private fun getUnExitedEntry() {
        homeVM.getUnExitedEntryRecords { liveDataRecords ->
            liveDataRecords.observe(viewLifecycleOwner, Observer<List<SafeEntryRecord>> { records ->
                seRecords = records
                if (seRecords.isNotEmpty()) {
                    hideShowSafeEntryCheckOutSection(true)
                    tvLastVenue.text =
                        if (seRecords[0].tenantName.isEmpty()) seRecords[0].venueName else seRecords[0].tenantName
                } else {
                    hideShowSafeEntryCheckOutSection(false)
                }
            })
        }
    }

    private fun showShimmer() {
        cvUnhappyBox.visibility = View.GONE

        if (remoteConfig.getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE))
            shimmerViewContainer.visibility = View.VISIBLE
        else
            shimmerViewContainer.visibility = View.GONE

        cvPossibleExposureBox.visibility = View.GONE
    }

    private fun hideCloseContact() {
        shimmerViewContainer.visibility = View.GONE
        cvPossibleExposureBox.visibility = View.GONE
        cvUnhappyBox.visibility = View.GONE
    }

    private fun showUnHappyBox(isServerDown: Boolean) {
        if (isServerDown)
            tvUnhappyTitle.text = getString(R.string.generic_unavailable)
        else
            tvUnhappyTitle.text = getString(R.string.network_issue_text)

        llTopLayout.visibility = View.VISIBLE
        rvCovidHealthStatusBar.visibility = View.GONE

        cvPossibleExposureBox.visibility = View.GONE
        shimmerViewContainer.visibility = View.GONE
        if (remoteConfig.getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE))
            cvUnhappyBox.visibility = View.VISIBLE
        else
            cvUnhappyBox.visibility = View.GONE
    }

    private fun showPossibleExposed(isExposed: Boolean) {
        cvUnhappyBox.visibility = View.GONE
        shimmerViewContainer.visibility = View.GONE
        if (remoteConfig.getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE))
            cvPossibleExposureBox.visibility = View.VISIBLE
        else
            cvPossibleExposureBox.visibility = View.GONE
        updateCloseContactView(isExposed)
    }

    private fun updateCloseContactView(isExposed: Boolean) {
        if (isExposed) {
            tvCCTitle.text = getString(R.string.possible_ex)
            tvCCDis.text = homeVM.getImageText(mContext, getString(R.string.you_were_at))
            ivCCIcon.background = ContextCompat.getDrawable(mContext, R.drawable.ic_exla)
            clCloseContact.setBackgroundColor(ContextCompat.getColor(mContext, R.color.pink))
        } else {
            tvCCTitle.text = getString(R.string.you_re_okay)
            tvCCDis.text = homeVM.getImageText(mContext, getString(R.string.based_on_al))
            ivCCIcon.background = ContextCompat.getDrawable(mContext, R.drawable.ic_ok)
            clCloseContact.setBackgroundColor(
                ContextCompat.getColor(
                    mContext,
                    R.color.close_box_bg
                )
            )
        }
        tvCCDis.makeLinks(
            Pair(getString(R.string.see_details), View.OnClickListener {
                (activity as MainActivity?)?.goToPossibleExposure()
            })
        )
    }

    private fun observeSeApi() {
        (activity as MainActivity).healthStatusApiStatus.observe(viewLifecycleOwner,
            Observer { state ->
                val userIdType = Preference.getUserIdentityType(requireContext())
                when (IdentityType.findByValue(userIdType)) {
                    IdentityType.FIN_STP, IdentityType.FIN_LTVP, IdentityType.FIN_WP, IdentityType.FIN_DP, IdentityType.NRIC -> {
                        tvCovidHealthStatus.visibility = View.VISIBLE
                        finNricUserSEView(state)
                    }
                    IdentityType.PASSPORT_VERIFIED -> {
                        tvCovidHealthStatus.visibility = View.GONE
                        passportUserSEView(state)
                    }
                    else -> {

                    }
                }
            })
    }

    private fun passportUserSEView(state: HealthStatusApiData) {
        when (state.state) {
            ConnectionState.Loading -> {
                showShimmer()
            }
            ConnectionState.Done -> {
                val response: SafeEntrySelfCheck? = state.data?.selfCheck
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
    }

    private fun finNricUserSEView(state: HealthStatusApiData) {
        when (state.state) {
            ConnectionState.Loading -> {
                showShimmer()
            }
            ConnectionState.Done -> {
                displayHealthStatusForNRICUI(state.data)
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
    }

    private fun displayHealthStatusForNRICUI(response: HealthStatus?) {
        if (response == null) {
            showUnHappyBox(true)
            return
        }

        healthStatus = response

        val list = ArrayList<HealthStatusModel>()

        if (response.vaccination.isVaccinated) // if vaccinated
            list.add(HealthStatusModel(response.vaccination.iconText, R.drawable.ic_vaccinated))
        else // if vaccination in progress or not vaccinated
            list.add(HealthStatusModel(response.vaccination.iconText, R.drawable.ic_not_vaccinated))

        if (response.selfCheck.count > 0) // if possible exposure
            list.add(HealthStatusModel(getString(R.string.possible_ex), R.drawable.ic_possible_exposure))
        else // if no possible exposure
            list.add(HealthStatusModel(getString(R.string.no_exposure_alerts), R.drawable.ic_no_exposure))

        list.add(HealthStatusModel(getString(R.string.covid_test_result), R.drawable.ic_coming_soon, 0))
        val mAdapter = HealthStatusBarAdapter(list)
        mAdapter.addCallback(this)
        val displayWidth = getdisplayWidth(requireContext())
        rvCovidHealthStatusBar.apply {
            layoutManager = object:GridLayoutManager(requireContext(), 1, GridLayoutManager.HORIZONTAL, false){
                override fun checkLayoutParams(lp: RecyclerView.LayoutParams) : Boolean {
                    // force width of viewHolder to be a fraction of RecyclerViews
                    // this will override layout_width from xml
                    if (displayWidth > 360) {
                        lp.width = width / 3
                    } else {
                        lp.width = 104
                    }
                    return true
                }
            }
            adapter = mAdapter

        }
        llTopLayout.visibility = View.GONE
        rvCovidHealthStatusBar.visibility = View.VISIBLE
    }

    fun getdisplayWidth(context: Context): Int {
        var wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var displaymetrics = DisplayMetrics()
        wm.getDefaultDisplay().getMetrics(displaymetrics)
        return displaymetrics.widthPixels
    }

    private fun hideShowSafeEntryCheckOutSection(show: Boolean) {
        if (show) {
            safeEntryCard.cardElevation = Utils.dpToPx(requireContext(), 2f).toFloat()
            clLastCheckIn.visibility = View.VISIBLE
        } else {
            safeEntryCard.cardElevation = 0f
            clLastCheckIn.visibility = View.GONE
        }
    }

    private fun pauseTraceTogether() {
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
            .setPositiveButton(R.string.ok) { _, _ ->
                var pauseUntil: Long = 0

                when (radioGroup.checkedRadioButtonId) {
                    R.id.pause_30m -> {
                        CentralLog.i(TAG, "pause for 30m")
                        pauseUntil = homeVM.getTimeToPause(0.5)
                    }
                    R.id.pause_2h -> {
                        CentralLog.i(TAG, "pause for 2h")
                        pauseUntil = homeVM.getTimeToPause(2.0)
                    }
                    R.id.pause_8h -> {
                        CentralLog.i(TAG, "pause for 8h")
                        pauseUntil = homeVM.getTimeToPause(8.0)
                    }
                    else -> {
                        CentralLog.i(TAG, "pause for XXX - invalid option")
                    }
                }

                //pause the service
                Utils.pauseBluetoothMonitoringService(TracerApp.AppContext, pauseUntil)
            }

        val dialog = builder.create()
        dialog.show()

    }

    /**
     * checks whether to display the self check box or not
     * depending upon remote config toggle
     */
    private fun checkPossibleExposureDisplay() {
        if (RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(mContext))
            || !remoteConfig.getBoolean(RemoteConfigUtils.REMOTE_CONFIG_TOGGLE_POSSIBLE_EXPOSURE)
        )
            hideCloseContact()
    }

    private fun checkForAnnouncement() {

        val announcement = homeVM.getAnnouncementRemoteConfig(mContext)

        if (announcement.isBlank())
            return

        val announcementModel = homeVM.getAnnouncementModel(announcement)
        val anncText = announcementModel.getAnnouncementMsg()

        clAnnouncement.setOnClickListener {
            if (!announcementModel.url.isNullOrBlank()) {
                openWebView(announcementModel.url, false)
            }
        }
        tvAnnouncementTitle.text = anncText

        val showAnnouncement = homeVM.displayAnnouncementAppVersionCheck(
            getCurrentAppVersion(),
            announcementModel.minAppVersion,
            announcementModel.maxAppVersion
        )

        if (!anncText.isNullOrBlank() && showAnnouncement) {
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

    private fun getCurrentAppVersion(): String? {
        return activity?.let { Utils.getCurrentAppVersionWithoutSuffix(it) }
    }

    private fun showHideAnnouncement(isShow: Boolean) {
        clAnnouncement.visibility = if (isShow) View.VISIBLE else View.GONE
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
        PassportUserOverlayDialogFragment().show(childFragmentManager, "HomeFragmentV3")
    }

    private fun checkUpdateSlotBanner() {
        val slot = homeVM.getTimeSlot()
        if (selectedMode != slot) {
            selectedMode = slot
            animateBannerImage(slot)
        }
    }

    private fun animateBannerImage(slot: Int) {
        val drawable = homeVM.getSlotBannerImg(mContext, selectedMode)
        drawable?.let {
            bannerImageView.animate()
                .alpha(0.1f)
                .setDuration(500)
                .setListener(object : AnimatorListener {
                    override fun onAnimationCancel(animator: Animator?) {}
                    override fun onAnimationRepeat(animator: Animator?) {}
                    override fun onAnimationStart(animator: Animator?) {}
                    override fun onAnimationEnd(animator: Animator?) {
                        bannerImageView.setImageDrawable(drawable)
                        bannerImageView.animate().alpha(1f).duration = 1200
                        if (slot == HomeViewModel.NIGHT)
                            ivMoon.visibility = View.VISIBLE
                        else
                            ivMoon.visibility = View.GONE
                    }
                })
        }
    }

    private fun btViewForDevicesNearby() {
        tvBtText.text = btDevicesNearbyText
        tvBtTimeLapsed.visibility = View.VISIBLE
    }

    private fun btViewForTotalExchanges() {
        tvBtText.text = btTotalExchangesText
        tvBtTimeLapsed.visibility = View.GONE
    }

    private fun setUpTimeLapsed(timeInMillis: Long) {
        tvBtTimeLapsed.text = getString(R.string.min_ago, homeVM.getTimeLapsedInText(timeInMillis))
    }

    private fun openWebView(url: String, isFabVisible: Boolean = true) {
        val fragment = WebViewZendeskSupportFragment()
        fragment.setUrl(url)
        if (!isFabVisible)
            fragment.setFabInvisible()
        childFragmentManager.beginTransaction()
            .addToBackStack(fragment.customTag)
            .replace(R.id.f_child_content, fragment)
            .commit()
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
        clProgressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            Utils.clearDataAndStopBTService(mContext)
            navigateToOnBoarding()
        }
    }

    private fun navigateToGroupSafeEntry() {
        val intent = Intent(mContext, GroupSafeEntryActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToOnBoarding() {
        startActivity(Intent(mContext, MainOnboardingActivity::class.java))
        activity?.finish()
    }

    override fun onHealthStatusItemClick(position: Int) {
        Preference.putIsHealthStatusNew(requireContext(), false)
        navigateToHealthStatusScreen(position)
    }

    private fun navigateToHealthStatusScreen(position: Int) {
        AndroidBus.healthStatus.onNext(healthStatus)
        val fragment = CovidHealthStatusFragment()
        val bundle = Bundle()
        bundle.putInt(CovidHealthStatusFragment.HEALTH_STATUS_POSITION, position)
        fragment.arguments = bundle
        (activity as MainActivity).openFragment((activity as MainActivity).LAYOUT_MAIN_ID, fragment)
    }

    private fun checkIfHealthStatusIsNew() {
        if (!Preference.isHealthStatusNew(requireContext()))
            tvCovidHealthStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
}