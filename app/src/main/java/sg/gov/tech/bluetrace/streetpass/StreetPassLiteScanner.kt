package sg.gov.tech.bluetrace.streetpass

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import android.util.Base64
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.bluetooth.BLEScanner
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.protocol.BTLite.StreetPassLite
import sg.gov.tech.bluetrace.status.Status
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.properties.Delegates

class StreetPassLiteScanner constructor(
    context: Context,
    private val scanDurationInMillis: Long
) {

    private var scanner: BLEScanner by Delegates.notNull()

    private var context: Context by Delegates.notNull()
    private val TAG = "StreetPassLiteScanner"

    private var handler: Handler = Handler()

    var scannerCount = 0

    private var blueTraceLiteServiceUUID: ParcelUuid =
        ParcelUuid(UUID.fromString(BuildConfig.BT_LITE_SSID))

    val scanCallback = BleScanCallback()

    val blacklist: HashMap<String, Int> = HashMap()

    init {

        val filters: ArrayList<ScanFilter> = ArrayList()

        val bluetraceLiteFilter = setupBlueTraceLiteFilter()
        filters.add(bluetraceLiteFilter)

        scanner = BLEScanner(context, filters, 0)
        this.context = context
    }

    fun startScan() {
        blacklist.clear()

        var statusRecord = Status("Scanning Started")
        Utils.broadcastStatusReceived(context, statusRecord)

        scanner.startScan(scanCallback)
        scannerCount++
        handler.postDelayed(
            { stopScan() }
            , scanDurationInMillis)

        CentralLog.d(TAG, "btl scanning started")
    }

    fun stopScan() {
        //only stop if scanning was successful - kinda.
        if (scannerCount > 0) {
            scanner.flush()

            var statusRecord = Status("Scanning Stopped")
            Utils.broadcastStatusReceived(context, statusRecord)
            scannerCount--
            scanner.stopScan()
        }

        blacklist.clear()

        CentralLog.d(TAG, "btl scanning stopped")
    }

    fun isScanning(): Boolean {
        return scannerCount > 0
    }

    fun handleBlueTraceLiteScan(scanResult: ScanResult, scanRecord: ScanRecord) {

        CentralLog.d(TAG, "BTL Service UUID Scanned: ${scanRecord.serviceUuids}")

        scanRecord.serviceData[blueTraceLiteServiceUUID]?.let { serviceData ->
            val connectionRecord =
                StreetPassLite
                    .processReadRequestDataReceived(
                        dataRead = serviceData,
                        peripheralAddress = scanResult.device.address,
                        rssi = scanResult.rssi
                    )

            connectionRecord?.let {
                Utils.broadcastStreetPassLiteReceived(
                    context,
                    connectionRecord
                )
            }
            CentralLog.i(
                TAG,
                "Read BTL Advertised Data BlueTraceLite: ${Base64.encodeToString(
                    serviceData,
                    Base64.NO_WRAP
                )}, length: ${serviceData.size} and serviceuuid: ${scanRecord.serviceUuids}"
            )
        }
    }

    inner class BleScanCallback : ScanCallback() {

        private val TAG = "BleScanCallback"

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            CentralLog.i(TAG, "BTL - OnBatch: ${results?.size ?: 0}")

            results?.forEach {
                if (!blacklist.containsKey(it.device.address)) {
                    processScanResult(it)
                    blacklist.put(it.device.address, 1)
                }
            }
        }

        private fun processScanResult(scanResult: ScanResult) {
            scanResult.let { result ->
                result.scanRecord?.let { scanRecord ->

                    if (scanRecord.serviceUuids?.contains(blueTraceLiteServiceUUID) == true) {
                        CentralLog.d(TAG, "BTL Detected BlueTrace Lite protocol")
                        handleBlueTraceLiteScan(result, scanRecord)
                        return@processScanResult
                    } else {
                        CentralLog.d(TAG, "BTL NOT Detected - ignoring")
                    }
                }

            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            CentralLog.i(TAG, "BTL - onScanResult")

            result?.let {
                if (!blacklist.containsKey(result.device.address)) {
                    processScanResult(result)
                    blacklist.put(result.device.address, 1)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

            val reason = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "$errorCode - SCAN_FAILED_ALREADY_STARTED"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "$errorCode - SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "$errorCode - SCAN_FAILED_FEATURE_UNSUPPORTED"
                SCAN_FAILED_INTERNAL_ERROR -> "$errorCode - SCAN_FAILED_INTERNAL_ERROR"
                else -> {
                    "$errorCode - UNDOCUMENTED"
                }
            }
            val loggerTAG =
                "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
            CentralLog.e(TAG, "BT Scan failed: $reason")
            DBLogger.e(
                DBLogger.LogType.BLUETRACELITE,
                loggerTAG,
                "BT Scan failed: $reason",
                null
            )
            if (scannerCount > 0) {
                scannerCount--
            }
        }
    }

    fun setupBlueTraceLiteFilter(): ScanFilter {
        return ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(BuildConfig.BT_LITE_SSID)))
            .build()
    }


}

