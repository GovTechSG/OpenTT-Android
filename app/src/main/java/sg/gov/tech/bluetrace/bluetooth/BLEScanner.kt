package sg.gov.tech.bluetrace.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import kotlin.properties.Delegates

class BLEScanner constructor(
    context: Context,
    val filters: ArrayList<ScanFilter>,
    reportDelay: Long
) {

    private var context: Context by Delegates.notNull()
    private var scanCallback: ScanCallback? = null
    private var reportDelay: Long by Delegates.notNull()

    private var scanner: BluetoothLeScanner? =
        BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

    private val TAG = "BLEScanner"

    init {
        this.context = context
        this.reportDelay = reportDelay
    }

    fun startScan(scanCallback: ScanCallback) {

        val adapter = BluetoothAdapter.getDefaultAdapter()
        val supported = adapter.isOffloadedScanBatchingSupported

        if(!supported){
            reportDelay = 0
        }
        CentralLog.d(TAG, "Batch scanning supported? : $supported, using: $reportDelay")

        val settings = ScanSettings.Builder()
            .setReportDelay(reportDelay)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        this.scanCallback = scanCallback
        //try to get a scanner if there isn't anything
        scanner = scanner ?: BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

        //Try catch to prevent a NullPointer error in a class (ScanManager.java) that try to do registerScanner with a null UUID value (Not sure why it occur)
        try {
            scanner?.startScan(filters, settings, scanCallback)
        }
        catch (e: Throwable) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(loggerTAG, "scanner?.startScan() error: ${e.message}")
            DBLogger.e(
                DBLogger.LogType.BLUETRACE,
                loggerTAG,
                "scanner?.startScan() error: ${e.message}",
                DBLogger.getStackTraceInJSONArrayString(e as Exception)
            )
        }
    }

    fun flush() {
        if (isBluetoothAvailable()) {
            scanCallback?.let {
                scanner?.flushPendingScanResults(scanCallback)
            }
        }
    }

    fun isBluetoothAvailable(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null &&
                bluetoothAdapter.isEnabled && bluetoothAdapter.state == BluetoothAdapter.STATE_ON
    }

    fun stopScan() {

        try {
            if (scanCallback != null && Utils.isBluetoothAvailable()) { //fixed crash if BT if turned off, stop scan will crash.
                scanner?.stopScan(scanCallback)
                CentralLog.d(TAG, "scanning stopped")
            }
        } catch (e: Throwable) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(
                loggerTAG,
                "unable to stop scanning - callback null or bluetooth off? : ${e.localizedMessage}"
            )
            DBLogger.e(
                DBLogger.LogType.BLUETRACE,
                loggerTAG,
                "unable to stop scanning - callback null or bluetooth off? : ${e.localizedMessage}",
                DBLogger.getStackTraceInJSONArrayString(e as Exception)
            )
        }
    }
}
