package sg.gov.tech.bluetrace.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import androidx.lifecycle.LifecycleService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import pub.devrel.easypermissions.EasyPermissions
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.bluetooth.BLEAdvertiser
import sg.gov.tech.bluetrace.bluetooth.gatt.*
import sg.gov.tech.bluetrace.idmanager.TempIDManager
import sg.gov.tech.bluetrace.idmanager.TemporaryID
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.notifications.NotificationTemplates
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import sg.gov.tech.bluetrace.permissions.RequestFileWritePermission
import sg.gov.tech.bluetrace.receivers.UnpauseAlarmReceiver
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import sg.gov.tech.bluetrace.scheduler.Scheduler
import sg.gov.tech.bluetrace.services.light.LightLifterService
import sg.gov.tech.bluetrace.status.Status
import sg.gov.tech.bluetrace.status.persistence.StatusRecord
import sg.gov.tech.bluetrace.status.persistence.StatusRecordStorage
import sg.gov.tech.bluetrace.streetpass.*
import sg.gov.tech.bluetrace.streetpass.persistence.*
import sg.gov.tech.revamp.requestModel.TempIdRequestModel
import java.lang.ref.WeakReference
import java.util.*
import kotlin.coroutines.CoroutineContext


class BluetoothMonitoringService : LifecycleService(), CoroutineScope {

    private var mNotificationManager: NotificationManager? = null

    private lateinit var serviceUUID: String
    private val apiHandler: ApiHandler by inject()
    private var streetPassServer: StreetPassServer? = null
    private var streetPassScanner: StreetPassScanner? = null
    private var streetPassLiteScanner: StreetPassLiteScanner? = null
    private var disposables = CompositeDisposable()
    private var advertiser: BLEAdvertiser? = null
    var worker: StreetPassWorker? = null
    private val streetPassReceiver = StreetPassReceiver()
    private val statusReceiver = StatusReceiver()
    private val bluetoothStatusReceiver = BluetoothStatusReceiver()
    private val streetPassLiteReceiver = StreetPassLiteRecordReceiver()

    private lateinit var streetPassRecordStorage: StreetPassRecordStorage
    private lateinit var safeEntryRecordStorage: SafeEntryRecordStorage
    private lateinit var statusRecordStorage: StatusRecordStorage

    private lateinit var newQueue: RequestQueue
    private var job: Job = Job()

    private lateinit var functions: FirebaseFunctions

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var commandHandler: CommandHandler

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth

    private var notificationShown: NOTIFICATION_STATE? = null
    private var wakelock: PowerManager.WakeLock? = null
    private var startDisposables = CompositeDisposable()
    private var btScanDisposables = CompositeDisposable()
    private var btlScanDisposables = CompositeDisposable()
    private var updateBmDisposables = CompositeDisposable()


    private fun loadLocale() {
        val preferredLanguage = Preference.getPreferredLanguageCode(this)
//        setLocale(preferredLanguage)
        fixLocale(preferredLanguage)
    }

    private fun fixLocale(lang: String) {
        val mBackedUpLocale = Locale(lang)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val resources: Resources = resources
            val config: Configuration = resources.configuration
            if (null != mBackedUpLocale && !config.locales.get(0).equals(mBackedUpLocale)) {
                Locale.setDefault(mBackedUpLocale)
                val newConfig = Configuration(config)
                newConfig.setLocale(
                    Locale(
                        mBackedUpLocale.language,
                        mBackedUpLocale.country
                    )
                )
                resources.updateConfiguration(newConfig, null)
            }
            // Also this must be overridden, otherwise for example when opening a dialog the title could have one language and the content other, because
            // different contexts are used to get the resources.
            val appResources: Resources = applicationContext.resources
            val appConfig: Configuration = appResources.configuration
            if (null != mBackedUpLocale && !appConfig.locales.get(0).equals(mBackedUpLocale)) {
                Locale.setDefault(mBackedUpLocale)
                val newConfig = Configuration(appConfig)
                newConfig.setLocale(
                    Locale(
                        mBackedUpLocale.language,
                        mBackedUpLocale.country
                    )
                )
                appResources.updateConfiguration(newConfig, null)
            }
        }
    }

    private var sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Preference.PREFERRED_LANGUAGE -> {

                    val lang = Preference.getPreferredLanguageCode(this)
                    loadLocale()

                    //update notification
                    when (notificationShown) {
                        NOTIFICATION_STATE.PAUSED_BY_USER -> {
                            notifyUserPaused(true)
                        }

                        NOTIFICATION_STATE.RUNNING -> {
                            notifyRunning(true)
                        }

                        NOTIFICATION_STATE.LACKING_THINGS -> {
                            notifyLackingThings(true)
                        }

                        else -> {

                        }
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        CentralLog.i(TAG, "Oncreate in BTMS")
        getWakeLock()
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        loadLocale()
        setup()
        LightLifterService.scheduleMetricUpload(this)
        LightLifterService.schedulePurge(this)
        LightLifterService.scheduleLogging(this)
    }

    private fun getWakeLock() {
        //safety net - do not accidentally roll out wakelock to production
        if (BuildConfig.WAKE_LOCK && BuildConfig.DEBUG) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakelock == null) {
                wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TT:BMS")
                wakelock?.acquire()
            }
        }
    }

    fun setup() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

        CentralLog.setPowerManager(pm)

        commandHandler = CommandHandler(WeakReference(this))

        CentralLog.d(TAG, "Creating service - BluetoothMonitoringService")
        serviceUUID = BuildConfig.BLE_SSID

        worker = StreetPassWorker(this.applicationContext)

        unregisterReceivers()
        registerReceivers()

        safeEntryRecordStorage = SafeEntryRecordStorage(this.applicationContext)
        streetPassRecordStorage = StreetPassRecordStorage(this.applicationContext)
        statusRecordStorage = StatusRecordStorage(this.applicationContext)

        newQueue = Volley.newRequestQueue(this)

        setupNotifications()
        functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
        broadcastMessage = TempIDManager.retrieveTemporaryID(this.applicationContext)

    }

    fun teardown() {
        advertiser?.stopAdvertising()
        advertiser = null

        streetPassServer?.tearDown()
        streetPassServer = null

        if (BuildConfig.SCAN_BT) {
            streetPassScanner?.stopScan()
            streetPassScanner = null
        }

        if (BuildConfig.SCAN_BTL) {
            streetPassLiteScanner?.stopScan()
            streetPassLiteScanner = null
        }

        commandHandler.removeCallbacksAndMessages(null)

        Utils.cancelBMUpdateCheck(this.applicationContext)
        Utils.cancelNextHealthCheck(this.applicationContext)
    }

    private fun setupNotifications() {

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_SERVICE
            // Create the channel for the notification
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            mChannel.enableLights(false)
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(0L)
            mChannel.setSound(null, null)
            mChannel.setShowBadge(false)

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
    }

    private fun notifyLackingThings(override: Boolean = false) {
        if (notificationShown != NOTIFICATION_STATE.LACKING_THINGS || override) {
            var notif =
                NotificationTemplates.lackingThingsNotification(this, CHANNEL_ID)
            startForeground(NOTIFICATION_ID, notif)
            notificationShown = NOTIFICATION_STATE.LACKING_THINGS
        }
    }

    private fun notifyRunning(override: Boolean = false) {
        if (notificationShown != NOTIFICATION_STATE.RUNNING || override) {
            var notif =
                NotificationTemplates.getRunningNotification(this, CHANNEL_ID)
            startForeground(NOTIFICATION_ID, notif)
            notificationShown = NOTIFICATION_STATE.RUNNING
        }
    }

    private fun notifyUserPaused(override: Boolean = false) {
        if (notificationShown != NOTIFICATION_STATE.PAUSED_BY_USER || override) {
            var notif =
                NotificationTemplates.getUserPausedNotification(this, CHANNEL_ID)
            startForeground(NOTIFICATION_ID, notif)
            notificationShown = NOTIFICATION_STATE.PAUSED_BY_USER
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val perms = Utils.getRequiredPermissions()
        return EasyPermissions.hasPermissions(this.applicationContext, *perms)
    }

    private fun hasWritePermissions(): Boolean {
        val perms = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return EasyPermissions.hasPermissions(this.applicationContext, *perms)
    }

    private fun acquireWritePermission() {
        val intent = Intent(this.applicationContext, RequestFileWritePermission::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun isBluetoothEnabled(): Boolean {
        var btOn = false
        val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }

        bluetoothAdapter?.let {
            btOn = it.isEnabled
        }
        return btOn
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        CentralLog.i(TAG, "BMS Service onStartCommand")

        var commandToRun = Command.INVALID.index
        intent?.let {
            commandToRun = intent.getIntExtra(COMMAND_KEY, Command.INVALID.index)
        }

        showNotificationBasedOnStatus(false, Command.findByValue(commandToRun))

        //check paused
        if (Preference.shouldBePaused(this)) {
            //if the command is not updating pause, remain paused.
            if (Command.findByValue(commandToRun) != Command.ACTION_USER_PAUSE) {
                return START_STICKY
            }
        }

        //check for permissions
        if ((!hasLocationPermissions() || !isBluetoothEnabled()) && Command.findByValue(commandToRun) != Command.ACTION_USER_PAUSE) {
            CentralLog.i(
                TAG,
                "location permission: ${hasLocationPermissions()} bluetooth: ${isBluetoothEnabled()}"
            )
            return START_STICKY
        }

        //check for write permissions  - not required for now. SDLog maybe?
        //only required for debug builds - for now
        if (BuildConfig.DEBUG) {
            if (!hasWritePermissions()) {
                CentralLog.i(TAG, "no write permission")
                //start write permission activity
                acquireWritePermission()
                stopSelf()
                return START_STICKY
            }
        }

        intent?.let {
            val cmd = intent.getIntExtra(COMMAND_KEY, Command.INVALID.index)
            runService(Command.findByValue(cmd), intent)
            return START_STICKY
        }

        if (intent == null) {
            val loggerTAG =
                "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
            CentralLog.e(TAG, "WTF? Nothing in intent @ onStartCommand")
            DBLogger.e(
                DBLogger.LogType.BLUETRACE,
                loggerTAG,
                "WTF? Nothing in intent @ onStartCommand",
                null
            )
            commandHandler.startBluetoothMonitoringService()
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_STICKY
    }

    private fun showNotificationBasedOnStatus(shouldOverwrite: Boolean, cmd: Command?) {
        if (Preference.shouldBePaused(this) && Command.ACTION_USER_PAUSE != cmd) {
            CentralLog.d(TAG, "Status: App is paused")
            notifyUserPaused(shouldOverwrite)
        } else if (!hasLocationPermissions()) {
            CentralLog.d(TAG, "Status: Dont have location permission")
            notifyLackingThings(shouldOverwrite)
        } else if (!isBluetoothEnabled()) {
            CentralLog.d(TAG, "Status: Bluetooth is not enabled")
            notifyLackingThings(shouldOverwrite)
        } else {
            CentralLog.d(TAG, "Status: App is running well")
            notifyRunning(shouldOverwrite)
        }
    }

    fun runService(cmd: Command, commandIntent: Intent?) {
        CentralLog.i(TAG, "Running command via runService: ${cmd.string}")
        showNotificationBasedOnStatus(false, cmd)

        //override Pause if command is pause
        //do pause here
        if (Preference.shouldBePaused(this) && Command.ACTION_USER_PAUSE != cmd) {
            return
        }

        //check for permissions
        if ((!hasLocationPermissions() || !isBluetoothEnabled()) && Command.ACTION_USER_PAUSE != cmd) {
            CentralLog.i(
                TAG,
                "location permission: ${hasLocationPermissions()} bluetooth: ${isBluetoothEnabled()}"
            )
            return
        }

        //check for write permissions  - not required for now. SDLog maybe?
        //only required for debug builds - for now
        if (BuildConfig.DEBUG) {
            if (!hasWritePermissions()) {
                CentralLog.i(TAG, "no write permission")
                //start write permission activity
                acquireWritePermission()
                stopSelf()
                return
            }
        }

        when (cmd) {
            Command.ACTION_START -> {
                setupService()
                Utils.scheduleNextHealthCheck(this.applicationContext, healthCheckInterval)
                Utils.scheduleBMUpdateCheck(this.applicationContext, bmCheckInterval)
                actionStart()
            }

            Command.ACTION_BT_SCAN -> {
                scheduleBTScan()
                actionBTScan()
            }

            Command.ACTION_BTL_SCAN -> {
                scheduleBTLScan()
                actionBTLScan()
            }

            Command.ACTION_ADVERTISE -> {
                scheduleAdvertisement()
                actionAdvertise()
            }

            Command.ACTION_UPDATE_BM -> {
                Utils.scheduleBMUpdateCheck(this.applicationContext, bmCheckInterval)
                actionUpdateBm()
            }

            Command.ACTION_STOP -> {
                actionStop()
            }

            Command.ACTION_SELF_CHECK -> {
                Utils.scheduleNextHealthCheck(this.applicationContext, healthCheckInterval)
                actionHealthCheck()
            }

            Command.ACTION_USER_PAUSE -> {
                CentralLog.d(TAG, "BMS action user pause")
                commandIntent?.let {
                    val pauseUntil = it.getLongExtra(COMMAND_ARGS, 0)

                    if (pauseUntil >= 0) {
                        //only pause if it is in future
                        if (pauseUntil > System.currentTimeMillis()) {
                            CentralLog.i(TAG, "Pausing until: ${Utils.getDate(pauseUntil)}")

                            Preference.putPauseUntil(TracerApp.AppContext, pauseUntil)
                            //schedule unpause
                            val unpauseIntent = Intent(this, UnpauseAlarmReceiver::class.java)
                            unpauseIntent.action = ACTION_UNPAUSE

                            Scheduler.scheduleExact(
                                PENDING_UNPAUSE,
                                TracerApp.AppContext,
                                unpauseIntent,
                                pauseUntil
                            )

                            notifyUserPaused()
                            teardown()
                        }
                    }
                    //-ve - implicit unpause command
                    else {
                        Preference.putPauseUntil(TracerApp.AppContext, pauseUntil)
                        Utils.startBluetoothMonitoringService(this)
                    }
                }
            }

            else -> CentralLog.i(TAG, "Invalid / ignored command: $cmd. Nothing to do")
        }
    }

    private fun getTempIDs(onComplete: (ApiResponseModel<out Any>) -> Unit): Disposable {
        val loggerTAG = "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
        val tempIdRequestData = TempIdRequestModel.getTempIdRequestData(this)
        var result = apiHandler.getTempID(tempIdRequestData)
        val disposable = result.subscribeWith(object :
            DisposableSingleObserver<ApiResponseModel<out Any>>() {
            override fun onError(e: Throwable) {
                CentralLog.e(TAG, "getTempId call failed")
                DBLogger.e(
                    DBLogger.LogType.USERDATAREGISTERATION,
                    loggerTAG,
                    "Failed to get tempId: ${e.message}",
                    null
                )
                onComplete.invoke(ApiResponseModel(false, e.message))
//                disposables.clear()
            }

            override fun onSuccess(data: ApiResponseModel<out Any>) {
                onComplete.invoke(data)
//                disposables.clear()
            }
        })
//        disposables.addAll(disposable)

        return disposable
    }

    private fun actionStop() {
        stopForeground(true)
        stopSelf()
        CentralLog.w(TAG, "Service Stopping")
    }

    private fun actionHealthCheck() {
        performUserLoginCheck()
        performHealthCheck()
    }

    private fun actionStart() {
        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionStart")
            var disposable: Disposable? = null
            disposable = getTempIDs {
                CentralLog.d(TAG, "Get TemporaryIDs completed in actionStart")
                //this will run whether it starts or fails.
                var fetch = TempIDManager.retrieveTemporaryID(this.applicationContext)
                fetch?.let {
                    broadcastMessage = it
                    setupCycles()
                }
                if (fetch == null) {
                    val loggerTAG =
                        "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
                    CentralLog.e(TAG, "[TempID] Failed to fetch new Temp ID in actionStart")
                    DBLogger.e(
                        DBLogger.LogType.BLUETRACELITE,
                        loggerTAG,
                        "[TempID] Failed to fetch new Temp ID in actionStart",
                        null
                    )
                }

                CentralLog.e(TAG, "disposing! - start")
                disposable?.dispose()
            }

        } else {
            setupCycles()
            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionStart")
        }
    }

    private fun actionUpdateBm() {
        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionUpdateBM")
            var disposable: Disposable? = null
            disposable = getTempIDs {
                CentralLog.d(TAG, "Get TemporaryIDs completed in actionUpdateBM")
                //this will run whether it starts or fails.
                var fetch = TempIDManager.retrieveTemporaryID(this.applicationContext)
                fetch?.let {
                    CentralLog.i(TAG, "[TempID] Updated Temp ID")
                    broadcastMessage = it
                }

                if (fetch == null) {
                    val loggerTAG =
                        "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
                    CentralLog.e(TAG, "[TempID] Failed to fetch new Temp ID in actionUpdateBm")
                    DBLogger.e(
                        DBLogger.LogType.BLUETRACELITE,
                        loggerTAG,
                        "[TempID] Failed to fetch new Temp ID in actionUpdateBm",
                        null
                    )
                }
                CentralLog.e(TAG, "disposing! - update")
                disposable?.dispose()
            }

        } else {
            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionUpdateBM")
        }
    }

    fun calcPhaseShift(min: Long, max: Long): Long {
        return (min + (Math.random() * (max - min))).toLong()
    }

    private fun actionBTScan() {
        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionBTScan")
            var disposable: Disposable? = null
            disposable = getTempIDs {
                CentralLog.d(TAG, "Get TemporaryIDs completed in actionBTScan")
                //this will run whether it starts or fails.
                var fetch = TempIDManager.retrieveTemporaryID(this.applicationContext)
                fetch?.let {
                    broadcastMessage = it
                    performBTScan()
                }

                if (fetch == null) {
                    val loggerTAG =
                        "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
                    CentralLog.e(TAG, "[TempID] Failed to fetch new Temp ID in actionBTScan")
                    DBLogger.e(
                        DBLogger.LogType.BLUETRACELITE,
                        loggerTAG,
                        "[TempID] Failed to fetch new Temp ID in actionBTScan",
                        null
                    )
                }
                CentralLog.e(TAG, "disposing! - bt")
                disposable?.dispose()
            }
        } else {
            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionBTScan")
            performBTScan()
        }
    }

    private fun actionBTLScan() {
        if (TempIDManager.needToUpdate(this.applicationContext) || broadcastMessage == null) {
            CentralLog.i(TAG, "[TempID] Need to update TemporaryID in actionBTLScan")
            var disposable: Disposable? = null
            disposable = getTempIDs {
                CentralLog.d(TAG, "Get TemporaryIDs completed in actionBTLScan")
                //this will run whether it starts or fails.
                var fetch = TempIDManager.retrieveTemporaryID(this.applicationContext)
                fetch?.let {
                    broadcastMessage = it
                }

                if (fetch == null) {
                    val loggerTAG =
                        "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
                    CentralLog.e(TAG, "[TempID] Failed to fetch new Temp ID in actionBTLScan")
                    DBLogger.e(
                        DBLogger.LogType.BLUETRACELITE,
                        loggerTAG,
                        "[TempID] Failed to fetch new Temp ID in actionBTLScan",
                        null
                    )
                }

                performBTLScan()

                CentralLog.e(TAG, "disposing! - btl")
                disposable?.dispose()
            }

            btlScanDisposables.add(disposable)


        } else {
            CentralLog.i(TAG, "[TempID] Don't need to update Temp ID in actionBTLScan")
            performBTLScan()
        }
    }

    private fun actionAdvertise() {
        setupAdvertiser()
        if (isBluetoothEnabled()) {
            advertiser?.startAdvertising(BuildConfig.ADVERTISING_DURATION)
        } else {
            CentralLog.w(TAG, "Unable to start advertising, bluetooth is off")
        }
    }

    private fun setupService() {
        streetPassServer =
            streetPassServer ?: StreetPassServer(this.applicationContext, serviceUUID)
        setupBTScanner()
        setupBTLScanner()
        setupAdvertiser()
    }

    private fun setupBTScanner() {
        streetPassScanner = streetPassScanner ?: StreetPassScanner(
            this,
            scanDuration
        )
    }

    private fun setupBTLScanner() {
        streetPassLiteScanner =
            streetPassLiteScanner ?: StreetPassLiteScanner(this, btlScanDuration)
    }

    private fun setupAdvertiser() {
        advertiser = advertiser ?: BLEAdvertiser()
    }

    private fun setupCycles() {
        //TODO - should only setup if not present
        setupScanCycles()
        setupAdvertisingCycles()
    }

    private fun setupScanCycles() {
        commandHandler.scheduleNextBTScan(0)
        commandHandler.scheduleNextBTLScan(0)
    }

    private fun setupAdvertisingCycles() {
        commandHandler.scheduleNextAdvertise(0)
    }

    private fun performBTScan() {
        setupBTScanner()
        startBTScan()
    }

    private fun performBTLScan() {
        setupBTLScanner()
        startBTLScan()
    }

    private fun scheduleBTScan() {
        commandHandler.scheduleNextBTScan(
            scanDuration + calcPhaseShift(
                minScanInterval,
                maxScanInterval
            )
        )
    }

    private fun scheduleBTLScan() {
        commandHandler.scheduleNextBTLScan(
            btlScanDuration + calcPhaseShift(
                btlMinScanInterval,
                btlMaxScanInterval
            )
        )
    }

    private fun scheduleAdvertisement() {
        commandHandler.scheduleNextAdvertise(BuildConfig.ADVERTISING_DURATION + BuildConfig.ADVERTISING_INTERVAL)
    }

    private fun startBTScan() {

        if (isBluetoothEnabled()) {
            if (BuildConfig.SCAN_BT) {
                streetPassScanner?.let { scanner ->
                    if (!scanner.isScanning()) {
                        scanner.startScan()
                    } else {
                        val loggerTAG =
                            "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"

                        CentralLog.e(TAG, "BT Already scanning!")
                        DBLogger.e(
                            DBLogger.LogType.BLUETRACE,
                            loggerTAG,
                            "BT Already scanning!",
                            null
                        )
                    }
                }
            }

        } else {
            CentralLog.w(TAG, "Unable to start BT scan - bluetooth is off")
        }
    }

    private fun startBTLScan() {

        if (isBluetoothEnabled()) {

            if (BuildConfig.SCAN_BTL) {
                streetPassLiteScanner?.let { scanner ->
                    if (!scanner.isScanning()) {
                        scanner.startScan()
                    } else {
                        val loggerTAG =
                            "${javaClass.simpleName} -> ${object {}.javaClass.enclosingMethod?.name}"
                        CentralLog.e(TAG, "BTL Already scanning!")
                        DBLogger.e(
                            DBLogger.LogType.BLUETRACELITE,
                            loggerTAG,
                            "BTL Already scanning!",
                            null
                        )
                    }
                }
            }

        } else {
            CentralLog.w(TAG, "Unable to start BTL scan - bluetooth is off")
        }
    }

    private fun performUserLoginCheck() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(applicationContext)
        auth = FirebaseAuth.getInstance()
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser == null && Preference.isOnBoarded(this)) {
            CentralLog.d(TAG, "User is not login but has completed onboarding")
            val bundle = Bundle()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Android")
            bundle.putString(
                FirebaseAnalytics.Param.ITEM_NAME,
                "Have not login yet but in main activity"
            )
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
        }
    }

    private fun performHealthCheck() {

        showNotificationBasedOnStatus(true, null)
        CentralLog.i(TAG, "Performing self diagnosis")

        if (!hasLocationPermissions() || !isBluetoothEnabled()) {
            CentralLog.i(TAG, "no location permission")
            return
        }

        notifyRunning(true)

        //ensure our service is there
        setupService()

        if (!commandHandler.hasBTScanScheduled()) {
            CentralLog.w(TAG, "Missing BT Scan Schedule - rectifying")
            commandHandler.scheduleNextBTScan(100)
        } else {
            CentralLog.w(TAG, "BT Scan Schedule present")
        }

        if (!commandHandler.hasBTLScanScheduled()) {
            CentralLog.w(TAG, "Missing BTL Scan Schedule - rectifying")
            commandHandler.scheduleNextBTLScan(100)
        } else {
            CentralLog.w(TAG, "BTL Scan Schedule present")
        }

        if (!commandHandler.hasAdvertiseScheduled()) {
            CentralLog.w(TAG, "Missing Advertise Schedule - rectifying")
            commandHandler.scheduleNextAdvertise(100)
        } else {
            CentralLog.w(
                TAG,
                "Advertise Schedule present. Should be advertising?:  ${advertiser?.shouldBeAdvertising()
                    ?: false}."
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startDisposables.dispose()
        btScanDisposables.dispose()
        btlScanDisposables.dispose()
        updateBmDisposables.dispose()
        wakelock?.release()
        CentralLog.i(TAG, "BluetoothMonitoringService destroyed - tearing down")
        stopService()
        CentralLog.i(TAG, "BluetoothMonitoringService destroyed")
    }

    private fun stopService() {
        teardown()
        unregisterReceivers()

        worker?.terminateConnections()
        worker?.unregisterReceivers()

        job.cancel()
    }


    private fun registerReceivers() {
        val recordAvailableFilter = IntentFilter(ACTION_RECEIVED_STREETPASS)
        localBroadcastManager.registerReceiver(streetPassReceiver, recordAvailableFilter)

        val statusReceivedFilter = IntentFilter(ACTION_RECEIVED_STATUS)
        localBroadcastManager.registerReceiver(statusReceiver, statusReceivedFilter)

        val bluetoothStatusReceivedFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStatusReceiver, bluetoothStatusReceivedFilter)

        localBroadcastManager.registerReceiver(
            streetPassLiteReceiver, IntentFilter(
                ACTION_RECEIVED_STREETPASS_LITE
            )
        )

        CentralLog.i(TAG, "Receivers registered")

        Preference.registerListener(this, sharedPreferenceChangeListener)
    }

    private fun unregisterReceivers() {
        try {
            localBroadcastManager.unregisterReceiver(streetPassReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "streetPassReceiver is not registered?")
        }

        try {
            localBroadcastManager.unregisterReceiver(statusReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "statusReceiver is not registered?")
        }

        try {
            localBroadcastManager.unregisterReceiver(streetPassLiteReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "statusReceiver is not registered?")
        }

        try {
            unregisterReceiver(bluetoothStatusReceiver)
        } catch (e: Throwable) {
            CentralLog.w(TAG, "bluetoothStatusReceiver is not registered?")
        }

        try {
            Preference.unregisterListener(this, sharedPreferenceChangeListener)
        } catch (e: Exception) {
            CentralLog.w(TAG, "preference listener is not registered?")
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    inner class BluetoothStatusReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = intent.action
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    var state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)

                    when (state) {
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_OFF")
                            if (!Preference.shouldBePaused(this@BluetoothMonitoringService)) {
                                notifyLackingThings()
                                teardown()
                            }
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_OFF")
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_TURNING_ON")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            CentralLog.d(TAG, "BluetoothAdapter.STATE_ON")
                            if (!Preference.shouldBePaused(this@BluetoothMonitoringService)) {
                                Utils.startBluetoothMonitoringService(this@BluetoothMonitoringService.applicationContext)
                            }
                        }
                    }
                }
            }
        }
    }

    inner class StreetPassReceiver : BroadcastReceiver() {

        private val TAG = "StreetPassReceiver"

        override fun onReceive(context: Context, intent: Intent) {

            if (ACTION_RECEIVED_STREETPASS == intent.action) {
                var connRecord: ConnectionRecord = intent.getParcelableExtra(STREET_PASS)
                CentralLog.d(
                    TAG,
                    "StreetPass received: $connRecord"
                )

                if (connRecord.msg.isNotEmpty()) {
                    val record = StreetPassRecord(
                        v = connRecord.version,
                        msg = connRecord.msg,
                        org = connRecord.org,
                        modelP = connRecord.peripheral.modelP,
                        modelC = connRecord.central.modelC,
                        rssi = connRecord.rssi,
                        txPower = connRecord.txPower
                    )

                    launch {
                        CentralLog.d(
                            TAG,
                            "Coroutine - Saving StreetPassRecord: ${Utils.getDate(record.timestamp)}"
                        )
                        streetPassRecordStorage.saveRecord(record)
                    }
                }
            }
        }
    }

    inner class StatusReceiver : BroadcastReceiver() {
        private val TAG = "StatusReceiver"

        override fun onReceive(context: Context, intent: Intent) {

            if (ACTION_RECEIVED_STATUS == intent.action) {
                var statusRecord: Status = intent.getParcelableExtra(STATUS)
                CentralLog.d(TAG, "Status received: ${statusRecord.msg}")

                if (statusRecord.msg.isNotEmpty()) {
                    val statusRecord = StatusRecord(statusRecord.msg)
                    launch {
                        statusRecordStorage.saveRecord(statusRecord)
                    }
                }
            }
        }
    }

    inner class StreetPassLiteRecordReceiver : BroadcastReceiver() {

        private val TAG = "BTL-Receiver"

        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_RECEIVED_STREETPASS_LITE == intent.action) {
                var connRecord: ConnectionRecord = intent.getParcelableExtra(STREET_PASS)
                CentralLog.d(
                    TAG,
                    "StreetPassLite received: $connRecord"
                )

                if (connRecord.msg.isNotEmpty()) {
                    launch {

                        val record = StreetPassLiteRecord(
                            connRecord.msg,
                            connRecord.rssi,
                            connRecord.txPower
                        )

                        CentralLog.d(
                            TAG,
                            "Coroutine - Saving StreetPassLiteRecord BTL: ${Utils.getDate(record.timestamp)}"
                        )
                        StreetPassRecordDatabase.getDatabase(context).bleRecordDao().insert(record)
                    }
                }
            }
        }
    }


    enum class Command(val index: Int, val string: String) {
        //do not change the numbers
        INVALID(-1, "INVALID"),
        ACTION_START(0, "START"),
        ACTION_SCAN(1, "SCAN"),
        ACTION_STOP(2, "STOP"),
        ACTION_ADVERTISE(3, "ADVERTISE"),
        ACTION_SELF_CHECK(4, "SELF_CHECK"),
        ACTION_UPDATE_BM(5, "UPDATE_BM"),

        //        ACTION_PAUSE_FOR_REDACTED(6, "PAUSE_FOR_[REDACTED]"),
        ACTION_PURGE(7, "PURGE"),
        ACTION_USER_PAUSE(8, "PAUSE_BY_USER"),
        ACTION_BT_SCAN(9, "BT_SCAN"),
        ACTION_BTL_SCAN(10, "BTL_SCAN");

        companion object {
            private val types = values().associate { it.index to it }
            fun findByValue(value: Int) = types[value] ?: INVALID
        }
    }

    enum class NOTIFICATION_STATE {
        RUNNING,
        LACKING_THINGS,
        PAUSED_BY_USER
    }

    companion object {

        private val TAG = "BTMService"

        private val NOTIFICATION_ID = BuildConfig.SERVICE_FOREGROUND_NOTIFICATION_ID
        private val CHANNEL_ID = BuildConfig.SERVICE_FOREGROUND_CHANNEL_ID
        val CHANNEL_SERVICE = BuildConfig.SERVICE_FOREGROUND_CHANNEL_NAME

        val PUSH_NOTIFICATION_ID = BuildConfig.PUSH_NOTIFICATION_ID

        val COMMAND_KEY = "${BuildConfig.APPLICATION_ID}_CMD"
        val COMMAND_ARGS = "${BuildConfig.APPLICATION_ID}_ARG"


        val PENDING_ACTIVITY = 5
        val PENDING_START = 6
        val PENDING_BT_SCAN_REQ_CODE = 7
        val PENDING_BTL_SCAN_REQ_CODE = 8
        val PENDING_ADVERTISE_REQ_CODE = 9
        val PENDING_HEALTH_CHECK_CODE = 10
        val PENDING_WIZARD_REQ_CODE = 11
        val PENDING_BM_UPDATE = 12
        val PENDING_PURGE_CODE = 13
        val PENDING_UNPAUSE = 14

        const val ACTION_UNPAUSE = "sg.gov.tech.bluetrace.ACTION_UNPAUSE"

        var broadcastMessage: TemporaryID? = null

        //should be more than advertising gap?
        val scanDuration: Long = BuildConfig.SCAN_DURATION
        val minScanInterval: Long = BuildConfig.MIN_SCAN_INTERVAL
        val maxScanInterval: Long = BuildConfig.MAX_SCAN_INTERVAL

        val btlScanDuration: Long = BuildConfig.BTL_SCAN_DURATION
        val btlMinScanInterval: Long = BuildConfig.BTL_MIN_SCAN_INTERVAL
        val btlMaxScanInterval: Long = BuildConfig.BTL_MAX_SCAN_INTERVAL

        val maxQueueTime: Long = BuildConfig.MAX_QUEUE_TIME
        val bmCheckInterval: Long = BuildConfig.BM_CHECK_INTERVAL
        val healthCheckInterval: Long = BuildConfig.HEALTH_CHECK_INTERVAL

        val connectionTimeout: Long = BuildConfig.CONNECTION_TIMEOUT

        val blacklistDuration: Long = BuildConfig.BLACKLIST_DURATION

        val useBlacklist = true
        val bmValidityCheck = false
    }

}
