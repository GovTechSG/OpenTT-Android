package sg.gov.tech.bluetrace.streetpass

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import android.util.Base64
import com.google.android.gms.common.util.Hex
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.bluetooth.BLEScanner
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.status.Status
import java.util.*
import kotlin.properties.Delegates

class StreetPassScanner constructor(
    context: Context,
    private val scanDurationInMillis: Long
) {

    private var scanner: BLEScanner by Delegates.notNull()

    private var context: Context by Delegates.notNull()
    private val TAG = "StreetPassScanner"

    private var handler: Handler = Handler()

    var scannerCount = 0

    val scanCallback = BleScanCallback()

    val BT_PARCEL_SSID = ParcelUuid(UUID.fromString(BuildConfig.BLE_SSID))


    init {

        val filters: ArrayList<ScanFilter> = ArrayList()

        val bluetraceFilter = setupBlueTraceFilter()
        val bluetraceIOSBackgroundFilter = setupBlueTraceIOSBackgroundFilter()

        filters.add(bluetraceFilter)
        filters.add(bluetraceIOSBackgroundFilter)

        scanner = BLEScanner(context, filters, 0)
        this.context = context
    }

    fun startScan() {

        var statusRecord = Status("Scanning Started")
        Utils.broadcastStatusReceived(context, statusRecord)

        scanner.startScan(scanCallback)
        scannerCount++

        handler.postDelayed(
            { stopScan() }
            , scanDurationInMillis)

        CentralLog.i(TAG, "BT scanning started")
    }

    fun stopScan() {
        //only stop if scanning was successful - kinda.
        if (scannerCount > 0) {
            var statusRecord = Status("Scanning Stopped")
            Utils.broadcastStatusReceived(context, statusRecord)
            scannerCount--
            scanner.stopScan()
        }

        CentralLog.i(TAG, "BT scanning stopped")
    }

    fun isScanning(): Boolean {
        return scannerCount > 0
    }

    inner class BleScanCallback : ScanCallback() {

        private val TAG = "BleScanCallback"

        private fun processScanResult(scanResult: ScanResult?) {

            scanResult?.let { result ->

                CentralLog.i(
                    TAG,
                    "Checking Scan Result from bluetrace: ${scanResult.device.address}"
                )

                if (result.rssi < BuildConfig.MIN_RSSI) {
                    CentralLog.d(
                        TAG,
                        "To not connect since rssi less than ${BuildConfig.MIN_RSSI}"
                    )
                    return
                }

                val hasBT = result.scanRecord?.serviceUuids?.contains(BT_PARCEL_SSID) ?: false

                var isIosBG = false

                if (scanResult.scanRecord?.getManufacturerSpecificData(76) != null) {

                    var scanResultHexString =
                        Hex.bytesToStringLowercase(scanResult.scanRecord?.bytes).toLowerCase()
                    var overflowIndex = scanResultHexString.indexOf("ff4c0001")
                    if (overflowIndex != -1) {
                        if (BuildConfig.DEBUG) {
                            //Looking for the serviceid bitmask
                            if (scanResultHexString.length > overflowIndex + 12) {
                                var serviceIdBit =
                                    scanResultHexString.get(overflowIndex + 12).toString()
                                var valueBit = Integer.valueOf(serviceIdBit, 16)
                                CentralLog.d(
                                    TAG,
                                    "Processing scanResultHex:$scanResultHexString valueBit: $valueBit and serviceIDBBit: $serviceIdBit"
                                )
                                //Meaning the bit is 8 and above in terms of hex
                                if (valueBit > 7) {
                                    isIosBG = true
                                }
                            }

                        } else {
                            if (scanResultHexString.length > overflowIndex + 9) {
                                var serviceIdBit =
                                    scanResultHexString.get(overflowIndex + 9).toString()
                                var serviceIdBitDebug =
                                    scanResultHexString.get(overflowIndex + 8).toString()
                                var valueBit = Integer.valueOf(serviceIdBit, 16)
                                CentralLog.d(
                                    TAG,
                                    "Processing scanResultHex:$scanResultHexString valueBit: $valueBit and serviceIDBBit: $serviceIdBit and debug: $serviceIdBitDebug"
                                )
                                //Meaning the bit is 8 and above in terms of hex
                                if (valueBit > 0) {
                                    isIosBG = true
                                }
                            }
                        }
                    }
                }

                if (!hasBT && !isIosBG) {
                    return
                }

                CentralLog.i(
                    TAG,
                    "Processing Scan Result from bluetrace: ${scanResult.device.address}"
                )

                val device = result.device
                var rssi = result.rssi
                var txPower: Int? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    txPower = result.txPower
                    if (txPower == 127) {
                        txPower = null
                    }
                }

                val manuData: ByteArray =
                    scanResult.scanRecord?.getManufacturerSpecificData(1023)
                        ?: "N.A".toByteArray()
                val manuString = String(manuData, Charsets.UTF_8)

                val connectable = ConnectablePeripheral(manuString, txPower, rssi)

                //check ios?
                val manuDataIOS: ByteArray =
                    scanResult.scanRecord?.getManufacturerSpecificData(76)
                        ?: "N.A".toByteArray()
                val manuIOSString = Base64.encodeToString(manuDataIOS, Base64.DEFAULT)

                CentralLog.i(TAG, "Scanned: ${device.address} - ${manuString} - $manuIOSString")

                result.scanRecord?.serviceUuids?.forEach {
                    CentralLog.i(TAG, "Scanned REC: ${device.address} - ${manuString} - $it")
                }

                Utils.broadcastDeviceScanned(context, device, connectable)
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            processScanResult(result)
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
                DBLogger.LogType.BLUETRACE,
                loggerTAG,
                "BT Scan failed: $reason",
                null
            )
            if (scannerCount > 0) {
                scannerCount--
            }
        }
    }


    fun setupBlueTraceFilter(): ScanFilter {
        return ScanFilter.Builder()
            .setServiceUuid(BT_PARCEL_SSID)
            .build()
    }

    fun setupBlueTraceIOSBackgroundFilter(): ScanFilter {
        var encodedUuid = BuildConfig.IOS_BACKGROUND_UUID

        return ScanFilter.Builder()
            .setServiceUuid(null)
            .setManufacturerData(76, byteArrayOf())
            .build()
    }

}

