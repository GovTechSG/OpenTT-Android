package sg.gov.tech.bluetrace.revamp.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import kotlinx.coroutines.*
import sg.gov.tech.bluetrace.AnnouncementModel
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.RemoteConfigUtils
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryDao
import sg.gov.tech.bluetrace.streetpass.persistence.SafeEntryRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDao
import sg.gov.tech.bluetrace.utils.VersionChecker.isVersionGreaterOrEqual
import java.util.*
import java.util.concurrent.TimeUnit

class HomeViewModel(val recordDao: StreetPassRecordDao, val safeEntryDao: SafeEntryDao) :
    ViewModel() {

    companion object {
        //interval for fetching  Bluetooth exchanges and devices nearby in minutes
        private const val REFRESH_INTERVAL_FOR_EXCHANGES = 5

        //range interval for devices nearby range
        private const val DEVICES_NEARBY_RANGE_INTERVAL = 5

        //get the nearby devices since past time (in minutes)
        private const val DEVICES_NEARBY_SINCE_PAST_TIME = 5

        //interval to switch the display of Bluetooth exchanges and device nearby in Millisecond
        const val BT_DISPLAY_REFRESH_INTERVAL: Long = 7000L

        const val MORNING = 1
        const val NOON = 2
        const val EVENING = 3
        const val NIGHT = 4
    }

    private lateinit var btExchangesJob: Job

    // For displaying of Bluetooth Exchange & Device Nearby
    var btDisplayTimerHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var updateBtTextTask: Runnable
    var currentViewIsDeviceNearby: Boolean = false
    var timeLapsed: Long = -BT_DISPLAY_REFRESH_INTERVAL

    fun doBTExchangeJob(onComplete: (Int, Int) -> Unit) {
        btExchangesJob = viewModelScope.launch {
            withContext(Dispatchers.Main) {
                while (true) {
                    val btExchangeCount = getBtExchangeCount()
                    val btExchangeDevices = getBtExchangeDevices()
                    val timeInMillis =
                        TimeUnit.MINUTES.toMillis(REFRESH_INTERVAL_FOR_EXCHANGES.toLong())
                    onComplete.invoke(btExchangeCount, btExchangeDevices)
                    delay(timeInMillis)
                }
            }
        }
    }

    fun cancelBTExchangeJob() {
        btExchangesJob.cancel()
    }

    private suspend fun getBtExchangeCount(): Int {
        val now = System.currentTimeMillis()
        return withContext(Dispatchers.IO) {
            recordDao.liveCountRecordsInRange(
                DateTools.getStartOfDay(now).timeInMillis,
                DateTools.getEndOfDay(now).timeInMillis
            )
        }
    }

    private suspend fun getBtExchangeDevices(): Int {
        return withContext(Dispatchers.IO) {
            recordDao.countUniqueBTnBTLTempId(
                DateTools.getTimeMinutesAgo(DEVICES_NEARBY_SINCE_PAST_TIME),
                Calendar.getInstance().timeInMillis
            )
        }
    }

    fun createUpdateBtTextTask(onCycleUpdate: (Boolean, Long) -> Unit) {
        updateBtTextTask = object : Runnable {
            override fun run() {
                timeLapsed += BT_DISPLAY_REFRESH_INTERVAL
                onCycleUpdate.invoke(currentViewIsDeviceNearby, timeLapsed)
                currentViewIsDeviceNearby = !currentViewIsDeviceNearby

                btDisplayTimerHandler.postDelayed(this, BT_DISPLAY_REFRESH_INTERVAL)
            }
        }
    }

    fun setUpDisplayBtTask() {
        //Reset Time and boolean value
        timeLapsed = -BT_DISPLAY_REFRESH_INTERVAL
        currentViewIsDeviceNearby = false

        removeUpdateBTTextTask()
        btDisplayTimerHandler.post(updateBtTextTask)
    }

    fun removeUpdateBTTextTask() {
        btDisplayTimerHandler.removeCallbacks(updateBtTextTask)
    }

    fun getUnExitedEntryRecords(onComplete: (LiveData<List<SafeEntryRecord>>) -> Unit) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR, -24)
        val twentyFourHoursAgo = cal.timeInMillis
        viewModelScope.launch {
            val liveDataRecords: LiveData<List<SafeEntryRecord>> = withContext(Dispatchers.IO) {
                safeEntryDao.getUnexitedEntryRecords(twentyFourHoursAgo)
            }
            onComplete.invoke(liveDataRecords)
        }
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

    fun convertQrResultToSeEntryRecord(seRecord: SafeEntryRecord): QrResultDataModel {
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

    fun getTimeToPause(timeInHour: Double): Long {
        return System.currentTimeMillis() + getTimeInMilli(timeInHour)
    }

    fun getTimeInMilli(timeInHour: Double): Long {
        return (timeInHour * 60 * 60 * 1000).toLong()
    }

    fun getAnnouncementRemoteConfig(mContext: Context): String {
        val announcement = FirebaseRemoteConfig.getInstance().getString(RemoteConfigUtils.REMOTE_CONFIG_ANNOUNCEMENT)

        //Check if announcement from remote config is a default value
        return if (announcement == RemoteConfigUtils.getDefaultValue(mContext, RemoteConfigUtils.REMOTE_CONFIG_ANNOUNCEMENT))
            ""
        else
            announcement
    }

    fun getAnnouncementModel(announcementJson: String): AnnouncementModel {
        return Gson().fromJson(announcementJson, AnnouncementModel::class.java)
    }

    /**
     * checks whether to display the announcements for current app version or not
     */
    fun displayAnnouncementAppVersionCheck(currentAppVersion: String?,
        minAppVersion: String?,
        maxAppVersion: String?
    ): Boolean {
        return if (currentAppVersion.isNullOrBlank())
            false
        else if (!minAppVersion.isNullOrBlank() && !maxAppVersion.isNullOrBlank()) {
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

    fun getTimeSlot(): Int {
        return when (Calendar.getInstance()[Calendar.HOUR_OF_DAY]) {
            in 5..9 -> MORNING
            in 9..17 -> NOON
            in 17..20 -> EVENING
            else -> NIGHT
        }
    }

    fun getSlotBannerImg(mContext: Context, mode: Int): Drawable? {
        return when (mode) {
            MORNING ->
                ContextCompat.getDrawable(mContext, R.drawable.ic_morning)
            NOON ->
                ContextCompat.getDrawable(mContext, R.drawable.ic_noon)
            EVENING ->
                ContextCompat.getDrawable(mContext, R.drawable.ic_evening)
            NIGHT ->
                ContextCompat.getDrawable(mContext, R.drawable.ic_night)
            else ->
                null
        }
    }

    /*
    <1 min = ""
    1 min = "1 min ago"
    >1 min = "X mins ago"
     */
    /*fun getTimeLapsedInText(timeInMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        var timeLapsed = ""
        if (minutes == 1L)
            timeLapsed = "$minutes min ago"
        else if (minutes > 1L)
            timeLapsed = "$minutes mins ago"

        return timeLapsed
    }*/
    fun getTimeLapsedInText(timeInMillis: Long): String {
        return TimeUnit.MILLISECONDS.toMinutes(timeInMillis).toString()
    }

    fun getImageText(mContext: Context, str: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        builder.append(
            HtmlCompat.fromHtml(
                str,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        )
            .append(" ", ImageSpan(mContext, R.drawable.keyboard_backspace), 0)

        return builder
    }
}