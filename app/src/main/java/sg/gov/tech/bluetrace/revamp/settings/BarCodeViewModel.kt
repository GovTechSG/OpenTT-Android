package sg.gov.tech.bluetrace.revamp.settings

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import java.text.CharacterIterator
import java.text.StringCharacterIterator

class BarCodeViewModel : ViewModel() {
    private val TAG = "BarCodeViewModel"
    private var isIdMasked: Boolean = true

    companion object {
        private const val charSet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
        const val OTHER_USER = 0
        const val PASSPORT_USER = 1
    }

    fun getUserID(): String {
        var user = Preference.getEncryptedUserData(TracerApp.AppContext)
        return user?.id ?: ""
    }

    fun getBarcodeBitmap(userID: String, onComplete: (Bitmap) -> Unit) {
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix = multiFormatWriter.encode(userID, BarcodeFormat.CODE_39, 200, 200)
            val bitmap = BarcodeEncoder().createBitmap(bitMatrix)
            onComplete.invoke(bitmap)
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(TAG, "Error creating barcode bitmap")
            DBLogger.e(
                DBLogger.LogType.SAFEENTRY,
                loggerTAG,
                "Error creating barcode bitmap",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
        }
    }

    fun isIDMasked(onComplete: (Boolean) -> Unit) {
        if (isIdMasked) {
            isIdMasked = false
            onComplete.invoke(true)
        } else {
            isIdMasked = true
            onComplete.invoke(false)
        }
    }

    //For Code39 Mod43 checksum, single check digit at the suffix
    private fun getChecksum(barCode: String): Char? {
        var total = 0
        val it: CharacterIterator = StringCharacterIterator(barCode)
        var ch: Char = it.current()
        while (ch != CharacterIterator.DONE) {
            val charValue = charSet.indexOf(ch)
            if (charValue == -1) {
                // Invalid character.
                CentralLog.e(TAG,"Input String '$barCode' contains characters that are invalid in a Code39 barcode.")
                return null
            }
            total += charValue
            ch = it.next()
        }
        val checksum = total % 43
        return charSet[checksum]
    }

    fun getPassportNumberWithCheckSum(id: String): String {
        var ppNum = "PP-$id"
        ppNum += getChecksum(ppNum) ?: ""

        return ppNum
    }
}