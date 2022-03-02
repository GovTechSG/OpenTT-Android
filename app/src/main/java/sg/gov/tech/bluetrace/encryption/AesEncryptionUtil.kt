package sg.gov.tech.bluetrace.encryption

import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesEncryptionUtil {

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val AES_ECB_PKCS7 = "AES/ECB/PKCS7Padding"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"

    fun createRandomKey(): ByteArray {
        val key = ByteArray(16)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(key)
        return key
    }

    /**
     * This method will use AES secret key stored in KeyStore to encrypt
     */

    @RequiresApi(Build.VERSION_CODES.M)
    fun encryptWithAesGcm(alias: String, plainString: String): ByteArray? {
        return try {
            val secretKey = KeyStoreUtil.generateAesKey(alias)

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val cipherMessage = cipher.doFinal(plainString.toByteArray(Charsets.UTF_8))

            wrapCipherMessageWithIV(cipher.iv, cipherMessage)
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}(Android M and above)"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot encrypt with AES",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e("AsymmetricEncrypt", "Cannot encrypt with AES:$e")
            null
        }
    }

    /**
     * Use AES secret key in KeyStore to decrypt.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun decryptWithAesGcm(alias: String, cipherMessage: ByteArray): String? {
        try {
            val cipherData = unwrapCipherMessage(cipherMessage)
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)

            val secretKeyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry

            val secretKey: SecretKey = secretKeyEntry.secretKey

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val spec = GCMParameterSpec(128, cipherData[0])
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decodedData = cipher.doFinal(cipherData[1])
            return String(decodedData, Charsets.UTF_8)
        } catch (e: Throwable) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}(Android M and above)"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot decrypt with AES",
                DBLogger.getStackTraceInJSONArrayString(e as Exception)
            )
            CentralLog.e("AsymmetricEncrypt", "Cannot decrypt with AES:$e")
            return null
        }
    }

    /**
     * AES encryption method with GCM
     * Use provided secret key to encrypt.
     */
    fun encryptWithAesGcm(key: ByteArray, plainText: String): ByteArray? {
        return try {
            // generate initialization vector
            val iv = ByteArray(12)
            val secureRandom = SecureRandom()
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val parameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), parameterSpec)

            val cipherMessage = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return wrapCipherMessageWithIV(cipher.iv, cipherMessage)
        } catch (e: Throwable) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}(before M)"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot encrypt with AES",
                DBLogger.getStackTraceInJSONArrayString(e as Exception)
            )
            CentralLog.e("AsymmetricEncrypt", "Cannot encrypt with AES:$e")

            null
        }
    }

    /**
     * AES decryption method with GCM
     * Use provided secret key to decrypt.
     */
    fun decryptWithAesGcm(key: ByteArray, encryptedData: ByteArray): String? {
        return try {
            val cipherData = unwrapCipherMessage(encryptedData)

            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, cipherData[0])
            )
            val plainText = cipher.doFinal(cipherData[1])
            String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}(before M)"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot decrypt with AES",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e("AsymmetricEncrypt", "Cannot decrypt with AES:$e")

            null
        }
    }

    fun encryptWithAes(key: ByteArray, plainText: String): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(AES_ECB_PKCS7)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))

            return cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot encrypt with AES",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e("AsymmetricEncrypt", "Cannot encrypt with AES:$e")

            null
        }
    }

    fun decryptWithAes(key: ByteArray, encryptedData: ByteArray): String? {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        return try {
            val cipher = Cipher.getInstance(AES_ECB_PKCS7)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))

            val decodedBytes = cipher.doFinal(encryptedData)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot decrypt with AES",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e(loggerTAG, "Cannot decrypt with AES:$e")

            null
        }
    }

    // Utility functions
    fun base64ToByteArray(string: String): ByteArray {
        return Base64.decode(string, Base64.DEFAULT)
    }

    fun byteArrayToBase64(bs: ByteArray): String {
        return Base64.encodeToString(bs, Base64.DEFAULT)
    }

    private fun wrapCipherMessageWithIV(iv: ByteArray, cipherMessage: ByteArray): ByteArray {
        // concat the iv in the encrypted message: size + cipher message
        val byteBuffer: ByteBuffer = ByteBuffer.allocate(4 + iv.size + cipherMessage.size)
        byteBuffer.putInt(iv.size)
        byteBuffer.put(iv)
        byteBuffer.put(cipherMessage)
        return byteBuffer.array()
    }

    private fun unwrapCipherMessage(cipherMessage: ByteArray): Array<ByteArray> {
        // unpack the cipher message.
        // first part is the iv, last is cipher message
        val byteBuffer = ByteBuffer.wrap(cipherMessage)
        val ivLength = byteBuffer.int
        require(!(ivLength < 12 || ivLength >= 16)) {
            // check input parameter
            "invalid iv length"
        }
        val iv = ByteArray(ivLength)
        byteBuffer[iv]
        val cipherText = ByteArray(byteBuffer.remaining())
        byteBuffer[cipherText]
        return arrayOf(iv, cipherText)
    }
}
