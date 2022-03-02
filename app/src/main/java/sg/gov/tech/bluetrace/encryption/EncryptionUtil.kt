package sg.gov.tech.bluetrace.encryption

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger

object EncryptionUtil {

    private const val TAG = "KeyUtil"

    //DO NOT USE ANYMORE!!
    //Only here for legacy reasons.
    //There's a bug because it can't support more than 1 alias (secret)
    fun encryptStringBeforeM(
        context: Context, alias: String, passkey: String, encSecret: String?,
        setEncSecretFunc: (encSecret: String) -> Unit
    ): String? {
        try {
            // if RSA keypair cannot be created, failed.
            if (!KeyStoreUtil.generateRsaKeyPair(context, alias)) return null

            // decrypt secret key or create one if not exist.
            val secret = if (encSecret == null) {
                val s = AesEncryptionUtil.createRandomKey()
                val encSecretByteArray = RsaEncryptionUtil.encryptWithRsa(Preference.PHONE_NUMBER_KEY, s)
                if (encSecretByteArray == null) {
                    return null
                } else {
                    setEncSecretFunc(AesEncryptionUtil.byteArrayToBase64(encSecretByteArray))
                }
                s   // just give the secret
            } else {
                RsaEncryptionUtil.decryptWithRsa(
                    Preference.PHONE_NUMBER_KEY,
                    AesEncryptionUtil.base64ToByteArray(encSecret)
                )    //decrypt it
            }

            secret?.let {
                val encString = AesEncryptionUtil.encryptWithAesGcm(secret, passkey)

                return if (encString != null) {
                    AesEncryptionUtil.byteArrayToBase64(encString)
                } else null
            }
            return null
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot encrypt string for device before M.",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e(TAG, "Cannot encrypt string for device before M:$e")

            return null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun encryptStringMAndAbove(alias: String, string: String): String? {
        return try {
            val encString = AesEncryptionUtil.encryptWithAesGcm(alias, string)

            if (encString != null) {
                AesEncryptionUtil.byteArrayToBase64(encString)
            } else null
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot encrypt string for device M and above.",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e(TAG, "Cannot encrypt string for device M and above:$e")

            null
        }
    }

    fun decryptStringBeforeM(alias: String, encString: String, encSecret: String?): String? {
        try {
            if (encSecret == null) return null

            val decryptedSecret =
                RsaEncryptionUtil.decryptWithRsa(alias, AesEncryptionUtil.base64ToByteArray(encSecret))
            if (decryptedSecret != null) {
                return AesEncryptionUtil.decryptWithAesGcm(
                    decryptedSecret,
                    AesEncryptionUtil.base64ToByteArray(encString)
                )
            }
            return null
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot decrypt string for device before M.",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e(TAG, "Cannot decrypt string for device before M:$e")

            return null
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun decryptStringMAndAbove(alias: String, encString: String): String? {
        return try {
            AesEncryptionUtil.decryptWithAesGcm(
                alias,
                AesEncryptionUtil.base64ToByteArray(encString)
            )
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot decrypt string for device M and above.",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e(TAG, "Cannot decrypt string for device M and above:$e")

            null
        }
    }
}