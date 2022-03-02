package sg.gov.tech.bluetrace.permissions

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pub.devrel.easypermissions.EasyPermissions
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Check the feature.
 * @param act AppCompatActivity instance.
 * @param features varargs of REQUEST_ENABLE_BLUETOOTH, REQUEST_ACCESS_LOCATION, REQUEST_BATTERY_OPTIMISER
 * If features are not provided, none will be checked.
 */
class FeatureChecker(private val act: AppCompatActivity, vararg features: Int) {

    companion object {
        private const val TAG = "FeatureChecker"
        const val REQUEST_ENABLE_BLUETOOTH = 123
        const val REQUEST_ACCESS_LOCATION = 456
        const val REQUEST_IGNORE_BATTERY_OPTIMISER = 789
        const val REQUEST_APP_SETTINGS = 200
        const val REQUEST_CAMERA_PERMISSION = 654
    }

    private var onDoneListener: ((Boolean) -> Unit)? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = act.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }

    private val locationManager: LocationManager? by lazy {
        val locationManager = act.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        locationManager
    }

    private val powerManager: PowerManager? by lazy {
        val powerManager = act.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        powerManager
    }

    private var uniqueID: String? = null

    private val isDirty = AtomicBoolean(false)

    private var locationRequired = false
    private var bluetoothRequired = false
    private var batteryRequired = false

    init {
        setFeature(*features)
    }

    private var locationChecked = locationRequired
    private var bluetoothChecked = bluetoothRequired
    private var batteryChecked = batteryRequired

    /**
     * Set the features to be checked.
     * @param features: varargs of the feature: REQUEST_ENABLE_BLUETOOTH, REQUEST_ACCESS_LOCATION, REQUEST_BATTERY_OPTIMISER
     * Empty params means all not required.
     */
    fun setFeature(vararg features: Int) {
        locationRequired = false
        bluetoothRequired = false
        batteryRequired = false

        for (f in features) {
            if (f == REQUEST_ACCESS_LOCATION) {
                locationRequired = true
            }
            if (f == REQUEST_ENABLE_BLUETOOTH) {
                bluetoothRequired = true
            }
            if (f == REQUEST_IGNORE_BATTERY_OPTIMISER) {
                batteryRequired = true
            }
        }
    }

    /**
     * Check the functions of the needed features.
     */
    fun check(): Array<Boolean> {

        // location
        val locationChecked = if (locationRequired) {
            val permissions = Utils.getRequiredPermissions()
            EasyPermissions.hasPermissions(act, *permissions)
        } else {
            true
        }

        // bluetooth
        val bluetoothChecked = if (bluetoothRequired) {
            bluetoothAdapter?.isEnabled ?: false
        } else true

        // power
        // if < M don't check
        val batteryChecked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (batteryRequired) {
                powerManager?.isIgnoringBatteryOptimizations(act.packageName) ?: false
            } else true
        } else true
        return arrayOf(locationChecked, bluetoothChecked, batteryChecked)
    }

    /**
     * Public function to check features and return its id also.
     * The id is to prevent multiple call to enable function since this will be called from onStart.
     */
    fun checkFeatures(onDone: (Boolean) -> Unit): CheckResult? {
        if (uniqueID != null) return null

        onDoneListener = onDone

        val checks = check()
        locationChecked = checks[0]
        bluetoothChecked = checks[1]
        batteryChecked = checks[2]

        CentralLog.d(
            TAG,
            "features -- [loc, bt, opt]: $locationChecked, $bluetoothChecked, $batteryChecked"
        )

        val id = UUID.randomUUID().toString()
        uniqueID = id
        return CheckResult(id, checks)
    }

    //region turn on features
    private fun turnOnLocation() {
        val permissions = Utils.getRequiredPermissions()
        when {
            ContextCompat.checkSelfPermission(
                act,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                locationChecked = true
                enableFeatures(uniqueID)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                act,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // Do not show rationale to turn on permission dialog. Just request for permission.
                locationChecked = true
                ActivityCompat.requestPermissions(act, permissions, REQUEST_ACCESS_LOCATION)
            }
            else -> {
                ActivityCompat.requestPermissions(act, permissions, REQUEST_ACCESS_LOCATION)
            }
        }
    }

    private fun turnOnBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        act.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
        bluetoothChecked = true
    }

    private fun turnOnIgnoreBatteryOptimization() {
        val intent = Utils.getBatteryOptimizerExemptionIntent(act.packageName)
        if (Utils.canHandleIntent(intent, act.packageManager)) {
            act.startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMISER)
        }
        batteryChecked = true
    }
    //endregion

    /**
     * This need to be called from the activity onRequestPermissionsResult
     */
    fun permissionCallback(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && ((grantResults[0] == PackageManager.PERMISSION_GRANTED) || locationChecked)) {
                enableFeatures(uniqueID)
            } else {
                locationChecked = true
                // this is checking if user has chosen "don't ask again"
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        act,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    AlertDialog.Builder(act)
                        .setMessage(R.string.requires_location_permission_setting)
                        .setPositiveButton(R.string.go_to_settings) { _, _ ->
                            val myAppSettings = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + act.packageName)
                            )
                            myAppSettings.addCategory(Intent.CATEGORY_DEFAULT)
                            myAppSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            act.startActivityForResult(myAppSettings, REQUEST_APP_SETTINGS)
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            enableFeatures(uniqueID)
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    enableFeatures(uniqueID)
                }
            }
        }
    }

    /**
     * Call this to enable features with the boolean of the features.
     * @param id is created when caller call checkFeatures()
     */
    fun enableFeatures(id: String?) {
        if (uniqueID != null && uniqueID != id) {
            return
        }

        if (isDirty.compareAndSet(true, false)) {
            refresh()
        }

        CentralLog.d(
            TAG,
            "features [loc, bt, opt]: $locationChecked, $bluetoothChecked, $batteryChecked"
        )

        if (locationRequired && !locationChecked) {
            CentralLog.d(TAG, "Turning on Location")
            turnOnLocation()
            return
        }

        if (bluetoothRequired && !bluetoothChecked) {
            CentralLog.d(TAG, "Turning on Bluetooth")
            turnOnBluetooth()
            return
        }

        if (batteryRequired && !batteryChecked) {
            CentralLog.d(TAG, "Turning on Ignore Battery optimization")
            turnOnIgnoreBatteryOptimization()
            return
        }

        // check again if the features all turned on
        val checks = refresh()

        // Check if BLE multiple advertisement is supported.
        if (checks[1]) {
            if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported) {
                Utils.stopBluetoothMonitoringService(act)
            }
        }

        val c = checks.reduce { acc, b -> acc && b }
        onDoneListener?.invoke(c)
        CentralLog.d(TAG, "Done checking")
    }

    fun setDirty() {
        isDirty.set(true)
    }

    private fun refresh(): Array<Boolean> {
        val checks = check()
        locationChecked = checks[0]
        bluetoothChecked = checks[1]
        batteryChecked = checks[2]
        return checks
    }

    fun clear() {
        locationChecked = locationRequired
        bluetoothChecked = bluetoothRequired
        batteryChecked = batteryRequired
        uniqueID = null
    }

    inner class CheckResult(val id: String, val checks: Array<Boolean>)
}
