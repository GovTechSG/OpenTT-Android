package sg.gov.tech.bluetrace.revamp.safeentry

import android.content.Context
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
import sg.gov.tech.bluetrace.revamp.requestModel.CheckOutRequestModel
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryDao
import sg.gov.tech.bluetrace.utils.TTDatabaseCryptoManager

class SafeEntryCheckOutViewModel(
    private val api: ApiHandler,
    private val favouriteDao: FavouriteDao,
    val safeEntryDao: SafeEntryDao
) : ViewModel() {


    private val disposables = CompositeDisposable()
    private val TAG = "SafeEntryCheckOutViewModel"
    var isFav: MutableLiveData<Boolean> = MutableLiveData()
    var checkOutResponseData: MutableLiveData<ApiResponseModel<out Any>> = MutableLiveData()

    fun isVenueFavorite(venue: QrResultDataModel) {
        viewModelScope.launch {
            val favouriteRecord: FavouriteRecord? = withContext(Dispatchers.IO) {
                favouriteDao.getFavouriteRecordById(venue.venueId, venue.tenantId)
            }
            isFav.value = (favouriteRecord != null)
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

    fun updateSeRecordInDB(
        checkOutTime: String,
        venue: QrResultDataModel,
        onRemoved: (Boolean) -> Unit
    ) {
        val checkOutTimeInMs = DateTools.convertCheckInOutTimeToMs(checkOutTime)
        val venueID = venue.id

        venueID?.let {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    safeEntryDao.exitVenue(venueID, checkOutTimeInMs)
                }
                onRemoved.invoke(true)
            }
        }
    }

    fun postSEEntryCheckOut(user: RegisterUserData,
                            groupIds: ArrayList<String>,
                            venue: QrResultDataModel) {

        val requestModel: CheckOutRequestModel
        if (groupIds.isNullOrEmpty()) {
            requestModel = CheckOutRequestModel(
                venue.venueId,
                venue.tenantId,
                venue.venueName,
                user.id
            )
        } else {
            val groupIdList = mutableListOf<CheckOutRequestModel.CheckInOutGroupIds>()
            groupIds.forEach { id ->
                val obj = CheckOutRequestModel.CheckInOutGroupIds(id)
                groupIdList.add(obj)
            }
            requestModel = CheckOutRequestModel.CheckOutRequestWithGroupMember(
                venue.venueId,
                venue.tenantId,
                venue.venueName,
                user.id,
                groupIdList
            )
        }
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val result = api.checkOutUser(requestModel)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                CentralLog.e(loggerTAG, "Check-out fail: ${e.message}")
                DBLogger.e(
                    DBLogger.LogType.SAFEENTRY,
                    loggerTAG,
                    "Check-out fail: ${e.message}",
                    null
                )
                checkOutResponseData.postValue(ApiResponseModel(false, e.message))
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                CentralLog.e(TAG, "Check-out success")
                checkOutResponseData.postValue(data as ApiResponseModel<out Any>?)
            }

        })
        disposables.addAll(disposable)
    }

    fun clearCheckOutResponseLiveData() {
        checkOutResponseData = MutableLiveData()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.dispose()
    }

    private fun splitGroupIds(encryptedIds: String): List<String> {
        return encryptedIds.split(",")
    }

    fun getDecryptedGroupIds(context: Context, encryptedIds: String): ArrayList<String> {
        var familyMembersList: List<String> = splitGroupIds(encryptedIds)
        return familyMembersList.map {
            TTDatabaseCryptoManager.getDecryptedFamilyMemberNRIC(context, it)
        } as ArrayList<String>
    }
}