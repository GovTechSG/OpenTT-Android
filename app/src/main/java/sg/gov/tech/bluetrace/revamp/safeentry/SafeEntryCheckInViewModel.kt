package sg.gov.tech.bluetrace.revamp.safeentry

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteDao
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.revamp.requestModel.CheckInOutGroupIds
import sg.gov.tech.bluetrace.revamp.requestModel.CheckInRequestModel
import sg.gov.tech.bluetrace.revamp.requestModel.CheckInRequestWithGroupMember
import sg.gov.tech.bluetrace.streetpass.persistence.FamilyMembersRecord
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryDao
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord

class SafeEntryCheckInViewModel(
    private val apiHandler: ApiHandler,
    private val favouriteDao: FavouriteDao,
    val safeEntryDao: SafeEntryDao
) : ViewModel() {

    var isFav: MutableLiveData<Boolean> = MutableLiveData()
    var checkInApiResponse: MutableLiveData<ApiResponseModel<out Any>> = MutableLiveData()
    private var disposables = CompositeDisposable()
    private val TAG = "SafeEntryCheckInViewModel"

    fun isVenueFavorite(venue: QrResultDataModel) {
        viewModelScope.launch {
            var favouriteRecord: FavouriteRecord? = withContext(Dispatchers.IO) {
                favouriteDao.getFavouriteRecordById(venue.venueId, venue.tenantId)
            }
            //if venue name  or tenant name has changed update venue name in fav DB
            if(favouriteRecord?.venueName != venue.venueName || favouriteRecord?.tenantName != venue.tenantName){
                updateVenueNameInFavDB(venue.venueName,venue.tenantName,venue.venueId, venue.tenantId)
            }
            isFav.value = (favouriteRecord != null)
        }
    }

    /**
     * if the venue name or tenant name has changed, updates the DB with new venue name
     */
    private fun updateVenueNameInFavDB(
        venueName: String?,
        tenantName: String?,
        venueId: String?,
        tenantId: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            favouriteDao.updateVenueName(venueName, tenantName, venueId, tenantId)
        }
    }

    fun insertFavourite(venue: QrResultDataModel, isInserted: (Boolean) -> Unit) {
        isFav.value = true
        val favouriteRecord = FavouriteRecord(
            venue.venueId ?: "",
            venue.venueName ?: "",
            venue.tenantId ?: "",
            venue.tenantName ?: "",
            venue.postalCode ?: "",
            venue.address ?: ""
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                favouriteDao.insert(favouriteRecord)
            }
            isInserted.invoke(true)
            AnalyticsUtils().trackEvent(
                AnalyticsKeys.SCREEN_NAME_CHECK_IN_CONFIRMATION,
                AnalyticsKeys.SE_TAP_FAVOURITE,
                AnalyticsKeys.TRUE
            )
        }
    }

    fun deleteFavourite(venue: QrResultDataModel, isDeleted: (Boolean) -> Unit) {
        isFav.value = false
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                favouriteDao.deleteRecord(
                    venue.venueId,
                    venue.tenantId
                )
            }
            isDeleted(true)
            AnalyticsUtils().trackEvent(
                AnalyticsKeys.SCREEN_NAME_CHECK_IN_CONFIRMATION,
                AnalyticsKeys.SE_TAP_FAVOURITE,
                AnalyticsKeys.FALSE
            )
        }
    }

    fun callUserCheckIn(
        user: RegisterUserData,
        groupIds: ArrayList<String>,
        venue: QrResultDataModel
    ) {
        val requestModel: CheckInRequestModel
        if (groupIds.isNullOrEmpty()) {
            requestModel = CheckInRequestModel(
                user.id,
                venue.venueId,
                venue.tenantId,
                venue.venueName,
                venue.postalCode ?: ""
            )
        } else {
            var groupIdList = mutableListOf<CheckInOutGroupIds>()
            groupIds.forEach { id ->
                var obj = CheckInOutGroupIds(id)
                groupIdList.add(obj)
            }
            requestModel = CheckInRequestWithGroupMember(
                user.id,
                venue.venueId,
                venue.tenantId,
                venue.venueName,
                venue.postalCode ?: "", groupIdList
            )
        }
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"

        val result = apiHandler.checkInUser(requestModel)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                CentralLog.e(loggerTAG, "Check-in fail: ${e.message}")
                DBLogger.e(
                    DBLogger.LogType.SAFEENTRY,
                    loggerTAG,
                    "Check-in fail: ${e.message}",
                    DBLogger.getStackTraceInJSONArrayString(e)
                )
                checkInApiResponse.value = (ApiResponseModel(false, e.message))
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                CentralLog.e(TAG, "Check-in success")
                checkInApiResponse.value = data
            }

        })
        disposables.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }

    fun insertSeRecordToDB(
        checkInTime: String,
        venue: QrResultDataModel,
        groupMembersList: List<FamilyMembersRecord>,
        isRecordInserted: (Boolean) -> Unit
    ) {
        val checkInTimeInMs = DateTools.convertCheckInOutTimeToMs(checkInTime)
        val groupMembers = getGroupMembers(groupMembersList)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                safeEntryDao.insert(
                    SafeEntryRecord(
                        venue.venueName ?: "",
                        venue.venueId ?: "",
                        venue.tenantName ?: "",
                        venue.tenantId ?: "",
                        venue.postalCode ?: "",
                        venue.address ?: "",
                        checkInTimeInMs,
                        groupMembers,
                        groupMembersList.size + 1
                    )
                )
            }
            isRecordInserted.invoke(true)
        }
    }

    private fun getGroupMembers(familyMembersRecord: List<FamilyMembersRecord>): String? {
        var groupMembers: String? = null
        if (!familyMembersRecord.isNullOrEmpty()) {
            for (familyMember in familyMembersRecord) {
                groupMembers = if (groupMembers.isNullOrEmpty())
                    familyMember.nric
                else
                    groupMembers + "," + familyMember.nric
            }
        }
        return groupMembers
    }
}
