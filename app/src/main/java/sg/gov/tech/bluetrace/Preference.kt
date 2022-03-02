package sg.gov.tech.bluetrace

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.google.gson.Gson
import sg.gov.tech.bluetrace.encryption.KeyUtil
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.Fragments
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData

object Preference {
    private const val HAS_SEEN_SAFE_ENTRY_DIALOG = "HAS_SEEN_SAFE_ENTRY_DIALOG"
    private const val APP_VERSION = "APP_VERSION"
    private const val PREF_ID = "Tracer_pref"
    private const val IS_ONBOARDED = "IS_ONBOARDED"
    private const val IS_ONBOARDED_IDENTITY = "IS_ONBOARDED_IDENTITY"
    private const val CHECK_POINT = "CHECK_POINT"
    private const val HANDSHAKE_PIN = "HANDSHAKE_PIN"
    private const val TTID = "TTID"
    private const val NEXT_FETCH_TIME = "NEXT_FETCH_TIME"
    private const val EXPIRY_TIME = "EXPIRY_TIME"
    private const val LAST_FETCH_TIME = "LAST_FETCH_TIME"
    private const val LAST_PURGE_TIME = "LAST_PURGE_TIME"
    const val ANNOUNCEMENT = "ANNOUNCEMENT"
    private const val USER_KEY = "USER_DATA"
    const val PHONE_NUMBER_KEY = "PHONE_NUMBER_KEY"

    private const val ENCRYPTED_USER_DATA = "ENCRYPTED_USER_DATA"
    //do not fix this spelling mistake
    private const val ENCRYPTED_PHONE_NUMBER = "ENCRYPTED_PHONNE_NUMBER"

    private const val ANNOUNCEMENT_VERSION = "ANNOUNCEMENT_VERSION"
    private const val ANNOUNCEMENT_SEEN = "ANNOUNCEMENT_SEEN"
    private const val SE_LAST_REFRESH_TIME = "SE_LAST_REFRESH_TIME"

    private const val SHOULD_SHOW_OPTIONAL_UPDATE_DIALOG = "SHOULD_SHOW_OPTIONAL_UPDATE_DIALOG"
    const val PAUSE_UNTIL = "PAUSE_UNTIL"

    const val PREFERRED_LANGUAGE = "PREFERRED_LANGUAGE"
    private const val LAST_SHOWCASE_HOW_TO_USE = "LAST_SHOWCASE_HOW_TO_USE"
    private const val LAST_APP_UPDATED_SHOWN = "LAST_APP_UPDATED_SHOWN"

    private const val IS_FAV_NEW = "IS_FAV_NEW"
    private const val IS_MANAGE_FAMILY_MEM_NEW = "IS_MANAGE_FAMILY_MEM_NEW"
    private const val USER_REGISTRATION_DATE = "USER_REGISTRATION_DATE"
    private const val IS_SUBMIT_ERROR_LOG_NEW = "IS_SUBMIT_ERROR_LOG_NEW"

    private const val CAMERA_PERM_REQUESTED = "CAMERA_PERM_REQUESTED"
    private const val IS_USER_DETAILS_PURGED = "IS_USER_DETAILS_PURGED"

    private const val SHOULD_SHOW_PRIVACY_POLICY = "SHOULD_SHOW_PRIVACY_POLICY" //To support dialog pop up once per app launch
    private const val PRIVACY_POLICY_POLICY_VERSION = "PRIVACY_POLICY_POLICY_VERSION" //To show that user has accepted this current policy
    private const val CONSENT_PRIVACY_POLICY_API_SUCCESS = "CONSENT_PRIVACY_POLICY_API_SUCCESS" //To show that the API to consent the policy is done successfully

    private const val IS_HEALTH_STATUS_NEW = "IS_HEALTH_STATUS_NEW"

    fun putHandShakePin(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(HANDSHAKE_PIN, value).apply()
    }

    fun getHandShakePin(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(HANDSHAKE_PIN, "AERTVC") ?: "AERTVC"
    }

    fun putIsOnBoarded(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(IS_ONBOARDED, value).apply()
    }

    fun isOnBoarded(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(IS_ONBOARDED, false)
    }

    fun putOnBoardedWithIdentity(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(IS_ONBOARDED_IDENTITY, value).apply()
    }

    fun onBoardedWithIdentity(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(IS_ONBOARDED_IDENTITY, false)
    }

    fun saveEncryptedPhoneNumber(context: Context, value: String): Boolean {
        //aes
        var encryptedString = KeyUtil.encryptString(context, value, PHONE_NUMBER_KEY)
        encryptedString?.let { encString ->
            context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(ENCRYPTED_PHONE_NUMBER, encryptedString).apply()
            return true
        }
        return false

//        val rsaBytes = encryptWithRsa(RSA_PHONE_NUMBER_KEY, value)
//        if (rsaBytes != null) {
//            val rsaB64 = byteArrayToBase64(rsaBytes)
//            context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
//                .edit().putString(RSA_PHONE_NUMBER, rsaB64).apply()
//            return true
//        }
//        return false
    }

    fun getEncryptedPhoneNumber(context: Context): String {
        val encryptedString: String = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(ENCRYPTED_PHONE_NUMBER, "") ?: ""
        CentralLog.d("Preference", "Encrypted Phone Number: $encryptedString")
        if (encryptedString.isNotEmpty()) {
            val decryptedString = KeyUtil.decryptString(context, encryptedString, PHONE_NUMBER_KEY)
            CentralLog.d("Preference", "Encrypted Phone Number retrieved: $decryptedString")
            return decryptedString ?: ""
        }
        return ""
    }

    fun putCheckpoint(context: Context, value: Int) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putInt(CHECK_POINT, value).apply()
    }

    fun getCheckpoint(context: Context): Int {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getInt(CHECK_POINT, -1)
    }

    fun getLastFetchTimeInMillis(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(
                LAST_FETCH_TIME, 0
            )
    }

    fun putLastFetchTimeInMillis(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(LAST_FETCH_TIME, time).apply()
    }

    fun putNextFetchTimeInMillis(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(NEXT_FETCH_TIME, time).apply()
    }

    fun getNextFetchTimeInMillis(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(
                NEXT_FETCH_TIME, 0
            )
    }

    fun putExpiryTimeInMillis(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(EXPIRY_TIME, time).apply()
    }

    fun getExpiryTimeInMillis(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(
                EXPIRY_TIME, 0
            )
    }

    fun putAnnouncement(context: Context, announcement: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(ANNOUNCEMENT, announcement).apply()
    }

    fun getAnnouncement(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(ANNOUNCEMENT, "") ?: ""
    }

    fun putLastPurgeTime(context: Context, lastPurgeTime: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(LAST_PURGE_TIME, lastPurgeTime).apply()
    }

    fun getLastPurgeTime(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(LAST_PURGE_TIME, 0)
    }

    fun registerListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun putTtID(context: Context, value: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(TTID, value).apply()
    }

    fun getTtID(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(TTID, "") ?: ""
    }

    fun putshouldShowOptionalUpdateDialog(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(SHOULD_SHOW_OPTIONAL_UPDATE_DIALOG, value).apply()
    }

    fun shouldShowOptionalUpdateDialog(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(SHOULD_SHOW_OPTIONAL_UPDATE_DIALOG, false)
    }

    fun putPauseUntil(context: Context, timeToResume: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(PAUSE_UNTIL, timeToResume).apply()
    }

    fun getPauseUntil(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(PAUSE_UNTIL, 0)
    }

    fun shouldBePaused(context: Context): Boolean {
        return System.currentTimeMillis() < getPauseUntil(context)
    }

    fun saveEncryptedUserData(context: Context, user: RegisterUserData): Boolean {
        var gson = Gson()
        var userDataString = gson.toJson(user)

        //To keep it consistent for users who already are using the USER key in production
        //For Android 5.1, will use the new key instead which is PHONE_NUMBER_KEY
        var key: String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) USER_KEY else PHONE_NUMBER_KEY
        var encryptedString = KeyUtil.encryptString(context, userDataString, key)
        encryptedString?.let { encString ->
            context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putString(ENCRYPTED_USER_DATA, encryptedString).apply()
            return true
        }
        return false

        //RSA
//        val rsaBytes = encryptWithRsa(RSA_USER_KEY, userDataString)
//        if (rsaBytes != null) {
//            val rsaB64 = byteArrayToBase64(rsaBytes)
//            context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
//                .edit().putString(RSA_USER_DATA, rsaB64).apply()
//            return true
//        }
//        return false
    }

    fun getEncryptedUserData(context: Context): RegisterUserData? {
        val gson = Gson()
        val encryptedString: String = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(ENCRYPTED_USER_DATA, "") ?: ""
        var userData: RegisterUserData? = null
        if (encryptedString.isNotEmpty()) {
            CentralLog.d("Preference", "AES Encrypted User String: $encryptedString")
            //To keep it consistent for users who already are using the USER key in production
            //For Android 5.1, will use the new key instead which is PHONE_NUMBER_KEY
            val key =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) USER_KEY else PHONE_NUMBER_KEY
            var decryptedString = KeyUtil.decryptString(context, encryptedString, key)
            userData = gson.fromJson(decryptedString, RegisterUserData::class.java)
            CentralLog.d("Preference", "Encrypted User Data retrieved: $userData")
        }
        return userData
    }

    //temp cache of value

    var userIdType: String? = null
    fun getUserIdentityType(context: Context) : String {

        var idType = IdentityType.findByValue(userIdType)

        if( idType == IdentityType.ERROR){
            idType = IdentityType.findByValue(getEncryptedUserData(context)?.idType)
        }


        //override identity type here if you want. just uncomment the line needed
//        idType = IdentityType.ERROR
//        idType = IdentityType.PASSPORT

        userIdType = idType.tag
        return idType.tag
    }

    fun putPreferredLanguageCode(context: Context, languageCode: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(PREFERRED_LANGUAGE, languageCode).apply()
    }

    fun getPreferredLanguageCode(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(PREFERRED_LANGUAGE, "en") ?: "en"
    }

    fun getAppVersion(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(APP_VERSION, "") ?: ""
    }

    fun getHasSeenSafeEntryDialog(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(HAS_SEEN_SAFE_ENTRY_DIALOG, false)
    }

    fun putHasSeenSafeEntryDialog(context: Context, hasSeen: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(HAS_SEEN_SAFE_ENTRY_DIALOG, hasSeen).apply()
    }

    fun putAppVersion(context: Context, appVersion: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(APP_VERSION, appVersion).apply()
    }

    fun setHasSeenHowToUse(context: Context, isShown: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(LAST_SHOWCASE_HOW_TO_USE, isShown).apply()
    }

    fun hasSeenHowToUse(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(LAST_SHOWCASE_HOW_TO_USE, false)
    }

    fun getLastAppUpdatedShown(context: Context): Float {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getFloat(LAST_APP_UPDATED_SHOWN, 0f)
    }

    fun putLastAppUpdatedShown(context: Context, lastAppUpdated: Float) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit()
            .putFloat(LAST_APP_UPDATED_SHOWN, lastAppUpdated)
            .apply()
    }

    fun putCameraPermRequestedFlag(context: Context, requested: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(CAMERA_PERM_REQUESTED, requested).apply()
    }

    fun getCameraPermRequestedFlag(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(CAMERA_PERM_REQUESTED, false)
    }

    fun putAnnouncementVersion(context: Context, version: Int) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putInt(ANNOUNCEMENT_VERSION, version).apply()
    }

    fun getAnnouncementVersion(context: Context): Int {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getInt(ANNOUNCEMENT_VERSION, 0)
    }

    fun setAnnouncementSeen(context: Context, boolean: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(ANNOUNCEMENT_SEEN, boolean).apply()
    }

    fun getAnnouncementSeen(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(ANNOUNCEMENT_SEEN, false)
    }

    fun isFavNew(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(IS_FAV_NEW, true)
    }

    fun putIsFavNew(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(IS_FAV_NEW, value).apply()
    }

    fun isManageFamilyMemNew(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(IS_MANAGE_FAMILY_MEM_NEW, true)
    }

    fun putIsManageFamilyMemNew(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(IS_MANAGE_FAMILY_MEM_NEW, value).apply()
    }

    fun putUserRegistrationDate(context: Context) {
        val currentDayLong  = DateTools.getStartOfDay(System.currentTimeMillis()).timeInMillis
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(USER_REGISTRATION_DATE, currentDayLong).apply()
    }

    fun getUserRegistrationDate(context: Context): Long {
        val regDate = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(USER_REGISTRATION_DATE, 0)
        return regDate

    }

    fun setLastSERefreshTime(context: Context, time: Long) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putLong(SE_LAST_REFRESH_TIME, time).apply()
    }

    fun getLastSERefreshTime(context: Context): Long {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getLong(SE_LAST_REFRESH_TIME, 0)
    }

    fun isSubmitErrorLogsNew(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(IS_SUBMIT_ERROR_LOG_NEW, true)
    }

    fun putIsSubmitErrorLogNew(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(IS_SUBMIT_ERROR_LOG_NEW, value).apply()
    }
    
    fun clearSharedPreferences(context: Context) {
        userIdType = null
        val savedLanguageCode = getPreferredLanguageCode(context)
        val savedAppVersion = getAppVersion(context)
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).edit().clear().apply()
        putPreferredLanguageCode(context, savedLanguageCode)
        putAppVersion(context, savedAppVersion)
        putCheckpoint(context, Fragments.VERIFY_NUMBER.id)

    }
    fun clearUserData(context: Context){
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE).edit()
            .remove(ENCRYPTED_USER_DATA).apply()
    }

    fun putPrivacyPolicyPolicyVersion(context: Context, date: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(PRIVACY_POLICY_POLICY_VERSION, date).apply()
    }

    fun getPrivacyPolicyPolicyVersion(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(PRIVACY_POLICY_POLICY_VERSION, "") ?: ""
    }

    fun putShouldShowPrivacyPolicy(context: Context, show: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(SHOULD_SHOW_PRIVACY_POLICY, show).apply()
    }

    fun shouldShowPrivacyPolicy(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(SHOULD_SHOW_PRIVACY_POLICY, false)
    }

    fun putConsentPrivacyPolicyApiSuccess(context: Context, successVersion: String) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putString(CONSENT_PRIVACY_POLICY_API_SUCCESS, successVersion).apply()
    }

    fun getConsentPrivacyPolicyApiSuccess(context: Context): String {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getString(CONSENT_PRIVACY_POLICY_API_SUCCESS, "") ?: ""
    }

    fun isHealthStatusNew(context: Context): Boolean {
        return context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(IS_HEALTH_STATUS_NEW, true)
    }

    fun putIsHealthStatusNew(context: Context, value: Boolean) {
        context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .edit().putBoolean(IS_HEALTH_STATUS_NEW, value).apply()
    }
    fun purgeUserDetaislData(context: Context){
        val isDataPurged = context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
            .getBoolean(IS_USER_DETAILS_PURGED, false)
        if(!isDataPurged){
            //decrypt the user data
            val userData = getEncryptedUserData(context)
            userData?.dateOfBirth = ""
            userData?.idDateOfIssue = ""
            userData?.cardSerialNumber = ""
            userData?.idDateOfApplication = ""
            //save user data
            if (userData != null) {
                saveEncryptedUserData(context,userData)
            }
            //once purged do not do again
            context.getSharedPreferences(PREF_ID, Context.MODE_PRIVATE)
                .edit().putBoolean(IS_USER_DETAILS_PURGED, true).apply()
        }
    }
}
