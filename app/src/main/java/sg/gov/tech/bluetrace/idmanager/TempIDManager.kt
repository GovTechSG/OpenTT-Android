package sg.gov.tech.bluetrace.idmanager

import android.content.Context
import android.os.Build
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.services.BluetoothMonitoringService.Companion.bmValidityCheck
import sg.gov.tech.revamp.responseModel.TempIdModel
import java.io.File
import java.util.*


object TempIDManager {
    private val tempIDKey = "tempIDs"
    private val shortIDKey = "shortTempIDs"
    private val mContext by lazy { TracerApp.AppContext }
    private const val TAG = "TempIDManager"


    fun storeTemporaryIDs(context: Context, packet: String) {
        CentralLog.d(TAG, "[TempID] Storing temporary IDs into internal storage...")
        val file = File(context.filesDir, tempIDKey)
        file.writeText(packet)
    }


    fun retrieveTemporaryID(context: Context): TemporaryID? {
        val file = File(context.filesDir, tempIDKey)
        if (file.exists()) {
            val readback = file.readText()

            if (!readback.isNullOrBlank()) {
//                CentralLog.d(TAG, "[TempID] fetched broadcastmessage from file:  $readback")
                var tempIDArrayList =
                    convertToTemporaryIDs(
                        readback
                    )
                var tempIDQueue =
                    tempIDArrayList?.let {
                        convertToQueue(
                            it
                        )
                    }
                return tempIDQueue?.let {
                    getValidOrLastTemporaryID(
                        context,
                        it
                    )
                }
            }
        } else{
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            AnalyticsUtils().exceptionEventAnalytics(
                AnalyticsKeys.TEMPID_ERROR,
                loggerTAG,
                "Error While Reading TempIds From File isNullOrBlank"
            )
        }
        return null
    }


    fun storeShortIDs(context: Context, packet: String) {
        CentralLog.d(TAG, "[shortIDs] Storing temporary shortIDs into internal storage...")
        val file = File(context.filesDir, shortIDKey)
        file.writeText(packet)
    }


    fun retrieveShortID(context: Context): TemporaryID? {
        val file = File(context.filesDir, shortIDKey)
        if (file.exists()) {
            val readback = file.readText()

            if (!readback.isNullOrBlank()) {
//                CentralLog.d(TAG, "[shortIDs] fetched broadcastmessage from file:  $readback")
                var tempIDArrayList =
                    convertToTemporaryIDs(
                        readback
                    )
                var tempIDQueue =
                    tempIDArrayList?.let {
                        convertToQueue(
                            it
                        )
                    }
                return tempIDQueue?.let {
                    getValidOrLastTemporaryID(
                        context,
                        it
                    )
                }
            }
        } else {
            val loggerTAG =
                "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
            AnalyticsUtils().exceptionEventAnalytics(
                AnalyticsKeys.SHORTID_ERROR,
                loggerTAG,
                "Error While Reading shortIDs From File isNullOrBlank"
            )
        }
        return null
    }


    private fun getValidOrLastTemporaryID(
        context: Context,
        tempIDQueue: Queue<TemporaryID>
    ): TemporaryID {
        CentralLog.d(TAG, "[TempID] Retrieving Temporary ID")
        var currentTime = System.currentTimeMillis()

        var pop = 0
        while (tempIDQueue.size > 1) {
            val tempID = tempIDQueue.peek()
            tempID.print()

            if (tempID.isValidForCurrentTime()) {
                CentralLog.d(TAG, "[TempID] Breaking out of the loop")
                break
            }

            tempIDQueue.poll()
            pop++
        }

        var foundTempID = tempIDQueue.peek()
        var foundTempIDStartTime = foundTempID.startTime * 1000
        var foundTempIDExpiryTime = foundTempID.expiryTime * 1000

        CentralLog.d(TAG, "[TempID Total number of items in queue: ${foundTempID.expiryTime}")
        CentralLog.d(TAG, "[TempID Total number of items in queue: ${foundTempID.startTime}")
        CentralLog.d(TAG, "[TempID Total number of items in queue: ${foundTempID.tempID}")
        CentralLog.d(TAG, "[TempID Total number of items in queue: ${tempIDQueue.size}")
        CentralLog.d(TAG, "[TempID Number of items popped from queue: $pop")
        CentralLog.d(TAG, "[TempID] Current time: ${currentTime}")
        CentralLog.d(TAG, "[TempID] Start time: ${foundTempIDStartTime}")
        CentralLog.d(TAG, "[TempID] Expiry time: ${foundTempIDExpiryTime}")
        CentralLog.d(TAG, "[TempID] Updating expiry time")
        Preference.putExpiryTimeInMillis(
            context,
            foundTempIDExpiryTime
        )
        return foundTempID
    }

    private fun convertToTemporaryIDs(tempIDString: String): Array<TemporaryID>? {
        try {
            val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
            val tempIDResult = gson.fromJson(tempIDString, Array<TemporaryID>::class.java)
            if (tempIDResult != null)
                CentralLog.d(
                    TAG,
                    "[TempID] After GSON conversion: ${tempIDResult[0].tempID} ${tempIDResult[0].startTime}"
                )
            else{
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                AnalyticsUtils().exceptionEventAnalytics(
                    AnalyticsKeys.TEMPID_ERROR,
                    loggerTAG,
                    "Invalid TempID Format From File"
                )
            }

            return tempIDResult
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            e.message?.let {
                AnalyticsUtils().exceptionEventAnalytics(
                    AnalyticsKeys.TEMPID_ERROR,
                    loggerTAG,
                    "Invalid TempID Format From File =>$it"
                )
            }
            return null
        }
    }

    private fun isValidTempIds(tempIds: Any?): Boolean {

        if (tempIds == null) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            AnalyticsUtils().exceptionEventAnalytics(
                AnalyticsKeys.TEMPID_ERROR,
                loggerTAG,
                "Invalid TempID format from server"
            )
            return false
        }
        try {
            val gsonMapBuilder = GsonBuilder()
            val gsonObject = gsonMapBuilder.create()
            var tempIdsObj = gsonObject.toJson(tempIds)
            gsonObject.fromJson(tempIdsObj, Array<TemporaryID>::class.java)
            return true
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            AnalyticsUtils().exceptionEventAnalytics(
                AnalyticsKeys.TEMPID_ERROR,
                loggerTAG,
                "Invalid TempID format from server =>${e.message}"
            )
            return false
        }
    }

    private fun convertToQueue(tempIDArray: Array<TemporaryID>): Queue<TemporaryID> {
        CentralLog.d(TAG, "[TempID] Before Sort: ${tempIDArray[0]}")

        //Sort based on start time
        tempIDArray.sortBy {
            return@sortBy it.startTime
        }
        CentralLog.d(TAG, "[TempID] After Sort: ${tempIDArray[0]}")

        //Preserving order of array which was sorted
        var tempIDQueue: Queue<TemporaryID> = LinkedList<TemporaryID>()
        for (tempID in tempIDArray) {
            tempIDQueue.offer(tempID)
        }

        CentralLog.d(TAG, "[TempID] Retrieving from Queue: ${tempIDQueue.peek()}")
        return tempIDQueue
    }

    fun getTemporaryIDs(context: Context, functions: FirebaseFunctions): Task<HttpsCallableResult> {
        val data: MutableMap<String, Any> = HashMap()
        data["ttId"] = Preference.getTtID(context)
        data["appVersion"] = Utils.getAppVersion(context)
        data["osVersion"] = Build.VERSION.RELEASE
        data["btLiteVersion"] = "2.0"
        data["os"] = "android"
        data["model"] = Utils.getDeviceName()
        return functions.getHttpsCallable("getTempIDsV3")
            .call(data).addOnSuccessListener {

                val result: HashMap<String, Any> = it.data as HashMap<String, Any>
                CentralLog.i(TAG,"Result from getTempID: $result")
                val tempIDs = result[tempIDKey]
                val shortIDs = result[shortIDKey]
                var isvalidTempIds = isValidTempIds(tempIDs)
                var isvalidShortId = isValidTempIds(shortIDs)
                CentralLog.i(TAG,"Result from tempIDs: $tempIDs")
                CentralLog.i(TAG,"Result from short tempIDs: $shortIDs")
                val status = result["status"].toString()
                if (status.toLowerCase()
                        .contentEquals("success") && isvalidTempIds && isvalidShortId
                ) {
                    CentralLog.w(TAG, "Retrieved Temporary IDs from Server")
//                    val modelMappingId = result["modelMappingId"]
//                    Preference.putMappingID(context, modelMappingId as Int)
                    val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
                    val jsonByteArray = gson.toJson(tempIDs).toByteArray(Charsets.UTF_8)
                    val jsonShortIdByteArray = gson.toJson(shortIDs).toByteArray(Charsets.UTF_8)
                    storeTemporaryIDs(
                        context,
                        jsonByteArray.toString(Charsets.UTF_8)
                    )
                    storeShortIDs(
                        context,
                        jsonShortIdByteArray.toString(Charsets.UTF_8)
                    )

                    val refreshTime = result["refreshTime"].toString()
                    var refresh = refreshTime.toLongOrNull() ?: 0
                    Preference.putNextFetchTimeInMillis(
                        context,
                        refresh * 1000
                    )
                    Preference.putLastFetchTimeInMillis(
                        context,
                        System.currentTimeMillis()
                    )
                } else
                    result["status"] = "failed"
            }.addOnFailureListener {
                CentralLog.d(TAG, "[TempID] Error getting Temporary IDs")
            }
    }

    fun needToUpdate(context: Context): Boolean {
        val nextFetchTime =
            Preference.getNextFetchTimeInMillis(context)
        val currentTime = System.currentTimeMillis()

        val update = currentTime >= nextFetchTime
        CentralLog.i(
            TAG,
            "Need to update and fetch TemporaryIDs? $nextFetchTime vs $currentTime: $update"
        )
        return update
    }


    fun needToRollNewTempID(context: Context): Boolean {
        val expiryTime =
            Preference.getExpiryTimeInMillis(context)
        val currentTime = System.currentTimeMillis()
        val update = currentTime >= expiryTime
        CentralLog.d(TAG, "[TempID] Need to get new TempID? $expiryTime vs $currentTime: $update")
        return update
    }

    //Can Cleanup, this function always return true
    fun bmValid(context: Context): Boolean {
        val expiryTime =
            Preference.getExpiryTimeInMillis(context)
        val currentTime = System.currentTimeMillis()
        val update = currentTime < expiryTime

        if (bmValidityCheck) {
            CentralLog.w(TAG, "Temp ID is valid")
            return update
        }

        return true
    }

    fun onTempIdResponse(result: ApiResponseModel<TempIdModel>): ApiResponseModel<TempIdModel> {
        var tempIdModel = result.result
        if (tempIdModel == null)
            return result

        CentralLog.i(TAG, "Result from getTempID: $result")
        val tempIDs = tempIdModel.tempIDs
        val shortIDs = tempIdModel.shortTempIDs
        var isvalidTempIds = isValidTempIds(tempIDs)
        var isvalidShortId = isValidTempIds(shortIDs)
        CentralLog.i(TAG, "Result from tempIDs: $tempIDs")
        CentralLog.i(TAG, "Result from short tempIDs: $shortIDs")
        if (result.isSuccess && isvalidTempIds && isvalidShortId) {
            CentralLog.w(TAG, "Retrieved Temporary IDs from Server")
            val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
            val jsonByteArray = gson.toJson(tempIDs).toByteArray(Charsets.UTF_8)
            val jsonShortIdByteArray = gson.toJson(shortIDs).toByteArray(Charsets.UTF_8)
            storeTemporaryIDs(
                mContext,
                jsonByteArray.toString(Charsets.UTF_8)
            )
            storeShortIDs(
                mContext,
                jsonShortIdByteArray.toString(Charsets.UTF_8)
            )
            val refreshTime = tempIdModel.refreshTime
            Preference.putNextFetchTimeInMillis(
                mContext,
                refreshTime * 1000
            )
            Preference.putLastFetchTimeInMillis(
                mContext,
                System.currentTimeMillis()
            )
        } else {
            result.isSuccess = false
        }
        return result
    }

    fun deleteTempIdFiles(context: Context) {
        val fileTempId = File(context.filesDir, tempIDKey)
        if (fileTempId.exists()) {
            fileTempId.deleteRecursively()
        }
        val fileShortId = File(context.filesDir, shortIDKey)
        if (fileShortId.exists()) {
            fileShortId.deleteRecursively()
        }
    }
}
