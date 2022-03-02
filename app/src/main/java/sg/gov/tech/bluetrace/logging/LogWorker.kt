package sg.gov.tech.bluetrace.logging

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.BatteryManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import pub.devrel.easypermissions.EasyPermissions
import sg.gov.tech.bluetrace.Utils

class LogWorker(val context: Context, workParams: WorkerParameters) : Worker(context, workParams) {

    private val TAG = LogWorker::class.java.simpleName

    override fun doWork(): Result {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val logData =
            "isBluetoothEnabled:" + isBluetoothEnabled(context) + ", isLocationEnabled:" + isLocationEnabled(
                context) + ", hasLocationPermissions:" + hasLocationPermissions(context) + ", BatteryPercent:" + getBatteryPercent(
                context)
        DBLogger.i(DBLogger.LogType.SETTINGS, loggerTAG, logData)

        return Result.success()
    }

    private fun isBluetoothEnabled(context: Context): Boolean {
        var isBluetoothEnabled = false
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }
        bluetoothAdapter?.let {
            isBluetoothEnabled = it.isEnabled
        }
        return isBluetoothEnabled
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.SETTINGS,
                loggerTAG, "Error while checking Location Settings", e.message
            )
        }
        return false
    }

    private fun hasLocationPermissions(context: Context): Boolean {
        val permissions = Utils.getRequiredPermissions()
        return EasyPermissions.hasPermissions(context, *permissions)
    }

    private fun getBatteryPercent(context: Context): Float {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        return batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        } ?: 0.0f
    }
}