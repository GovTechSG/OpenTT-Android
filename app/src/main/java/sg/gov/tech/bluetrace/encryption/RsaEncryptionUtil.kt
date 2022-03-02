package sg.gov.tech.bluetrace.encryption

import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

object RsaEncryptionUtil {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val RSA_MODE = "RSA/ECB/PKCS1Padding"

    fun encryptWithRsa(alias: String, plainText: String): ByteArray? {
        return encryptWithRsa(alias, plainText.toByteArray(Charsets.UTF_8))
    }

    fun encryptWithRsa(alias: String, secret: ByteArray): ByteArray? {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)

            //check if keyStore has entry, create if missing
            if (!keyStore.containsAlias(alias)) {
                KeyStoreUtil.generateRsaKeyPair(TracerApp.AppContext, alias)
            }

            val privateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry

            val inputCipher: Cipher = Cipher.getInstance(RSA_MODE)
            inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.certificate.publicKey)

            val outputStream = ByteArrayOutputStream()
            val cipherOutputStream = CipherOutputStream(outputStream, inputCipher)
            cipherOutputStream.write(secret)
            cipherOutputStream.close()
            return outputStream.toByteArray()
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot encrypt with RSA - $alias: ",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e("RsaEncryptionUtil", "Cannot encrypt with RSA - $alias: $e")
            return null
        }
    }

    fun decryptToStringWithRsa(alias: String, encrypted: ByteArray): String? {
        val bytes = decryptWithRsa(alias, encrypted)
        return if (bytes != null) {
            String(bytes, Charsets.UTF_8)
        } else {
            null
        }
    }

    fun decryptWithRsa(alias: String, encrypted: ByteArray): ByteArray? {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)
            val privateKeyEntry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry

            val output = Cipher.getInstance(RSA_MODE)
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
            val cipherInputStream = CipherInputStream(ByteArrayInputStream(encrypted), output)
            val values: ArrayList<Byte> = ArrayList()
            var nextByte: Int
            while (cipherInputStream.read().also { nextByte = it } != -1) {
                values.add(nextByte.toByte())
            }
            val decryptedKeyAsBytes = ByteArray(values.size)
            for (i in decryptedKeyAsBytes.indices) {
                decryptedKeyAsBytes[i] = values[i]
            }
            return decryptedKeyAsBytes
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            DBLogger.e(
                DBLogger.LogType.ENCRYPTION,
                loggerTAG,
                "Cannot decrypt with AES",
                DBLogger.getStackTraceInJSONArrayString(e)
            )
            CentralLog.e("RsaEncryptionUtil", "Cannot decrypt with AES:$e")

            return null
        }
    }
}
