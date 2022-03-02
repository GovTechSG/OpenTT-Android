package sg.gov.tech.bluetrace.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import org.koin.android.viewmodel.ext.android.viewModel
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_bluetooth_history.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.favourite.FavouriteViewModel
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import sg.gov.tech.bluetrace.history.YourDataSafeDialogFragment
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.revamp.requestModel.CheckOutRequestModel
import sg.gov.tech.bluetrace.revamp.responseModel.CheckOutResponseModel
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecordStorage
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordStorage
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder
import sg.gov.tech.bluetrace.utils.TTDatabaseCryptoManager
import sg.gov.tech.safeentry.selfcheck.HealthStatusApi
import sg.gov.tech.safeentry.selfcheck.model.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

data class RawHistoryData(
    val day: String,
    val dayInMs: Long,
    val recordsCount: Int,
    val seRecords: List<SafeEntryRecord>,
    val matches: List<SafeEntryMatch>?
)

data class HistoryRecord(
    val locationLabel: String,
    val checkInTimeMs: Long,
    var checkOutTimeMs: Long?,
    val venueId: String?,
    val tenantId: String?,
    val postalCode: String?,
    val hotSpots: MutableList<HotSpotData>,
    val groupMembers: String?,
    val dbSeRecordId: Int?
)

data class HotSpotData(
    val locationLabel: String,
    val address: String,
    val checkInTimeMs: Long,
    val checkOutTimeMs: Long?
)

data class DayHistoryRecord(
    val dayInMs: Long,
    val btRecordsCount: Int,
    val historyRecords: List<HistoryRecord>
)

class BluetoothHistoryFragment : MainActivityFragment("BluetoothHistoryFragment"),
    HistoryListAdapter.Callback, CoroutineScope {
    private lateinit var observableRecords: Observable<List<DayHistoryRecord>>
    override val coroutineContext: CoroutineContext get() = Dispatchers.Main + Job()

    private val favouriteViewModel: FavouriteViewModel by viewModel()
    private var historyListAdapter: HistoryListAdapter? = null
    private lateinit var favouriteRecordList: ArrayList<FavouriteRecord>
    private lateinit var allDisplayedDbRecords: List<SafeEntryRecord>
    private val apiHandler: ApiHandler by inject()
    private val disposables: MutableList<Disposable> = ArrayList()

    private fun getHotSpots(
        seRecord: SafeEntryRecord,
        matches: List<SafeEntryMatch>?
    ): MutableList<HotSpotData> {
        matches?.forEach { match ->
            if (seRecord.checkInTimeMS / 1000 == match.safeentry.checkin.time &&
                seRecord.postalCode == match.safeentry.location.postalCode
            ) {
                return match.hotspots.map { hotSpot ->
                    HotSpotData(
                        hotSpot.location.description ?: "Unknown",
                        hotSpot.location.address ?: "Unknown",
                        hotSpot.timeWindow.start * 1000,
                        hotSpot.timeWindow.end * 1000
                    )
                }.toMutableList()
            }
        }
        return ArrayList()
    }

    private fun getHistoryListData(historyData: RawHistoryData): DayHistoryRecord {
        val historyRecords: MutableList<HistoryRecord> = ArrayList()

        val notInDb: MutableList<SafeEntryMatch> = ArrayList()
        val inDb: MutableList<SafeEntryMatch> = ArrayList()

        historyData.matches?.forEach { match ->
            var isMatch = false
            historyData.seRecords.forEach { seRecord ->
                isMatch = isMatch or seRecord.matchesMatch(match)
            }

            if (!isMatch)
                notInDb.add(match)
            else
                inDb.add(match)
        }

        historyRecords.addAll(notInDb.map {
            val checkout = if (it.safeentry.isCheckout()) it.safeentry.checkout?.time ?: 0L else 0L
            HistoryRecord(
                it.safeentry.location.description ?: "Unknown",
                it.safeentry.checkin.time * 1000,
                checkout * 1000,
                hotSpots = it.hotspots.map { hotSpot ->
                    HotSpotData(
                        hotSpot.location.description ?: "Unknown",
                        hotSpot.location.address ?: "Unknown",
                        hotSpot.timeWindow.start * 1000,
                        hotSpot.timeWindow.end * 1000
                    )
                }.toMutableList()
                , tenantId = null,
                venueId = null,
                postalCode = it.safeentry.location.postalCode,
                dbSeRecordId = null,
                groupMembers = null
            )
        })

        val seRecords = historyData.seRecords.map {
            val checkout = if (it.isCheckOut()) it.checkOutTimeMS else null
            var seMatch: SafeEntryMatch? = null

            historyData.matches?.forEach { match ->
                if (it.checkInTimeMS / 1000 == match.safeentry.checkin.time &&
                    it.postalCode == match.safeentry.location.postalCode
                ) {
                    seMatch = match
                }
            }
            val placeName = seMatch?.safeentry?.location?.description ?: it.getPlaceName()
            val matchCheckout = seMatch?.safeentry?.checkout?.time?.times(1000L)

            HistoryRecord(
                placeName,
                it.checkInTimeMS,
                checkout ?: matchCheckout,
                venueId = it.venueId,
                tenantId = it.tenantId,
                hotSpots = getHotSpots(it, inDb),
                postalCode = it.postalCode,
                dbSeRecordId = it.id,
                groupMembers = it.groupMembers
            )
        }

        historyRecords.addAll(seRecords)
        return DayHistoryRecord(
            historyRecords = historyRecords,
            dayInMs = historyData.dayInMs,
            btRecordsCount = historyData.recordsCount
        )
    }

    override fun didProcessBack(): Boolean {
        return false
    }

    fun setupList(state: HealthStatusApiData) {
        val selfCheckInRecords = state.data?.selfCheck

        observableRecords = Observable.create<List<DayHistoryRecord>> {
            val historyListData = ArrayList<DayHistoryRecord>()
            val recordsCountList =
                StreetPassRecordStorage(TracerApp.AppContext).getAllRecordsCountForDays(
                    days = BuildConfig.NO_OF_DAYS_FOR_HISTORY
                )
            val seRecords: ArrayList<List<SafeEntryRecord>> =
                SafeEntryRecordStorage(TracerApp.AppContext).getRecordsForDays(
                    days = BuildConfig.NO_OF_DAYS_FOR_HISTORY
                )

            val allDisplayedDbRecordsTemp = ArrayList<SafeEntryRecord>()
            seRecords.forEach { dayOfRecords ->
                allDisplayedDbRecordsTemp.addAll(dayOfRecords)
            }
            allDisplayedDbRecords = allDisplayedDbRecordsTemp

            var previousDay =
                DateTools.getStartOfDay(System.currentTimeMillis()).timeInMillis

            repeat(BuildConfig.NO_OF_DAYS_FOR_HISTORY) { index ->
                var recordsCount = recordsCountList[index]
                if (previousDay == DateTools.getStartOfDay(System.currentTimeMillis()).timeInMillis) {
                    recordsCount = -1
                }
                val matchesForTheDay = selfCheckInRecords?.data?.filter { match ->
                    (Utils.compareDate(
                        match.safeentry.checkin.time * 1000,
                        previousDay
                    ) == 0)
                }

                val dayHistoryRecord = getHistoryListData(
                    RawHistoryData(
                        dayInMs = previousDay,
                        day = Utils.getShortDateWithComaAfterDay(previousDay),
                        recordsCount = recordsCount,
                        seRecords = seRecords[index],
                        matches = matchesForTheDay
                    )
                )
                //do not add to the list if its current date and the data is null
                val registrationDay = activity?.let { context -> Preference.getUserRegistrationDate(context) }
                if(registrationDay != null && registrationDay != 0L){
                    val currentDay = DateTools.getStartOfDay(System.currentTimeMillis()).timeInMillis
                    //for current day add to the list only if it has data
                    if(Date(dayHistoryRecord.dayInMs).equals(Date(currentDay))){
                        if(dayHistoryRecord.historyRecords.isNotEmpty())
                            historyListData.add(dayHistoryRecord)
                    }
                    //if history record date is equal to registration day or is before registration day
                    else if(Date(dayHistoryRecord.dayInMs).equals(Date(registrationDay))
                        || Date(dayHistoryRecord.dayInMs).before(Date(registrationDay))){
                        //add only if it has checkin checkout or hotspot record
                        if(dayHistoryRecord.historyRecords.isNotEmpty())
                            historyListData.add(dayHistoryRecord)
                    }
                    //if history record is after registration date
                    else if(Date(dayHistoryRecord.dayInMs).after(Date(registrationDay))){
                        historyListData.add(dayHistoryRecord)
                    }
                    else{
                        historyListData.add(dayHistoryRecord)
                    }
                }
                else{
                    historyListData.add(dayHistoryRecord)
                }
                previousDay -= TimeUnit.DAYS.toMillis(1)
            }

            if (BuildConfig.DEBUG) {
                val mocks = SelfCheckInMocks.getMocks()
                val temp = allDisplayedDbRecords.toMutableList()
                temp.add(
                    SafeEntryRecord(
                        "NO MATCH",
                        "",
                        "NO MATCH",
                        "",
                        "222222",
                        "",
                        3000
                    )
                )
                allDisplayedDbRecords = temp
                historyListData.add(
                    getHistoryListData(
                        RawHistoryData(
                            dayInMs = 0,
                            day = "Not real data",
                            recordsCount = -2,
                            seRecords = mocks.first,
                            matches = mocks.second
                        )
                    )
                )
            }

            it.onNext(historyListData)
        }

        val disposable = observableRecords.observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { dataToCombine ->
                getFavouriteRecords(dataToCombine)
            }
        disposables.add(disposable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(TracerApp.AppContext))) {
            HealthStatusApi.healthStatusApiStatus.observe(
                viewLifecycleOwner,
                Observer { state ->
                    when (state.state) {
                        ConnectionState.Loading -> {
                        }

                        ConnectionState.Done -> {
                            setupList(state)
                        }

                        ConnectionState.Error -> {
                            setupList(state)
                        }
                        ConnectionState.NoNetwork -> {
                            setupList(state)
                        }

                        else -> {

                        }
                    }
                }
            )
        } else {
            val state = HealthStatusApiData.done(null)
            setupList(state)
        }

        AnalyticsUtils().screenAnalytics(
            activity as Activity,
            AnalyticsKeys.SCREEN_NAME_HISTORY_MAIN
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bluetooth_history, container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.forEach {
            it.dispose()
        }
    }

    override fun onStarClicked(isChecked: Boolean, safeEntryRecord: HistoryRecord) {
        if (isChecked) {
            insertRecord(safeEntryRecord.venueId, safeEntryRecord.tenantId)
        } else {
            safeEntryRecord.venueId?.let {
                safeEntryRecord.tenantId?.let { it1 ->
                    deleteRecord(
                        it,
                        it1
                    )
                }
            }
        }
    }

    private fun decryptedGroupMembers(groupMember: String?): ArrayList<String> {
        var groupIds = ArrayList<String>()
        var groupMembersStr = groupMember
        if (!groupMembersStr.isNullOrEmpty()) {
            var groupMembersList: List<String> = groupMembersStr.split(",").map { it.trim() }
            groupMembersList.forEach { nric ->
                TTDatabaseCryptoManager.getDecryptedFamilyMemberNRIC(requireContext(), nric)
                    ?.let { it -> groupIds.add(it) }
            }
        }
        return groupIds
    }

    override fun onCheckoutNowClicked(
        safeEntryRecord: HistoryRecord,
        position: Int,
        historyRecordItemPosition: Int
    ) {
        val parentFragment = this.parentFragment
        if (parentFragment is BluetoothHistoryPagerFragment) {
            parentFragment.displayLoader(true)
        }
        var groupIds: ArrayList<String> = decryptedGroupMembers(safeEntryRecord.groupMembers)
        val user = activity?.let { Preference.getEncryptedUserData(it) }
        var requestModel: CheckOutRequestModel? = null
        if (groupIds.isNullOrEmpty()) {
            if (user != null) {
                requestModel = CheckOutRequestModel(
                    safeEntryRecord.venueId,
                    safeEntryRecord.tenantId,
                    safeEntryRecord.locationLabel,
                    user.id
                )
            }
        } else {
            val groupIdList = mutableListOf<CheckOutRequestModel.CheckInOutGroupIds>()
            groupIds.forEach { id ->
                val obj = CheckOutRequestModel.CheckInOutGroupIds(id)
                groupIdList.add(obj)
            }
            if (user != null) {
                requestModel = CheckOutRequestModel.CheckOutRequestWithGroupMember(
                    safeEntryRecord.venueId,
                    safeEntryRecord.tenantId,
                    safeEntryRecord.locationLabel,
                    user.id,
                    groupIdList
                )
            }
        }
        val loggerTAG = "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
        val result = requestModel?.let { apiHandler.checkOutUser(it) }
        if (result != null) {
            val disposable = result.subscribeWith(object :
                DisposableSingleObserver<ApiResponseModel<out Any>>() {
                override fun onError(e: Throwable) {
                    if (isAdded) {
                        if (parentFragment is BluetoothHistoryPagerFragment) {
                            parentFragment.hideLoader()
                        }
                        var alertType = AlertType.CHECK_OUT_NETWORK_ERROR_DIALOG
                        activity?.let { context ->
                            TTAlertBuilder().show(context, alertType) {
                                if (it) {
                                    val user = Preference.getEncryptedUserData(TracerApp.AppContext)
                                    user?.let {
                                        onCheckoutNowClicked(
                                            safeEntryRecord,
                                            position,
                                            historyRecordItemPosition
                                        )
                                    }
                                }
                            }
                        }
                        CentralLog.e(loggerTAG, "Check-out fail: ${e.message}")
                        DBLogger.e(
                            DBLogger.LogType.SAFEENTRY,
                            loggerTAG,
                            "Check-out fail: ${e.message}",
                            null
                        )
                    }
                }

                override fun onSuccess(data: ApiResponseModel<out Any>) {
                    val result = data.result
                    if (data.isSuccess) {
                        if (result is CheckOutResponseModel) {
                            result.timeStamp?.let {
                                updateCheckoutTime(
                                    DateTools.convertCheckInOutTimeToMs(it),
                                    safeEntryRecord,
                                    position,
                                    historyRecordItemPosition
                                )
                            }
                        }
                    }
                }
            })
            disposables.add(disposable)
        } else {
            //dismiss loader
            if (parentFragment is BluetoothHistoryPagerFragment) {
                parentFragment.hideLoader()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun updateCheckoutTime(checkOutTimeInMs: Long,safeEntryRecord: HistoryRecord, position: Int, historyRecordItemPosition: Int) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val parentFragment = this.parentFragment
        val seDao = StreetPassRecordDatabase.getDatabase(TracerApp.AppContext).safeEntryDao()
        Observable.create<Boolean> {
            safeEntryRecord.dbSeRecordId?.let { id -> seDao.exitVenue(id, checkOutTimeInMs) }
            it.onNext(true)
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe {
                if (isAdded) {
                    if(parentFragment is BluetoothHistoryPagerFragment){
                        parentFragment.hideLoader()
                    }
                    try {
                        //update the UI with checkout time
                        historyListAdapter?.updateCheckOutTime(
                            checkOutTimeInMs,
                            position,
                            historyRecordItemPosition
                        )
                        refreshAdapter()
                        favouriteViewModel.showSnackBar(
                            requireContext(),
                            main_layout,
                            R.string.successful_check_out
                        )
                    } catch (e: Exception) {
                        CentralLog.e("SE_CHECK_INOUT", "error: ${e.message}")
                        DBLogger.e(
                            DBLogger.LogType.SAFEENTRY,
                            loggerTAG,
                            "error: ${e.message}",
                            DBLogger.getStackTraceInJSONArrayString(e)
                        )
                    }
                }
            }
    }

    private fun insertRecord(venueId: String?, tenantId: String?) {
        val matchingRecords = allDisplayedDbRecords.filter {
            it.venueId == venueId && it.tenantId == tenantId
        }
        if (matchingRecords.isNotEmpty()) {
            val safeEntryRecord = matchingRecords[0]
            launch {
                val favouriteRecord =
                    safeEntryRecord.let {
                        favouriteViewModel.insertRecord(
                            requireContext(),
                            it
                        )
                    }
                favouriteRecordList.add(favouriteRecord)
                refreshAdapter()
                favouriteViewModel.showSnackBar(
                    requireContext(),
                    main_layout,
                    R.string.saved_to_favourites
                )
                AnalyticsUtils().trackEvent(
                    AnalyticsKeys.SCREEN_NAME_HISTORY_MAIN,
                    AnalyticsKeys.SE_TAP_FAVOURITE,
                    AnalyticsKeys.TRUE
                )
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun deleteRecord(venueId: String, tenantId: String) {
        Observable.create<Boolean> {
            favouriteViewModel.deleteRecord(requireContext(), venueId, tenantId)
            it.onNext(true)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe {
                for (favouriteRecord in favouriteRecordList) {
                    if (favouriteRecord.venueId == venueId && favouriteRecord.tenantId == tenantId) {
                        favouriteRecordList.remove(favouriteRecord)
                        break
                    }
                }
                refreshAdapter()
                favouriteViewModel.showSnackBar(
                    requireContext(),
                    main_layout,
                    R.string.removed_from_favourites
                )
                AnalyticsUtils().trackEvent(
                    AnalyticsKeys.SCREEN_NAME_HISTORY_MAIN,
                    AnalyticsKeys.SE_TAP_FAVOURITE,
                    AnalyticsKeys.FALSE
                )
            }
    }

    @SuppressLint("CheckResult")
    private fun getFavouriteRecords(historyDataList: List<DayHistoryRecord>) {
        Observable.create<List<FavouriteRecord>> {
            val records = favouriteViewModel.getAllRecords(requireContext())
            it.onNext(records)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { favouriteRecords ->
                favouriteRecordList = favouriteRecords as ArrayList<FavouriteRecord>
                if (isAdded) {
                    setAdapter(historyDataList)
                    historyListAdapter?.notifyDataSetChanged()
                }
            }
    }

    private fun setAdapter(historyDataList: List<DayHistoryRecord>) {
        if(historyDataList.isEmpty()){
            tv_no_records_yet.visibility = View.VISIBLE
        }
        else{
            rv_possible_exposure.layoutManager = LinearLayoutManager(activity)
            historyListAdapter = HistoryListAdapter(
                activity!!,
                View.OnClickListener {
                    YourDataSafeDialogFragment().show(
                        childFragmentManager,
                        "bluetoothHistoryFragment"
                    )
                },
                historyDataList,
                favouriteRecordList
            )
            tv_no_records_yet.visibility = View.GONE
            rv_possible_exposure.adapter = historyListAdapter
            historyListAdapter?.addCallback(this)
        }

    }

    private fun refreshAdapter() {
        val recyclerViewState = rv_possible_exposure.layoutManager?.onSaveInstanceState()
        rv_possible_exposure.adapter = historyListAdapter
        historyListAdapter?.notifyDataSetChanged()
        rv_possible_exposure.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    companion object SelfCheckInMocks {
        fun getMocks(): Pair<ArrayList<SafeEntryRecord>, ArrayList<SafeEntryMatch>> {
            val matchWithDifferentLocation = Pair(
                SafeEntryRecord(
                    "LOCATION NAME FROM LOCAL DB",
                    "",
                    "LOCATION NAME FROM LOCAL DB",
                    "",
                    "123456",
                    "",
                    0
                ), SafeEntryMatch(
                    SafeEntryInfo(
                        CheckInInfo("1", 0, ""),
                        MatchLocation(
                            "",
                            "123456",
                            "LOCATION NAME FROM API"
                        ),
                        CheckoutInfo("", System.currentTimeMillis() / 1000, "")
                    ),
                    arrayListOf(
                        HotSpot(
                            TimeWindow(0, System.currentTimeMillis() / 1000),
                            MatchLocation("Hotspot 1", "123456", "Hotspot 1"),
                            ""
                        ),
                        HotSpot(
                            TimeWindow(0, System.currentTimeMillis() / 1000),
                            MatchLocation("Hotspot 2", "123456", "Hotspot 2"),
                            ""
                        )
                    ),
                    ""
                )
            )

            val matchWithNoCheckOut = Pair(
                SafeEntryRecord(
                    "SAME NAME BUT NO CHECKOUT IN LOCAL RECORD",
                    "",
                    "SAME NAME BUT NO CHECKOUT IN LOCAL RECORD",
                    "",
                    "123456",
                    "",
                    1000
                ), SafeEntryMatch(
                    SafeEntryInfo(
                        CheckInInfo("1", 1, ""),
                        MatchLocation(
                            "",
                            "123456",
                            "SAME NAME BUT NO CHECKOUT IN LOCAL RECORD"
                        ),
                        CheckoutInfo("", System.currentTimeMillis() / 1000, "")
                    ),
                    arrayListOf(
                        HotSpot(
                            TimeWindow(0, System.currentTimeMillis() / 1000),
                            MatchLocation("Hotspot 1", "123456", "Hotspot 1"),
                            ""
                        ),
                        HotSpot(
                            TimeWindow(0, System.currentTimeMillis() / 1000),
                            MatchLocation("Hotspot 2", "123456", "Hotspot 2"),
                            ""
                        )
                    ),
                    ""
                )
            )


            val matchWithDifferentLocationNameAndNoCheckOut = Pair(
                SafeEntryRecord(
                    "LOCATION NAME FROM LOCAL DB",
                    "",
                    "LOCATION NAME FROM LOCAL DB",
                    "",
                    "123456",
                    "",
                    2000
                ), SafeEntryMatch(
                    SafeEntryInfo(
                        CheckInInfo("1", 2, ""),
                        MatchLocation(
                            "",
                            "123456",
                            "Different Location and no check out : LOCATION NAME FROM API"
                        ),
                        null
                    ),
                    arrayListOf(
                        HotSpot(
                            TimeWindow(0, System.currentTimeMillis() / 1000),
                            MatchLocation("Hotspot 1", "123456", "Hotspot 1"),
                            ""
                        ),
                        HotSpot(
                            TimeWindow(0, System.currentTimeMillis() / 1000),
                            MatchLocation("Hotspot 2", "123456", "Hotspot 2"),
                            ""
                        )
                    ),
                    ""
                )
            )


            val noMatchRecord = SafeEntryRecord(
                "NO MATCH",
                "",
                "NO MATCH",
                "",
                "222222",
                "",
                3000
            )
            noMatchRecord.checkOutTimeMS = System.currentTimeMillis()
            val noMatch = Pair(
                noMatchRecord, SafeEntryMatch(
                    SafeEntryInfo(
                        CheckInInfo("1", 1598341238605 / 1000, ""),
                        MatchLocation(
                            "",
                            "123456",
                            "LOCATION NAME FROM API BECAUSE NOT SAME IN LOCAL DB"
                        ),
                        CheckoutInfo("", System.currentTimeMillis() / 1000, "")
                    ),
                    arrayListOf(
                        HotSpot(
                            TimeWindow(0, System.currentTimeMillis() / 1000),
                            MatchLocation("Hotspot 1", "123456", "Hotspot 1"),
                            ""
                        ),
                        HotSpot(
                            TimeWindow(0, System.currentTimeMillis() / 1000),
                            MatchLocation("Hotspot 2", "123456", "Hotspot 2"),
                            ""
                        )
                    ),
                    ""
                )
            )

            val scenarios = arrayOf(
                matchWithNoCheckOut,
                matchWithDifferentLocationNameAndNoCheckOut,
                noMatch,
                matchWithDifferentLocation
            )
            val seMocks = ArrayList<SafeEntryRecord>()
            val selfCheckInMocks = ArrayList<SafeEntryMatch>()
            scenarios.forEach { scenario ->
                seMocks.add(scenario.first)
                selfCheckInMocks.add(scenario.second)
            }
            return Pair(seMocks, selfCheckInMocks)
        }
    }
}
