package sg.gov.tech.bluetrace.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.Handler
import android.os.ParcelUuid
import android.util.Base64
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.idmanager.TempIDManager
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import java.util.*

class BLEAdvertiser {

    private var advertiser: BluetoothLeAdvertiser? =
        BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
    private val TAG = "BLEAdvertiser"
    private var charLength = 3
    private var bluetraceCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            CentralLog.i(TAG, "Advertising onStartSuccess bluetrace")
            CentralLog.i(TAG, settingsInEffect.toString())
            isAdvertisingBT = true
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)

            var reason: String

            when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    reason = "ADVERTISE_FAILED_ALREADY_STARTED"
                    isAdvertisingBT = true
                }
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> {
                    reason = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                    isAdvertisingBT = false
                }
                ADVERTISE_FAILED_INTERNAL_ERROR -> {
                    reason = "ADVERTISE_FAILED_INTERNAL_ERROR"
                    isAdvertisingBT = false
                }
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> {
                    reason = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                    isAdvertisingBT = false
                }
                ADVERTISE_FAILED_DATA_TOO_LARGE -> {
                    reason = "ADVERTISE_FAILED_DATA_TOO_LARGE"
                    isAdvertisingBT = false
                    charLength--
                }

                else -> {
                    reason = "UNDOCUMENTED"
                }
            }

            CentralLog.d(TAG, "Advertising onStartFailure: $errorCode - $reason")
        }
    }

    private var bluetraceLiteCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            CentralLog.i(TAG, "Advertising onStartSuccess bluetraceLite")
            CentralLog.i(TAG, settingsInEffect.toString())
            isAdvertisingBTL = true
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)

            var reason: String

            when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    reason = "ADVERTISE_FAILED_ALREADY_STARTED"
                    isAdvertisingBTL = true
                }
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> {
                    reason = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                    isAdvertisingBTL = false
                }
                ADVERTISE_FAILED_INTERNAL_ERROR -> {
                    reason = "ADVERTISE_FAILED_INTERNAL_ERROR"
                    isAdvertisingBTL = false
                }
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> {
                    reason = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                    isAdvertisingBTL = false
                }
                ADVERTISE_FAILED_DATA_TOO_LARGE -> {
                    reason = "ADVERTISE_FAILED_DATA_TOO_LARGE"
                    isAdvertisingBTL = false
                    charLength--
                }

                else -> {
                    reason = "UNDOCUMENTED"
                }
            }

            CentralLog.d(TAG, "Advertising onStartFailure: $errorCode - $reason")
        }
    }

    val pUuid = ParcelUuid(UUID.fromString(BuildConfig.BLE_SSID))
    val pUuid2 = ParcelUuid(UUID.fromString(BuildConfig.BT_LITE_SSID))

    val settings = AdvertiseSettings.Builder()
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setConnectable(true)
        .setTimeout(0)
        .build()

//    var bluetraceData: AdvertiseData? = null
//    var bluetraceLiteData: AdvertiseData? = null

    var handler = Handler()

    var stopRunnable: Runnable = Runnable {
        CentralLog.i(TAG, "Advertising stopping as scheduled.")
        stopAdvertising()
    }

    var isAdvertisingBT = false
    var isAdvertisingBTL = false

    private var shouldBeAdvertising = false

    fun shouldBeAdvertising() : Boolean {
        CentralLog.d(TAG, "Advertising BT: $isAdvertisingBT, BTL: $isAdvertisingBTL")
        return shouldBeAdvertising
    }

    //reference
    //https://code.tutsplus.com/tutorials/how-to-advertise-android-as-a-bluetooth-le-peripheral--cms-25426
    private fun startAdvertisingLegacy(timeoutInMillis: Long) {

        val randomUUID = UUID.randomUUID().toString()
        val finalString = randomUUID.substring(randomUUID.length - charLength, randomUUID.length)
        CentralLog.d(TAG, "Unique string: $finalString")
        val serviceDataByteArray = finalString.toByteArray()

        val bluetraceData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(true)
            .addServiceUuid(pUuid)
            .addManufacturerData(1023, serviceDataByteArray)
            .build()

        //Start of date generation
        val today = Date()
        var firstJan: Long = 1577836800
        var thousands: Long = 1000
        var firstJan2020Date = firstJan * thousands
        val firstJan2020 = Date(firstJan2020Date)
        var difference = today.time - firstJan2020Date
        var daysDifference = (difference / 86400000) % 255
        var daysDifferenceByte = daysDifference.toByte()

        var version = 1
        var versionByte = version.toByte()

        var groupNumber1 = 0
        var groupNumber2 = 0
        var groupNumber1Byte = groupNumber1.toByte()
        var groupNumber2Byte = groupNumber2.toByte()

        CentralLog.i(TAG, "First jan date: ${firstJan2020} and today: ${today} and ${today.time}")
        CentralLog.i(TAG, "First jan diff: ${daysDifference}")


        val liteTempId = TempIDManager.retrieveShortID(TracerApp.AppContext)


        liteTempId?.let {

            CentralLog.i(TAG, "Short TempID retrieved: $it")
//            var randomHexString32 = generateRandomHexString(32)
//            var hexString32 = Hex.stringToBytes(randomHexString32)

            val decodedData = Base64.decode(it.tempID, Base64.DEFAULT)
            val advertiseData = decodedData

            CentralLog.i(
                TAG,
                "BTL Advert: ${it.tempID}, BArray Length: ${advertiseData.size}, Decoded data size: ${decodedData.size}"
            )


            var bArray = ByteArray(20)
            System.arraycopy(advertiseData, 0, bArray, 0, advertiseData.size)
//            bArray[19] = versionByte
//            bArray[18] = daysDifferenceByte
//            bArray[17] = groupNumber1Byte
//            bArray[16] = groupNumber2Byte

            val bluetraceLiteData = AdvertiseData.Builder()
                .addServiceUuid(pUuid2)
                .addServiceData(pUuid2, bArray)
                .build()

            val dataAdvertisedB64 = Base64.encodeToString(bArray, Base64.DEFAULT)

            if (BuildConfig.ADVERTISE_BTL) {
                try {
                    CentralLog.d(TAG, "Start advertising BTL: $dataAdvertisedB64")
                    advertiser =
                        advertiser ?: BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
                    advertiser?.startAdvertising(settings, bluetraceLiteData, bluetraceLiteCallback)
                } catch (e: Throwable) {
                    val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                    CentralLog.e(loggerTAG, "Failed to start advertising legacy: ${e.message}")
                    DBLogger.e(
                        DBLogger.LogType.BLUETRACELITE,
                        loggerTAG,
                        "Failed to start advertising legacy: ${e.message}",
                        DBLogger.getStackTraceInJSONArrayString(e as Exception)
                    )
                }
            }
        }

        if (BuildConfig.ADVERTISE_BT) {
            try {
                CentralLog.d(TAG, "Start advertising BT")
                advertiser =
                    advertiser ?: BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
                advertiser?.startAdvertising(settings, bluetraceData, bluetraceCallback)
            } catch (e: Throwable) {
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                CentralLog.e(loggerTAG, "Failed to start advertising legacy: ${e.message}")
                DBLogger.e(
                    DBLogger.LogType.BLUETRACE,
                    loggerTAG,
                    "Failed to start advertising legacy: ${e.message}",
                    DBLogger.getStackTraceInJSONArrayString(e as Exception)
                )
            }
        }

            handler.removeCallbacksAndMessages(stopRunnable)
            handler.postDelayed(stopRunnable, timeoutInMillis)
    }

    fun startAdvertising(timeoutInMillis: Long) {
        CentralLog.d(TAG, "Start advert. Current state: Advertising BT: $isAdvertisingBT, BTL: $isAdvertisingBTL")
        startAdvertisingLegacy(timeoutInMillis)
        shouldBeAdvertising = true
    }

    fun stopAdvertising() {

        CentralLog.d(TAG, "stop advertising")

        try {
            if (BuildConfig.ADVERTISE_BT) {
                advertiser?.stopAdvertising(bluetraceCallback)
                isAdvertisingBT = false
            }
        } catch (e: Throwable) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(loggerTAG, "Failed to stop advertising BT: ${e.message}")
            DBLogger.e(
                DBLogger.LogType.BLUETRACE,
                loggerTAG,
                "Failed to stop advertising BT: ${e.message}",
                DBLogger.getStackTraceInJSONArrayString(e as Exception)
            )
        }

        try {
            if (BuildConfig.ADVERTISE_BTL) {
                advertiser?.stopAdvertising(bluetraceLiteCallback)
                isAdvertisingBTL = false
            }
        } catch (e: Throwable) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(loggerTAG, "Failed to stop advertising BTL: ${e.message}")
            DBLogger.e(
                DBLogger.LogType.BLUETRACE,
                loggerTAG,
                "Failed to stop advertising BTL: ${e.message}",
                DBLogger.getStackTraceInJSONArrayString(e as Exception)
            )
        }
        shouldBeAdvertising = false
        handler.removeCallbacksAndMessages(null)
    }
}
