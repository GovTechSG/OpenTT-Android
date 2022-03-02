package sg.gov.tech.bluetrace.favourite

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase

class FavouriteViewModel : ViewModel() {

    var favouriteRecordList: MutableLiveData<List<FavouriteRecord>> = MutableLiveData()
    var isDeleted: MutableLiveData<Boolean> = MutableLiveData()
    var isAdded: MutableLiveData<Boolean> = MutableLiveData()

    fun getAllRecords(context: Context): List<FavouriteRecord> {
        return StreetPassRecordDatabase.getDatabase(context).favouriteDao().getAllRecords()
    }

    suspend fun insertRecord(context: Context, safeEntryRecord: SafeEntryRecord): FavouriteRecord {
        val favouriteRecord = FavouriteRecord(
            safeEntryRecord.venueId,
            safeEntryRecord.venueName,
            safeEntryRecord.tenantId,
            safeEntryRecord.tenantName,
            safeEntryRecord.postalCode,
            safeEntryRecord.address
        )
        return insertRecord(context, favouriteRecord)
    }

    suspend fun insertRecord(context: Context, favouriteRecord: FavouriteRecord): FavouriteRecord {
        StreetPassRecordDatabase.getDatabase(context).favouriteDao().insert(favouriteRecord)
        return favouriteRecord
    }

    fun deleteRecord(context: Context, venueId: String?, tenantId: String?) {
        return StreetPassRecordDatabase.getDatabase(context).favouriteDao()
            .deleteRecord(venueId, tenantId)
    }

    fun getFavouriteRecordById(
        context: Context,
        venueId: String?,
        tenantId: String?
    ): FavouriteRecord? {
        return StreetPassRecordDatabase.getDatabase(context).favouriteDao()
            .getFavouriteRecordById(venueId, tenantId)
    }

    fun showSnackBar(context: Context, view: View, text: Int) {
        val snackBar = Snackbar.make(view, text, Snackbar.LENGTH_LONG)
        snackBar.view.setBackgroundColor(ContextCompat.getColor(context, R.color.green_bg))
        val snackBarTextView =
            snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        snackBarTextView.setTypeface(snackBarTextView.typeface, Typeface.BOLD)
        snackBar.setTextColor(ContextCompat.getColor(context, R.color.grey_1))
        val snackBarActionView =
            snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        snackBarActionView.setTypeface(snackBarActionView.typeface, Typeface.BOLD)
        snackBar.setActionTextColor(ContextCompat.getColor(context, R.color.normal_text))
        snackBar.setAction(R.string.close_uppercase) {
            // Responds to click
            snackBar.dismiss()
        }.show()
    }

    fun getTermsFavText(
        context: Context,
        declarationString: String,
        termsString: String,
        text: CharSequence
    ): SpannableString {
        val spannable = SpannableString(text)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.blue_text)),
            declarationString.indexOf(termsString),
            declarationString.indexOf(termsString) + termsString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    fun getTermsFavLink(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(BuildConfig.SE_TERMS_URL)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(loggerTAG, e.message.toString())
            DBLogger.e(
                DBLogger.LogType.SAFEENTRY,
                loggerTAG, e.message.toString(),
                DBLogger.getStackTraceInJSONArrayString(e)
            )
        }
    }

    fun getFavouriteRecords(context: Context) {
        viewModelScope.launch {
            val favouriteRecords: List<FavouriteRecord> = withContext(Dispatchers.IO) {
                getAllRecords(context)
            }
            favouriteRecordList.value = favouriteRecords
        }
    }

    fun deleteFavRecord(context: Context, venueId: String?, tenantId: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                deleteRecord(context, venueId, tenantId)
            }
            isDeleted.value = true
        }
    }

    fun insertFavRecord(context: Context, venue: QrResultDataModel, view: View) {
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
                insertRecord(context, favouriteRecord)
                showSnackBar(context, view, R.string.saved_to_favourites)
                AnalyticsUtils().trackEvent(
                    AnalyticsKeys.SCREEN_NAME_VIEW_PASS,
                    AnalyticsKeys.SE_TAP_FAVOURITE,
                    AnalyticsKeys.TRUE
                )
            }
        }
    }

    fun isFavAdded(context: Context, venue: QrResultDataModel) {
        viewModelScope.launch {
            val favouriteRecord: FavouriteRecord? = withContext(Dispatchers.IO) {
                getFavouriteRecordById(
                    context,
                    venue.venueId,
                    venue.tenantId
                )
            }
            isAdded.value = favouriteRecord != null
        }
    }
}

class SortFavouriteListAlphabetically {

    companion object : Comparator<FavouriteRecord> {
        private fun getTitle(record: FavouriteRecord): String {
            return if (record.tenantName.isEmpty()) {
                record.venueName
            } else {
                record.tenantName
            }
        }

        override fun compare(a: FavouriteRecord, b: FavouriteRecord): Int = when {
            getTitle(a) > getTitle(b) -> 1
            getTitle(a) < getTitle(b) -> -1
            else -> 0
        }
    }
}
