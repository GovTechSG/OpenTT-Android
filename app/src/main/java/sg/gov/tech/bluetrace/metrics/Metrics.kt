package sg.gov.tech.bluetrace.metrics

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*
import kotlin.collections.ArrayList


data class BTMetrics(
    var prevDayBTEncounterCount: Int,
    var lastBTEncounterTimestamp: Long
)

class Metrics(ctx: Context) {

    enum class LocationSetting(val value: Int) {
        OFF(4),
        ON(10)
    }

    enum class LocationStateSettings(val value: Int){
        OFF(0),
        ON(1)
    }

    enum class BluetoothStateSettings(val value: Int) {
        OFF(0),
        ON(1)
    }

    enum class PushNotificationSettings(val value: Int) {
        OFF(4),
        ON(10)
    }

    val platform = "android"
    val appVersion = BuildConfig.VERSION_NAME

    //time stamp in seconds. what's with this.
    val timestamp = System.currentTimeMillis() / 1000

    /*
    locationSettings = Refers to location permission
    locationStateSettings = Refers to location enabled or disabled

    bluetoothSettings = Refers to bluetooth permission
    bluetoothStateSettings = Refers to bluetooth enabled or disabled
     */

    var bluetoothStateSettings: Int
    var bluetoothSettings: Int

    var locationStateSettings: Int
    var locationSettings: Int

    var pushNotificationSettings: Int

    var prevDayBTEncounterCount: Int = -1
    var lastBTEncounterTimestamp: Long = -1

    val ttId: String = Preference.getTtID(ctx)

    @Transient
    val disposables: MutableList<Disposable> = ArrayList()

    @Transient
    val context: Context = ctx

    init {

        bluetoothStateSettings =
            if (isBluetoothEnabled(context)) {
                BluetoothStateSettings.ON.value
            } else {
                BluetoothStateSettings.OFF.value
            }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationSettings = LocationSetting.ON.value
            bluetoothSettings = LocationSetting.ON.value
        } else {
            locationSettings = LocationSetting.OFF.value
            bluetoothSettings = LocationSetting.OFF.value
        }

        locationStateSettings =
            if(isLocationEnabled(context))
                LocationStateSettings.ON.value
            else
                LocationStateSettings.OFF.value

        pushNotificationSettings =
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PushNotificationSettings.ON.value
            } else {
                PushNotificationSettings.OFF.value
            }
    }

    fun upload() {
        if (Preference.onBoardedWithIdentity(context) && FirebaseAuth.getInstance().currentUser != null)
            fetchDataFromDB(context)
    }

    private fun fetchDataFromDB(context: Context) {
        val db = StreetPassRecordDatabase.getDatabase(context)
        val recordDao = db.recordDao()

        val now = System.currentTimeMillis()
        val startOfYesterday = DateTools.getStartOfYesterday(now)
        val endOfYesterday = DateTools.getEndOfDay(startOfYesterday).timeInMillis

        var streetRecordMetrics = Observable.create<BTMetrics> {

            try {
                val lastRecord = recordDao.getLastRecord()
                val count = recordDao.countBTRecordsInRange(startOfYesterday, endOfYesterday)
                it.onNext(BTMetrics(count, lastRecord?.timestamp ?: -1))
                it.onComplete()
            } catch (e: Throwable) {
                it.onError(e)
            }
        }

        var stringMetrics = Observable.create<String> {
            it.onNext("")
            it.onComplete()
        }
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"

        disposables.add(
            Observable.zip(streetRecordMetrics, stringMetrics,
                BiFunction<BTMetrics, String, Unit> { btMetrics, strings ->
                    prevDayBTEncounterCount = btMetrics.prevDayBTEncounterCount
                    lastBTEncounterTimestamp = btMetrics.lastBTEncounterTimestamp / 1000
                }
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    val paramString = Gson().toJson(this, Metrics::class.java)
                    val params = JSONObject(paramString)
                    params.put("ttId", Preference.getTtID(context))
                    params.put("appVersion", Utils.getAppVersion(context))
                    FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
                        .getHttpsCallable("sendHeartbeat")
                        .call(params)
                        .addOnSuccessListener {
                            val result: HashMap<String, Any> = it.data as HashMap<String, Any>
                            CentralLog.d(
                                "Metrics",
                                "Metrics upload success: " + result.toString()
                            )
                            CentralLog.d("Metrics", paramString)
                        }
                        .addOnFailureListener { e: Throwable ->
                            CentralLog.e(
                                "Metrics",
                                "Metrics upload failed: ${e.message}"
                            )
                            DBLogger.e(
                                DBLogger.LogType.UPLOAD,
                                loggerTAG,
                                "Metrics upload failed: ${e.message}",
                                DBLogger.getStackTraceInJSONArrayString(e as Exception)
                            )
                            CentralLog.d(loggerTAG, paramString)
                            AnalyticsUtils().exceptionEventAnalytics(
                                AnalyticsKeys.METRICS_ERROR,
                                loggerTAG,
                                "Metrics upload failed: ${e.message}"
                            )

                        }
                        .addOnCompleteListener {
                            disposables.forEach {
                                it.dispose()
                            }
                            disposables.clear()
                        }
                }
                    , {
                        CentralLog.e(
                            loggerTAG,
                            "Metrics upload failed: ${it.message}"
                        )
                        DBLogger.e(
                            DBLogger.LogType.UPLOAD,
                            loggerTAG,
                            "Metrics upload failed: ${it.message}",
                            DBLogger.getStackTraceInJSONArrayString(it as Exception)
                        )
                        AnalyticsUtils().exceptionEventAnalytics(
                            AnalyticsKeys.METRICS_ERROR,
                            loggerTAG,
                            "Metrics upload failed: ${it.message}"
                        )

                    }
                )
        )

    }

    private fun isBluetoothEnabled(context: Context): Boolean {
        var btOn = false
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        bluetoothAdapter?.let {
            btOn = it.isEnabled
        }
        return btOn
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(loggerTAG, "Error checking if location is enabled: " + e.message)
            DBLogger.e(
                DBLogger.LogType.UPLOAD,
                loggerTAG,
                "Error checking if location is enabled: " + e.message,
                DBLogger.getStackTraceInJSONArrayString(e)
            )
        }

        return false
    }

}
