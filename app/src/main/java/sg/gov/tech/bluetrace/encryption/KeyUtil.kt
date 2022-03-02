package sg.gov.tech.bluetrace.encryption

import android.content.Context
import android.os.Build

const val ENC_ALIAS = "enc-alias"
private const val PREF_ID = "core-pref"
private const val ENC_SECRET = "encrypted_secret"

object KeyUtil {

    fun getEncryptedSecret(context: Context): String? {
        val pref = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
        return pref.getString(ENC_SECRET, null)
    }

    fun setEncryptedSecret(context: Context, encPasskey: String) {
        val pref = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
        pref.edit().putString(ENC_SECRET, encPasskey).apply()
    }

    fun decryptString(context: Context, stringToDecrypt: String, keyName: String): String? {
        var decryptedString: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            decryptedString = EncryptionUtil.decryptStringMAndAbove(keyName, stringToDecrypt)
        } else {
            decryptedString =
                EncryptionUtil.decryptStringBeforeM(keyName, stringToDecrypt, getEncryptedSecret(context))
        }
        return decryptedString
    }

    fun encryptString(context: Context, stringToEncrypt: String, keyName: String): String? {

        var encryptedString: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            encryptedString = EncryptionUtil.encryptStringMAndAbove(keyName, stringToEncrypt)
        } else {
            encryptedString = EncryptionUtil.encryptStringBeforeM(
                context,
                keyName,
                stringToEncrypt,
                getEncryptedSecret(context)
            ) { encSecret ->
                setEncryptedSecret(context, encSecret)
            }
        }
        return encryptedString
    }

    fun encryptStringWithRSA(context: Context, data: String, alias: String): String?{
        KeyStoreUtil.generateRsaKeyPair(context,alias)
        val encryptedByteArray = RsaEncryptionUtil.encryptWithRsa(alias,data)
        return encryptedByteArray?.let { AesEncryptionUtil.byteArrayToBase64(it) }
    }

    fun decryptStringWithRSA(alias: String, encryptedString: String) : String?{
        val decryptedByteArray = AesEncryptionUtil.base64ToByteArray(encryptedString)
        return RsaEncryptionUtil.decryptToStringWithRsa(alias,decryptedByteArray)
    }
}
