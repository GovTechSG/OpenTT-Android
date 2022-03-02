package sg.gov.tech.bluetrace

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import io.reactivex.Single
import org.json.JSONArray
import org.json.JSONObject
import sg.gov.tech.bluetrace.bluetooth.gatt.*
import sg.gov.tech.bluetrace.idmanager.TempIDManager
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.qrscanner.QrResultDataModel
import sg.gov.tech.bluetrace.scheduler.Scheduler
import sg.gov.tech.bluetrace.services.BluetoothMonitoringService
import sg.gov.tech.bluetrace.services.BluetoothMonitoringService.Companion.PENDING_BM_UPDATE
import sg.gov.tech.bluetrace.services.BluetoothMonitoringService.Companion.PENDING_HEALTH_CHECK_CODE
import sg.gov.tech.bluetrace.services.BluetoothMonitoringService.Companion.PENDING_START
import sg.gov.tech.bluetrace.status.Status
import sg.gov.tech.bluetrace.streetpass.ACTION_DEVICE_SCANNED
import sg.gov.tech.bluetrace.streetpass.ConnectablePeripheral
import sg.gov.tech.bluetrace.streetpass.ConnectionRecord
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

object Utils {

    private const val TAG = "Utils"

    fun getRequiredPermissions(): Array<String> {
        return arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun getCameraPermissions(): Array<String> {
        return arrayOf(Manifest.permission.CAMERA)
    }

    fun getBatteryOptimizerExemptionIntent(packageName: String): Intent {
        val intent = Intent()
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        intent.data = Uri.parse("package:$packageName")
        return intent
    }

    fun canHandleIntent(batteryExemptionIntent: Intent, packageManager: PackageManager?): Boolean {
        packageManager?.let {
            return batteryExemptionIntent.resolveActivity(packageManager) != null
        }
        return false
    }

    fun getSafeEntryCheckInOutDateFromMs(msDate: Long): List<String> {
        val inputDateFormat = SimpleDateFormat(
            "dd-MMM-yyyy'T'HH:mm:ssZ",
            Locale.ENGLISH
        )

        val inputDate: String = inputDateFormat.format(Date(msDate))
        val dateFormat = "d MMM yyyy/h:mm a"
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        val res = formatter.format(inputDateFormat.parse(inputDate)!!)
        return res.split("/")
    }

    fun getSafeEntryCheckInOutDate(stringDate: String): List<String> {
        val inputDateFormat = SimpleDateFormat(
            "dd-MMM-yyyy'T'HH:mm:ssZ",
            Locale.ENGLISH
        )
        val dateFormat = "d MMM yyyy/h:mm a"
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        val res = formatter.format(inputDateFormat.parse(stringDate)!!)
        return res.split("/")
    }

    fun getDate(milliSeconds: Long): String {
        val dateFormat = "dd/MM/yyyy HH:mm:ss.SSS"
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun getTime(milliSeconds: Long): String {
        val dateFormat = "h:mm a"
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun getShortDate(milliSeconds: Long): String {
        val dateFormat = "E d MMM"
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun compareDate(dateMillis1: Long, dateMillis2: Long): Int {
        val cal1 = Calendar.getInstance()
        cal1.time = Date(dateMillis1)

        val cal2 = Calendar.getInstance()
        cal2.time = Date(dateMillis2)

        val yearDiff = cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR)
        if (yearDiff != 0) return yearDiff

        val monthDiff = cal1.get(Calendar.MONTH) - cal2.get(Calendar.MONTH)
        if (monthDiff != 0) return monthDiff

        return cal1.get(Calendar.DAY_OF_MONTH) - cal2.get(Calendar.DAY_OF_MONTH)
    }

    fun startBluetoothMonitoringService(context: Context) {
        if (Preference.onBoardedWithIdentity(context)) {
            val intent = Intent(context, BluetoothMonitoringService::class.java)
            intent.putExtra(
                BluetoothMonitoringService.COMMAND_KEY,
                BluetoothMonitoringService.Command.ACTION_START.index
            )
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun pauseBluetoothMonitoringService(context: Context, pauseUntil: Long) {
        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_USER_PAUSE.index
        )
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_ARGS,
            pauseUntil
        )
        ContextCompat.startForegroundService(context, intent)
    }

    fun helpCheckMonitoringService(context: Context) {
        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_SELF_CHECK.index
        )

        context.startService(intent)
    }


    fun scheduleStartMonitoringService(context: Context, timeInMillis: Long) {
        if (Preference.onBoardedWithIdentity(context)) {
            val intent = Intent(context, BluetoothMonitoringService::class.java)
            intent.putExtra(
                BluetoothMonitoringService.COMMAND_KEY,
                BluetoothMonitoringService.Command.ACTION_START.index
            )

            Scheduler.scheduleServiceIntent(
                PENDING_START,
                context,
                intent,
                timeInMillis
            )
        }
    }

    fun scheduleBMUpdateCheck(context: Context, bmCheckInterval: Long) {

        cancelBMUpdateCheck(context)

        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_UPDATE_BM.index
        )

        Scheduler.scheduleServiceIntent(
            PENDING_BM_UPDATE,
            context,
            intent,
            bmCheckInterval
        )
    }


    fun cancelBMUpdateCheck(context: Context) {
        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_UPDATE_BM.index
        )

        Scheduler.cancelServiceIntent(PENDING_BM_UPDATE, context, intent)
    }

    fun stopBluetoothMonitoringService(context: Context) {
        val intent = Intent(context, BluetoothMonitoringService::class.java)
        intent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_STOP.index
        )
        cancelNextHealthCheck(context)
        //context.stopService(intent)
        ContextCompat.startForegroundService(context, intent)
    }


    fun clearDataAndStopBTService(context: Context) {
        stopBluetoothMonitoringService(context)
        StreetPassRecordDatabase.clearDatabase(context)
        Preference.clearSharedPreferences(context)
        TempIDManager.deleteTempIdFiles(context)
    }

    fun scheduleNextHealthCheck(context: Context, timeInMillis: Long) {
        //cancels any outstanding check schedules.
        cancelNextHealthCheck(context)

        val nextIntent = Intent(context, BluetoothMonitoringService::class.java)
        nextIntent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_SELF_CHECK.index
        )
        //runs every XXX milliseconds - every minute?
        Scheduler.scheduleServiceIntent(
            PENDING_HEALTH_CHECK_CODE,
            context,
            nextIntent,
            timeInMillis
        )
    }

    fun cancelNextHealthCheck(context: Context) {
        val nextIntent = Intent(context, BluetoothMonitoringService::class.java)
        nextIntent.putExtra(
            BluetoothMonitoringService.COMMAND_KEY,
            BluetoothMonitoringService.Command.ACTION_SELF_CHECK.index
        )
        Scheduler.cancelServiceIntent(PENDING_HEALTH_CHECK_CODE, context, nextIntent)
    }


    fun broadcastDeviceScanned(
        context: Context,
        device: BluetoothDevice,
        connectableBleDevice: ConnectablePeripheral
    ) {
        val intent = Intent(ACTION_DEVICE_SCANNED)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        intent.putExtra(CONNECTION_DATA, connectableBleDevice)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun broadcastDeviceProcessed(context: Context, deviceAddress: String) {
        val intent = Intent(ACTION_DEVICE_PROCESSED)
        intent.putExtra(DEVICE_ADDRESS, deviceAddress)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun broadcastStreetPassReceived(context: Context, streetpass: ConnectionRecord) {
        val intent = Intent(ACTION_RECEIVED_STREETPASS)
        intent.putExtra(STREET_PASS, streetpass)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun broadcastStreetPassLiteReceived(context: Context, streetpass: ConnectionRecord) {
        val intent = Intent(ACTION_RECEIVED_STREETPASS_LITE)
        intent.putExtra(STREET_PASS, streetpass)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }


    fun broadcastStatusReceived(context: Context, statusRecord: Status) {
        val intent = Intent(ACTION_RECEIVED_STATUS)
        intent.putExtra(STATUS, statusRecord)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun broadcastDeviceDisconnected(context: Context, device: BluetoothDevice) {
        val intent = Intent(ACTION_GATT_DISCONNECTED)
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun readFromInternalStorage(context: Context, fileName: String): String {
        CentralLog.d(TAG, "Reading from internal storage")
        val fileInputStream: FileInputStream
        var text: String? = null
        val stringBuilder: StringBuilder = StringBuilder()
        fileInputStream = context.openFileInput(fileName)
        var inputStreamReader: InputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)
        try {
            while ({ text = bufferedReader.readLine(); text }() != null) {
                CentralLog.d(TAG, "Text: " + text)
                stringBuilder.append(text)
            }

            bufferedReader.close()

        } catch (e: Throwable) {
            CentralLog.e(TAG, "Failed to readFromInternalStorage: ${e.message}")
        }
        return stringBuilder.toString()
    }

    fun getDateFromUnix(unix_timestamp: Long): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH)
        val date = sdf.format(unix_timestamp)
        return date.toString()
    }

    fun hideKeyboardFrom(
        context: Context,
        view: View
    ) {
        val imm = context.getSystemService(
            Activity.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showKeyboardFrom(
        context: Context,
        view: View?
    ) {
        val imm = context.getSystemService(
            Activity.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    fun isBluetoothAvailable(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null &&
                bluetoothAdapter.isEnabled && bluetoothAdapter.state == BluetoothAdapter.STATE_ON
    }

    fun registerFCMToken(
        token: String,
        context: Context,
        functions: FirebaseFunctions
    ): Task<HttpsCallableResult> {
        val loggerTAG = "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
        val data: MutableMap<String, Any> = HashMap()
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        CentralLog.d(TAG, "[FCM] Registering FCM Token ${androidId}")
        data["token"] = token
        data["deviceId"] = androidId
        data["deviceOS"] = "android"
        data["ttId"] = Preference.getTtID(context)
        data["appVersion"] = getAppVersion(context)
        return functions
            .getHttpsCallable("registerFCMToken")
            .call(data)
            .addOnSuccessListener {
                val result: HashMap<String, Any> = it.data as HashMap<String, Any>
                CentralLog.d(TAG, "[FCM] RegisterFCMToken success: " + result.toString())
            }.addOnFailureListener { e ->
                CentralLog.e(TAG, "[FCM] RegisterFCMToken (failure): ${e.message}")
                DBLogger.e(
                    DBLogger.LogType.FIREBASE,
                    loggerTAG,
                    e.toString(),
                    DBLogger.getStackTraceInJSONArrayString(e)
                )
            }
    }

    fun startBluetoothMonitoringServiceViaIndex(context: Context, cmdIndex: Int) {
        if (Preference.onBoardedWithIdentity(context)) {
            CentralLog.e(TAG, "Starting command: $cmdIndex")
            val intent = Intent(context, BluetoothMonitoringService::class.java)
            intent.putExtra(
                BluetoothMonitoringService.COMMAND_KEY,
                cmdIndex
            )
            context.startService(intent)
        }
    }

    fun withComma(count: Int): String {

        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        val formatter = DecimalFormat("##,###,###", symbols)
        val formatted: String = formatter.format(count)
        return formatted
    }

    fun getAppVersion(context: Context): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }

    fun getCurrentAppVersionWithoutSuffix(context: Context): String {
        return context.packageManager.getPackageInfo(
            context.packageName,
            0
        ).versionName.split("-")[0]
    }

    fun dpToPx(activity: Context, valueInDp: Float): Int {
        val metrics = activity.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics).toInt()
    }

    fun isTaskCompletedWithSuccess(task: Task<HttpsCallableResult>): Boolean {
        try {
            return if (task.isSuccessful) {
                val result: HashMap<String, Any> = task.result?.data as HashMap<String, Any>
                val status = result["status"].toString()
                status.toLowerCase().contentEquals("success")
            } else {
                CentralLog.e(TAG, "Task is not successful: " + task.exception.toString())
                false
            }
        } catch (e: Exception) {
            CentralLog.e(TAG, "isTaskCompletedWithSuccess Error: " + e.message)
        }

        return false
    }

    fun getDeviceName(): String {
//        val manufacturer: String = Build.MANUFACTURER
//        val model: String = Build.MODEL
//        return "$manufacturer  $model"
        return Build.MODEL ?: ""
    }

    fun getShortDateWithComaAfterDay(msDate: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = msDate

        val dateFormat = "E, d MMM"
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        return formatter.format(calendar.time)
    }

    fun getDayAndHourWithComaAfterDay(msDate: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = msDate

        val dateFormat = "d MMM, h:mma"
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        return formatter.format(calendar.time)
    }

    fun getHourPmAm(msDate: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = msDate

        val dateFormat = "h:mma"
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        return formatter.format(calendar.time)
    }

    fun maskIdWithDot(id: String): String {
        return maskId(id, '⦁')
    }

    fun maskIdWithCross(id: String): String {
        return maskId(id, 'X')
    }

    private fun maskId(id: String, maskedChar: Char): String {
        var range = 5
        if (id.length < 7) {
            range = 2
        }
        var strArray = id.toCharArray()
        for (i in 1..strArray.size - range) {
            strArray[i] = maskedChar
        }

        return String(strArray)
    }

    fun doesHealthStatusNeedRefresh(): Boolean {
        var last = Preference.getLastSERefreshTime(TracerApp.AppContext)
        if (last > 0) {
            var now = System.currentTimeMillis()
            var diff = now - last
            var diffHours = diff / (60 * 60 * 1000)
            return diffHours >= 2
        }
        return true
    }

    fun redirectToPlayStore(context: Context) {
        if (isGooglePlayStoreInstalled(context)) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(BuildConfig.PLAY_STORE_URL)
                )
            )
        } else if (isHuaweiAppGalleryInstalled(context)) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(BuildConfig.HUAWEI_APP_GALLERY_URL)
                )
            )
        }
    }

    private fun isGooglePlayStoreInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, 0
            )
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isHuaweiAppGalleryInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.huawei.appmarket", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
