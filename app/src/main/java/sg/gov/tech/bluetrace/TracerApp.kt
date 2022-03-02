package sg.gov.tech.bluetrace

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import sg.gov.tech.bluetrace.idmanager.TempIDManager
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.revamp.di.modules.apiModule
import sg.gov.tech.bluetrace.revamp.di.modules.dbModule
import sg.gov.tech.bluetrace.revamp.di.modules.utilsModule
import sg.gov.tech.bluetrace.revamp.di.modules.viewModelModule
import sg.gov.tech.bluetrace.services.BluetoothMonitoringService
import sg.gov.tech.bluetrace.streetpass.CentralDevice
import sg.gov.tech.bluetrace.streetpass.PeripheralDevice
import zendesk.core.AnonymousIdentity
import zendesk.core.Zendesk
import zendesk.support.Support

class TracerApp : Application() {

    private val ZENDESK_URL = BuildConfig.ZENDESK_URL
    private val ZENDESK_APP_ID = BuildConfig.ZENDESK_APP_ID
    private val ZENDESK_CLIENT_ID = BuildConfig.ZENDESK_CLIENT_ID

    override fun onCreate() {
        super.onCreate()
        AppContext = applicationContext
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        try { //Initialise Zendesk SDK
            Zendesk.INSTANCE.init(
                this,
                ZENDESK_URL,
                ZENDESK_APP_ID,
                ZENDESK_CLIENT_ID
            )

            Support.INSTANCE.init(Zendesk.INSTANCE)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        setupDynamicShortcuts(this)


        if (BuildConfig.DEBUG) {
//           DBMockDataHelper.saveMockHistory(applicationContext)

//            TTDatabaseCryptoManager.testEncryptionWithRSA(applicationContext,100)
//            TTDatabaseCryptoManager.testEncryptionWithAES(applicationContext,100)
//
//            TTDatabaseCryptoManager.testDecryptionWithRSA(applicationContext,100)
//            TTDatabaseCryptoManager.testDecryptionWithAES(applicationContext,100)

        }
        startKoin {
            androidContext(this@TracerApp)
            modules(listOf(apiModule, utilsModule, dbModule, viewModelModule))
        }
    }


    companion object {
        var mFirebaseAnalytics: FirebaseAnalytics? = null
        private val TAG = "TracerApp"
        const val ORG = BuildConfig.ORG

        lateinit var AppContext: Context

        fun thisDeviceMsg(): String {
            BluetoothMonitoringService.broadcastMessage?.let {
                CentralLog.i(TAG, "Retrieved BM for storage: $it")

                if (!it.isValidForCurrentTime()) {

                    var fetch = TempIDManager.retrieveTemporaryID(AppContext)
                    fetch?.let {
                        CentralLog.i(TAG, "Grab New Temp ID")
                        BluetoothMonitoringService.broadcastMessage = it
                    }

                    if (fetch == null) {
                        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                        CentralLog.e(loggerTAG, "Failed to grab new Temp ID")
                        DBLogger.e(
                            DBLogger.LogType.BLUETRACELITE,
                            loggerTAG,
                            "Failed to grab new Temp ID",
                            null
                        )
                    }
                }
            }
            return BluetoothMonitoringService.broadcastMessage?.tempID ?: "Missing TempID"
        }

        fun asPeripheralDevice(): PeripheralDevice {
            return PeripheralDevice(Build.MODEL, "SELF")
        }

        fun asCentralDevice(): CentralDevice {
            return CentralDevice(Build.MODEL, "SELF")
        }

        fun setUserIdentity(name: String?, email: String?) {
            val identity = AnonymousIdentity.Builder()
                .withNameIdentifier(name)
                .withEmailIdentifier(email)
                .build()
            Zendesk.INSTANCE.setIdentity(identity)
        }

        fun setupDynamicShortcuts(context: Context) {

            //cleanup existing shortcuts
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)

            //check if firebase user exists
            val user = FirebaseAuth.getInstance().currentUser

            if (user != null
                && Preference.onBoardedWithIdentity(context)
                && !RegisterUserData.isInvalidPassportOrInvalidUser(Preference.getUserIdentityType(context))
            ) {

                val intent = Intent(context, SafeEntryActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.action = Intent.ACTION_VIEW
                intent.putExtra(SafeEntryActivity.IS_FROM_SHORT_CUT, true)

                val shortcutSEQR =
                    ShortcutInfoCompat.Builder(context, "Enter Safely with SafeEntry")
                        .setShortLabel("SafeEntry")
                        .setLongLabel("Enter Safely with SafeEntry")
                        .setIcon(
                            IconCompat.createWithResource(
                                context,
                                R.drawable.ic_safeentry_logo_mark
                            )
                        )
                        .setIntent(intent)
                        .build()

                val dynamicShortcuts = arrayListOf<ShortcutInfoCompat>(shortcutSEQR)
                ShortcutManagerCompat.addDynamicShortcuts(context, dynamicShortcuts)
            }
        }
    }
}
