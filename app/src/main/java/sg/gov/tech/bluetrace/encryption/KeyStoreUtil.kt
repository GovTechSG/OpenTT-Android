package sg.gov.tech.bluetrace.encryption

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.security.auth.x500.X500Principal

object KeyStoreUtil {
    private const val TAG = "KeyStoreUtil"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val RSA = "RSA"

    fun generateRsaKeyPair(
        context: Context,
        alias: String
    ): Boolean { // Generate a key pair for encryption
        try {
            val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(alias)) {
                val start: Calendar = Calendar.getInstance()
                val end: Calendar = Calendar.getInstance()
                end.add(Calendar.YEAR, 30)
                val spec = KeyPairGeneratorSpec.Builder(context)
                    .setKeySize(4096)
                    .setAlias(alias)
                    .setSubject(X500Principal("CN=$alias"))
                    .setSerialNumber(BigInteger.TEN)
                    .setStartDate(start.time)
                    .setEndDate(end.time)
                    .build()
                val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(RSA, ANDROID_KEY_STORE)
                kpg.initialize(spec)
                kpg.generateKeyPair()
            }
            return true
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot create RSA key pair.",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e(TAG, "Cannot create RSA key pair:$e")
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun generateAesKey(alias: String): SecretKey? {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)

            val keyEntry = keyStore.getEntry(alias, null)
            return if (keyEntry == null) {
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                val keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            } else {
                (keyEntry as KeyStore.SecretKeyEntry).secretKey
            }
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot create AES key.",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e(TAG, "Cannot create AES key:$e")
            return null
        }
    }

    fun removeKey(alias: String) {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        keyStore.deleteEntry(alias)
    }

    fun removeAllKeys() {
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        val aliases = keyStore.aliases()
        for (alias in aliases) {
            keyStore.deleteEntry(alias)
        }
    }
}
