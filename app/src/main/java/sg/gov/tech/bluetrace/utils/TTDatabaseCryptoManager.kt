package sg.gov.tech.bluetrace.utils

import android.content.Context
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.encryption.KeyUtil
import sg.gov.tech.bluetrace.logging.CentralLog

object TTDatabaseCryptoManager {

    fun getEncryptedFamilyMemberNRIC(context: Context, value: String): String? {
        return KeyUtil.encryptString(context, value, Preference.PHONE_NUMBER_KEY)
    }

    fun getDecryptedFamilyMemberNRIC(context: Context, value: String): String? {
        return KeyUtil.decryptString(context, value, Preference.PHONE_NUMBER_KEY)
    }

    /**
     * test encryption with RSA
     */
    fun testEncryptionWithRSA(context: Context, iter: Int){

        val start = System.currentTimeMillis()
        for(i in 0 until iter){
            val NRIC = "1234XXBR12"
            val enc = getEncryptedFamilyMemberNRIC(context, NRIC)
            //Log.d("TEST_CRYPTO_RSA_ENC","Encrypted string $enc")
        }
        val end = System.currentTimeMillis()

        val sec = (end - start) / iter
        CentralLog.d("TEST_CRYPTO",
            "Encryption time with RSA is $sec ms")
    }

    /**
     * test decryption with RSA
     */
    fun testDecryptionWithRSA(context: Context, iter: Int){

        val NRIC = "1234XXBR12"
        val enc = getEncryptedFamilyMemberNRIC(context, NRIC)

        val start = System.currentTimeMillis()

        for(i in 0 until iter){
           getDecryptedFamilyMemberNRIC(context, enc!!)
            //Log.d("TEST_CRYPTO_RSA_ENC","Encrypted string $enc")
        }
        val end = System.currentTimeMillis()

        val sec = (end - start) / iter
        CentralLog.d("TEST_CRYPTO",
            "Decryption time with RSA is $sec ms")
    }

    /**
     * test encryption with AES
     */
    fun testEncryptionWithAES(context: Context, iter: Int){
        val start = System.currentTimeMillis()
        for(i in 0 until iter){
            val NRIC = "G3372126P"
            val enc = KeyUtil.encryptString(context,NRIC,"FAMILY_MEMBER_NRIC_AES")
        }
        val end = System.currentTimeMillis()
        val sec = (end - start) / iter
        CentralLog.d("TEST_CRYPTO",
            "Encryption time with AES is $sec ms")
    }

    fun testDecryptionWithAES(context: Context, iter: Int){
        val NRIC = "G3372126P"
        val enc = KeyUtil.encryptString(context,NRIC,"FAMILY_MEMBER_NRIC_AES")

        val start = System.currentTimeMillis()
        for(i in 0 until iter){
            KeyUtil.decryptString(context, enc!!, "FAMILY_MEMBER_NRIC_AES")
        }

        val end = System.currentTimeMillis()
        val sec = (end - start) / iter
        CentralLog.d("TEST_CRYPTO",
            "Decryption time with AES is $sec ms")
    }
}
