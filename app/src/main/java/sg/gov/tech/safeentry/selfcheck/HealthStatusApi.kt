package sg.gov.tech.safeentry.selfcheck

import androidx.lifecycle.MutableLiveData
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.safeentry.selfcheck.model.*
import java.util.*
import kotlin.collections.HashMap

class HealthStatusApi {

    companion object {

        private const val TAG = "HealthStatusApi"

        private val debugExposure: ExposureMock = ExposureMock.FROM_SERVER
        private val debugVaccination: VaccineMock = VaccineMock.FROM_SERVER

        enum class ExposureMock {
            FROM_SERVER, // fetch directly from server
            NOT_EXPOSED, // mock no exposure
            EXPOSED // add one exposure data
        }

        enum class VaccineMock {
            FROM_SERVER, // fetch from server
            NOT_VACCINATED,
            IN_PROGRESS,
            VACCINATED
        }

        val healthStatusApiStatus = MutableLiveData<HealthStatusApiData>()
        var inProgress = false

        fun fetchHealthStatus(
                userId:String,
                ttId: String,
                languageCode: String,
                functions: FirebaseFunctions
        ) {
            val loggerTAG = "${HealthStatusApi::class.java.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
            inProgress = true

            val data: MutableMap<String, Any> = HashMap()
            data["id"] = userId
            data["ttId"] = ttId
            data["languageCode"] = languageCode
            healthStatusApiStatus.postValue(HealthStatusApiData.loading())

            functions
                    .getHttpsCallable("getHealthStatus")
                    .call(data)
                    .addOnSuccessListener {
                        try {
                            val result = it.data as HashMap<String, Any>
                            val gson = Gson()

                            val resultString = gson.toJson(result)
                            val healthStatus = gson.fromJson(resultString, HealthStatus::class.java)

                            val cal = Calendar.getInstance()
                            Preference.setLastSERefreshTime(
                                TracerApp.AppContext,
                                cal.timeInMillis
                            )

                            try {
                                if (BuildConfig.DEBUG) {
                                    // add in the fake exposure data
                                    val stubbedResponse = createStubbedResponse(healthStatus)
                                    healthStatusApiStatus.postValue(HealthStatusApiData.done(stubbedResponse))
                                } else {
                                    healthStatusApiStatus.postValue(HealthStatusApiData.done(healthStatus))
                                }
                            } catch (e: Throwable) {
                                CentralLog.e(TAG, "Failed to process success: ${e.localizedMessage}")
                                DBLogger.e(DBLogger.LogType.HEALTHSTATUS, loggerTAG, "Failed to process success: ${e.localizedMessage}", DBLogger.getStackTraceInJSONArrayString(e as Exception))
                                healthStatusApiStatus.postValue(HealthStatusApiData.error(e))
                            }

                        } catch (e: Exception) {
                            CentralLog.e(TAG, "Error parsing result: ${e.stackTrace}")
                            DBLogger.e(
                                    DBLogger.LogType.HEALTHSTATUS,
                                    HealthStatusApi::class.java.simpleName,
                                    "Error parsing result: ${e.stackTrace}",
                                    DBLogger.getStackTraceInJSONArrayString(e)
                            )
                            healthStatusApiStatus.postValue(HealthStatusApiData.error(e))
                        }

                    }.addOnFailureListener { e ->
                        CentralLog.e(TAG, "getHealthStatus (failure): ${e.message}")
                        DBLogger.e(DBLogger.LogType.HEALTHSTATUS, loggerTAG, "getHealthStatus (failure): ${e.message}", DBLogger.getStackTraceInJSONArrayString(e))
                        healthStatusApiStatus.postValue(HealthStatusApiData.error(e))
                    }.addOnCompleteListener {
                        inProgress = false
                    }

        }

        private fun createStubbedResponse(original: HealthStatus): HealthStatus {
            val stubbedSelfCheckData = original.selfCheck.data.toMutableList()

            return HealthStatus(
                    original.status,
                    createStubbedSelfCheckData(stubbedSelfCheckData),
                    createStubbedVaccineData(original.vaccination)
            )
        }

        private fun createStubbedVaccineData(original: VaccinationInfo): VaccinationInfo {
            return when (debugVaccination) {
                VaccineMock.FROM_SERVER -> original
                VaccineMock.NOT_VACCINATED ->
                    VaccinationInfo(
                            isVaccinated = false,
                            iconText = "Not vaccinated",
                            header = "Not vaccinated",
                            subtext = "To complete the vaccination process, you need to take all doses and wait at least 14 days for the vaccine to take effect.",
                            urlText = "More info about COVID-19 vaccines",
                            urlLink = "https://www.vaccine.gov.sg/"
                    )
                VaccineMock.IN_PROGRESS ->
                    VaccinationInfo(
                            isVaccinated = false,
                            iconText = "Vaccination in progress",
                            header = "In progress",
                            subtext = "To complete the vaccination process, you need to take all doses and wait at least 14 days for the vaccine to take effect.",
                            urlText = "Login to HealthHub for more details",
                            urlLink = "https://eservices.healthhub.sg/covid/records"
                    )
                VaccineMock.VACCINATED ->
                    VaccinationInfo(
                            isVaccinated = true,
                            iconText = "Vaccinated",
                            header = "Vaccinated",
                            subtext = "Pfizer-BioNTech\nEffective from 05 Feb 2021",
                            urlText = "Login to HealthHub for more details",
                            urlLink = "https://eservices.healthhub.sg/covid/records"
                    )
            }
        }

        private fun createStubbedSelfCheckData(data: List<SafeEntryMatch>): SafeEntrySelfCheck {
            return when (debugExposure) {
                ExposureMock.FROM_SERVER -> SafeEntrySelfCheck(data.size, data)
                ExposureMock.NOT_EXPOSED -> SafeEntrySelfCheck(0, listOf())
                ExposureMock.EXPOSED -> {
                    val mutableSafeEntryMatch = data.toMutableList()

                    val cal1 = Calendar.getInstance()
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

                    mutableSafeEntryMatch.add(0,
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

                    SafeEntrySelfCheck(mutableSafeEntryMatch.size, mutableSafeEntryMatch)
                }
            }
        }

    }

}
