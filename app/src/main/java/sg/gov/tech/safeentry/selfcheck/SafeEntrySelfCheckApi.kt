package sg.gov.tech.safeentry.selfcheck

import android.os.Build
import androidx.lifecycle.MutableLiveData
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.safeentry.selfcheck.model.*
import java.util.*
import kotlin.collections.HashMap

class SafeEntrySelfCheckApi {

    companion object {

        private const val TAG = "SafeEntrySelfCheckApi"
        var mSeApiStatus = MutableLiveData<SEApiData>()

        fun postToSEApiData(count: Int, data: List<SafeEntryMatch>) {
            val loggerTAG = "${SafeEntrySelfCheckApi::class.java.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"

            try {
                if (BuildConfig.DEBUG) {
                    // add in the fake exposure data
                    val stubbedResponse = SafeEntrySelfCheck(count + 1, createStubbedResponse(data))
                    mSeApiStatus.postValue(SEApiData.done(stubbedResponse))
                } else {
                    val response = SafeEntrySelfCheck(count, data)
                    mSeApiStatus.postValue(SEApiData.done(response))
                }
            } catch (e: Throwable) {
                CentralLog.e(TAG, "Failed to process success: ${e.localizedMessage}")
                DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "Failed to process success: ${e.localizedMessage}", DBLogger.getStackTraceInJSONArrayString(e as Exception))
                mSeApiStatus.postValue(SEApiData.error(e))
            }

        }

        private fun createStubbedResponse(data: List<SafeEntryMatch>): List<SafeEntryMatch> {
            val stubbedSelfCheckData = data.toMutableList()

            val cal1 = Calendar.getInstance()
            Preference.setLastSERefreshTime(
                    TracerApp.AppContext,
                    cal1.timeInMillis
            )
            cal1.add(Calendar.DAY_OF_YEAR, -2)
            cal1.set(Calendar.HOUR_OF_DAY, 12)
            cal1.set(Calendar.MINUTE, 25)
            cal1.set(Calendar.MILLISECOND, 983)

            val cal2 = Calendar.getInstance()
            cal2.add(Calendar.DAY_OF_YEAR, -2)
            cal2.set(Calendar.HOUR_OF_DAY, 15)
            cal2.set(Calendar.MINUTE, 43)
            cal2.set(Calendar.MILLISECOND, 782)

            val checkInTime = cal1.timeInMillis / 1000
            val checkOutTime = cal2.timeInMillis / 1000

            cal1.add(Calendar.MINUTE, 20)
            cal2.add(Calendar.MINUTE, -20)

            val hotspotStartTime = cal1.timeInMillis / 1000
            val hotspotEndTime = cal2.timeInMillis / 100

            stubbedSelfCheckData.add(
                    SafeEntryMatch(
                            SafeEntryInfo(
                                    CheckInInfo(
                                            "stub-ci",
                                            checkInTime,
                                            "stub"
                                    ),
                                    MatchLocation(
                                            "no address",
                                            "no postal code",
                                            "no description"
                                    ),
                                    CheckoutInfo(
                                            "stub-co",
                                            checkOutTime,
                                            "stub"
                                    )
                            ),
                            arrayListOf(
                                    HotSpot(
                                            TimeWindow(
                                                    hotspotStartTime,
                                                    hotspotEndTime
                                            ),
                                            MatchLocation(
                                                    "no address",
                                                    "no postal code",
                                                    "no description"
                                            ),
                                            "some id thing"
                                    )
                            ),
                            "some fake fin here. huh. we don't verify"
                    )
            )

            return stubbedSelfCheckData
        }

        // keeping the function for reference until safe to delete?
        fun fetchSafeEntrySelfCheck(
                ttId: String,
                nric: String,
                functions: FirebaseFunctions
        ) {
            val loggerTAG = "${SafeEntrySelfCheckApi::class.java.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
//            inProgress = true

            val data: MutableMap<String, Any> = HashMap()
            data["ttId"] = ttId
            data["nric"] = nric
            data["appVersion"] = Utils.getAppVersion(TracerApp.AppContext)
            data["model"] = Utils.getDeviceName()
            data["osVersion"] = Build.VERSION.RELEASE
            data["os"] = "android"

            mSeApiStatus.postValue(SEApiData.loading())

            functions
                    .getHttpsCallable("getSESelfCheck")
                    .call(data)
                    .addOnSuccessListener {

                        try {
                            val result: HashMap<String, Any> = it.data as HashMap<String, Any>
                            val gson = Gson()
                            val dataField = result["data"]
                            val count: Int = (result["count"] as? Int?) ?: 0
                            CentralLog.w(TAG, "dataField: ${dataField}")
                            dataField?.let {
                                val dataJsonString = gson.toJson(it)
                                val tempIDResult =
                                        gson.fromJson(dataJsonString, Array<SafeEntryMatch>::class.java)

                                try {
                                    val response = SafeEntrySelfCheck(count, tempIDResult.toList())
                                    val cal1 = Calendar.getInstance()
                                    Preference.setLastSERefreshTime(
                                            TracerApp.AppContext,
                                            cal1.timeInMillis
                                    )
                                    cal1.add(Calendar.DAY_OF_YEAR, -2)
                                    cal1.set(Calendar.HOUR_OF_DAY, 12)
                                    cal1.set(Calendar.MINUTE, 25)
                                    cal1.set(Calendar.MILLISECOND, 983)

                                    val checkInTime = cal1.timeInMillis / 1000

                                    val cal2 = Calendar.getInstance()
                                    cal2.add(Calendar.DAY_OF_YEAR, -2)
                                    cal2.set(Calendar.HOUR_OF_DAY, 15)
                                    cal2.set(Calendar.MINUTE, 43)
                                    cal2.set(Calendar.MILLISECOND, 782)

                                    val checkOutTime = cal2.timeInMillis / 1000


                                    val safeEntryInfo = SafeEntryInfo(

                                            CheckInInfo(
                                                    "stub-ci",
                                                    checkInTime,
                                                    "stub"
                                            ),

                                            MatchLocation(
                                                    "no address",
                                                    "no postal code",
                                                    "no description"
                                            ),

                                            CheckoutInfo(
                                                    "stub-co",
                                                    checkOutTime,
                                                    "stub"
                                            )

                                    )


                                    cal1.add(Calendar.MINUTE, 20)
                                    cal2.add(Calendar.MINUTE, -20)

                                    val hotspots: ArrayList<HotSpot> = arrayListOf(
                                            HotSpot(
                                                    TimeWindow(
                                                            cal1.timeInMillis / 1000,
                                                            cal2.timeInMillis / 1000
                                                    ),
                                                    MatchLocation(
                                                            "no address",
                                                            "no postal code",
                                                            "no description"
                                                    ),
                                                    "some id thing"
                                            )
                                    )

                                    val oriSafeEntryMatchList = response.data.toMutableList()
                                    oriSafeEntryMatchList.add(SafeEntryMatch(
                                            safeEntryInfo,
                                            hotspots,
                                            "some fake fin here. huh. we don't verify."
                                    ))

                                    val stubbedResponse = SafeEntrySelfCheck(
                                            response.count + 1,
                                            oriSafeEntryMatchList
                                    )

                                    if (BuildConfig.DEBUG) {
                                        mSeApiStatus.postValue(SEApiData.done(stubbedResponse))
                                    } else {
                                        mSeApiStatus.postValue(SEApiData.done(response))
                                    }

                                } catch (e: Throwable) {
                                    CentralLog.e(
                                            TAG,
                                            "Failed to process success: ${e.localizedMessage}"
                                    )

                                    DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "Failed to process success: ${e.localizedMessage}", DBLogger.getStackTraceInJSONArrayString(e as Exception))
                                    mSeApiStatus.postValue(SEApiData.error(e))
                                }

                            }
                        } catch (e: Exception) {
                            CentralLog.e(TAG, "Error parsing result: ${e.stackTrace}")
                            DBLogger.e(
                                    DBLogger.LogType.SAFEENTRY,
                                    SafeEntrySelfCheckApi::class.java.simpleName,
                                    "Error parsing result: ${e.stackTrace}",
                                    DBLogger.getStackTraceInJSONArrayString(e)
                            )
                            mSeApiStatus.postValue(SEApiData.error(e))
                        }

                    }.addOnFailureListener { e ->
                        CentralLog.e(TAG, "getSESelfCheck (failure): ${e.message}")
                        DBLogger.e(DBLogger.LogType.SAFEENTRY, loggerTAG, "getSESelfCheck (failure): ${e.message}", DBLogger.getStackTraceInJSONArrayString(e))
                        mSeApiStatus.postValue(SEApiData.error(e))
                    }
                    .addOnCompleteListener {
//                        inProgress = false
                    }
        }

    }

}
