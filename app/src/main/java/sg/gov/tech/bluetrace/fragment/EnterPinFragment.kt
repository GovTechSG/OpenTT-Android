package sg.gov.tech.bluetrace.fragment

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_upload_enterpin.*
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.status.persistence.StatusRecordStorage
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassLiteRecordLiteStorage
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordStorage
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.HashMap

class EnterPinFragment : MainActivityFragment("EnterPinFragment") {
    private var TAG = "UploadFragment"

    private var disposeObj: Disposable? = null
    val firebaseAnalytics = FirebaseAnalytics.getInstance(TracerApp.AppContext)

    val pageSize = 50000

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_enterpin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_UPLOAD_ENTER_PIN)
        enterPinFragmentUploadCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                if (s.length == 6) {
                    Utils.hideKeyboardFrom(view.context, view)
                    enterPinActionButton.isEnabled = true

                    if(s.startsWith("999")){
                        tv_consent.setText(R.string.upload_cpc)
                    }
                    else{
                        tv_consent.setText(R.string.upload_agreement)
                    }
                }

                else if(s.length < 6){
                    enterPinActionButton.isEnabled = false

                    if(s.startsWith("999")){
                        tv_consent.setText(R.string.upload_cpc)
                    }
                    else{
                        tv_consent.setText(R.string.upload_agreement)
                    }
                }
            }
        })

        enterPinActionButton.setOnClickListener {
            enterPinFragmentErrorMessage.visibility = View.INVISIBLE
            val myParentFragment: UploadPageFragment = (parentFragment as UploadPageFragment)
            myParentFragment.turnOnLoadingProgress()

            val observableStreetRecords = Observable.create<File> {
                val dao = StreetPassRecordStorage(TracerApp.AppContext)

                var itemIndex = 0
                var rawPageResults = dao.getPagedRecords(pageSize, itemIndex)

                val dir = getUploadDir()
                val dirToUse = File(dir, "BlueTrace")
                dirToUse.mkdirs()
                while (rawPageResults.isNotEmpty()) {

                    val pageResults = rawPageResults.map {
                        it.timestamp = it.timestamp / 1000
                        return@map it
                    }
                    writeToInternalStorageModular(
                        dirToUse,
                        "${itemIndex}.json",
                        pageResults,
                        "records"
                    )

                    itemIndex += rawPageResults.size
                    rawPageResults = dao.getPagedRecords(pageSize, itemIndex)

                }

                it.onNext(dirToUse)
            }

            val observableStreetPassLiteRecords = Observable.create<File> {

                //loop through pages
                //onNext per page? per item?
                val dao = StreetPassLiteRecordLiteStorage(TracerApp.AppContext)
                var itemIndex = 0
                var rawPageResults = dao.getPagedRecords(pageSize, itemIndex)

                val dir = getUploadDir()
                val dirToUse = File(dir, "BlueTrace Lite")
                dirToUse.mkdirs()
                while (rawPageResults.isNotEmpty()) {

                    val pageResults = rawPageResults.map {
                        it.timestamp = it.timestamp / 1000
                        return@map it
                    }

                    writeToInternalStorageModular(
                        dirToUse,
                        "${itemIndex}.json",
                        pageResults,
                        "btLiteRecords"
                    )

                    itemIndex += rawPageResults.size
                    rawPageResults = dao.getPagedRecords(pageSize, itemIndex)

                }

                it.onNext(dirToUse)
            }

            val observableStatusRecords = Observable.create<File> {
                //loop through pages
                //onNext per page? per item?
                val dao = StatusRecordStorage(TracerApp.AppContext)
                var itemIndex = 0
                var rawPageResults = dao.getPagedRecords(pageSize, itemIndex)

                val dir = getUploadDir()
                val dirToUse = File(dir, "Status")
                dirToUse.mkdirs()

                while (rawPageResults.isNotEmpty()) {

                    val pageResults = rawPageResults.map {
                        it.timestamp = it.timestamp / 1000
                        return@map it
                    }

                    writeToInternalStorageModular(
                        dirToUse,
                        "${itemIndex}.json",
                        pageResults,
                        "status"
                    )

                    itemIndex += rawPageResults.size
                    rawPageResults = dao.getPagedRecords(pageSize, itemIndex)

                }

                it.onNext(dirToUse)
            }

            prepareUploadDir()

            disposeObj = Observable.zip(
                observableStreetRecords,
                observableStreetPassLiteRecords,
                observableStatusRecords,

                Function3<File, File, File, List<File>> { records, recordsLite, status ->
                    listOf(records, recordsLite, status)
                }
            )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe { exportedFiles ->

                    val uploadCode = enterPinFragmentUploadCode.text.toString()
                    getUploadToken(uploadCode)
                        .addOnSuccessListener {
                            val response = it.data as HashMap<String, String>
                            val uploadToken = response["token"]
                            if (uploadToken != null) {
                                CentralLog.d(TAG, "uploadToken: $uploadToken")
                                var fileToUpload: File? = null

                                try {

                                    val dir = getUploadDir()

                                    val date = Utils.getDateFromUnix(System.currentTimeMillis())
                                    val manufacturer = Build.MANUFACTURER
                                    val model = Build.MODEL

                                    val idFileName = "identity.json"

                                    val idFile = writeIdentityFile(
                                        TracerApp.AppContext,
                                        getUploadDir(),
                                        idFileName,
                                        uploadToken
                                    )
                                    val filesToWrite = exportedFiles.toMutableList()
                                    filesToWrite.add(idFile)


                                    val fileName =
                                        "StreetPassRecord_${manufacturer}_${model}_$date.zip"

                                    val zipFile = File(dir, fileName)
                                    zip(filesToWrite, zipFile, dir.absolutePath)
                                    fileToUpload = zipFile
                                } catch (e: Throwable) {
                                    val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                                    CentralLog.e(loggerTAG, "Failed to prep zip: ${e.localizedMessage}")
                                    DBLogger.e(
                                        DBLogger.LogType.UPLOAD,
                                        loggerTAG,
                                        "Failed to prep zip: ${e.localizedMessage}",
                                        DBLogger.getStackTraceInJSONArrayString(e as Exception)
                                    )
                                    e.printStackTrace()
                                    val auth = FirebaseAuth.getInstance()
                                    val currentUser: FirebaseUser? = auth.currentUser
                                    val bundle = Bundle()
                                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Android")
                                    bundle.putString(
                                        FirebaseAnalytics.Param.ITEM_NAME,
                                        "Unable to prepare file"
                                    )
                                    bundle.putString(AnalyticsKeys.REASON, e.message)

                                    currentUser?.uid?.let { uid ->
                                        bundle.putString(
                                            FirebaseAnalytics.Param.ITEM_CATEGORY,
                                            uid
                                        )
                                    }
                                    firebaseAnalytics.logEvent(AnalyticsKeys.UPLOAD_ERR, bundle)
                                    showUploadFailedError()
                                }

                                if (fileToUpload != null) {
                                    try {
                                        upload(fileToUpload, uploadCode.startsWith("999"))

//                                        CentralLog.d(TAG, "uploaded successfully")
//                                        val myParentFragment: UploadPageFragment =
//                                            (parentFragment as UploadPageFragment)
//                                        myParentFragment.turnOffLoadingProgress()
//                                        myParentFragment.navigateToUploadComplete(uploadCode.startsWith("999"))

                                    } catch (e: Exception) {
                                        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                                        //can fail to read / access the file
                                        CentralLog.e(loggerTAG, "ehh? ${e.message}")
                                        DBLogger.e(
                                            DBLogger.LogType.UPLOAD,
                                            loggerTAG,
                                            "ehh? ${e.message}",
                                            DBLogger.getStackTraceInJSONArrayString(e)
                                        )

                                        val auth = FirebaseAuth.getInstance()
                                        val currentUser: FirebaseUser? = auth.currentUser
                                        val bundle = Bundle()
                                        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Android")
                                        bundle.putString(
                                            FirebaseAnalytics.Param.ITEM_NAME,
                                            "Unable to upload file"
                                        )
                                        bundle.putString(AnalyticsKeys.REASON, e.message)

                                        currentUser?.uid?.let { uid ->
                                            bundle.putString(
                                                FirebaseAnalytics.Param.ITEM_CATEGORY,
                                                uid
                                            )
                                        }
                                        firebaseAnalytics.logEvent(AnalyticsKeys.UPLOAD_ERR, bundle)
                                        showUploadFailedError()
                                    }
                                }

                            } else {
                                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                                CentralLog.e(loggerTAG, "Failed to upload data: no upload token")
                                DBLogger.e(
                                    DBLogger.LogType.UPLOAD,
                                    loggerTAG,
                                    "Failed to upload data: no upload token",
                                    null
                                )
                                val auth = FirebaseAuth.getInstance()
                                val currentUser: FirebaseUser? = auth.currentUser
                                val bundle = Bundle()
                                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Android")
                                bundle.putString(
                                    FirebaseAnalytics.Param.ITEM_NAME,
                                    "Server did not return upload token in response"
                                )
                                bundle.putString(
                                    AnalyticsKeys.REASON,
                                    "Server did not return upload token in response"
                                )
                                bundle.putString(
                                    FirebaseAnalytics.Param.ITEM_CATEGORY,
                                    currentUser?.uid
                                )
                                firebaseAnalytics.logEvent(AnalyticsKeys.UPLOAD_ERR, bundle)
                                showUploadFailedError()
                            }
                        }
                        .addOnFailureListener {
                            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                            CentralLog.e(loggerTAG, "Failed to get upload token : ${it.localizedMessage}")
                            DBLogger.e(
                                DBLogger.LogType.UPLOAD,
                                loggerTAG,
                                "Failed to get upload token : ${it.localizedMessage}",
                                DBLogger.getStackTraceInJSONArrayString(it)
                            )
                            val auth = FirebaseAuth.getInstance()
                            val currentUser: FirebaseUser? = auth.currentUser
                            val bundle = Bundle()
                            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "Android")
                            bundle.putString(
                                FirebaseAnalytics.Param.ITEM_NAME,
                                "Failed to get upload token"
                            )
                            bundle.putString(AnalyticsKeys.REASON, it.message)

                            currentUser?.uid?.let { uid ->
                                bundle.putString(
                                    FirebaseAnalytics.Param.ITEM_CATEGORY,
                                    uid
                                )
                            }
                            firebaseAnalytics.logEvent(AnalyticsKeys.UPLOAD_ERR, bundle)
                            showUploadCodeError()
                        }
                        .addOnCompleteListener {
                            disposeObj?.dispose()
                        }
                }
        }

        no_upload_code.setOnClickListener {
            it.isEnabled = false
            (activity as MainActivity?)?.onBackPressed()
        }
    }

    private fun showUploadCodeError() {
        var myParentFragment: UploadPageFragment =
            (parentFragment as UploadPageFragment)
        myParentFragment.turnOffLoadingProgress()
        enterPinFragmentErrorMessage.setText(R.string.invalid_pin)
        enterPinFragmentErrorMessage.visibility = View.VISIBLE
    }

    private fun showUploadFailedError() {
        var myParentFragment: UploadPageFragment =
            (parentFragment as UploadPageFragment)
        myParentFragment.turnOffLoadingProgress()
        enterPinFragmentErrorMessage.setText(R.string.upload_failed)
        enterPinFragmentErrorMessage.visibility = View.VISIBLE
    }

    private fun upload(fileToUpload: File, isForCPC: Boolean) {

        val uploadTask = uploadToCloudStorage(TracerApp.AppContext, fileToUpload)
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        uploadTask.addOnFailureListener {
            CentralLog.e(loggerTAG, "failed to upload: ${it.localizedMessage}")
            DBLogger.e(
                DBLogger.LogType.UPLOAD,
                loggerTAG,
                "failed to upload: ${it.localizedMessage}",
                DBLogger.getStackTraceInJSONArrayString(it)
            )

            val myParentFragment: UploadPageFragment =
                (parentFragment as UploadPageFragment)
            myParentFragment.turnOffLoadingProgress()
            enterPinFragmentErrorMessage.visibility = View.VISIBLE
        }.addOnSuccessListener {
            CentralLog.d(loggerTAG, "uploaded successfully")
            val myParentFragment: UploadPageFragment =
                (parentFragment as UploadPageFragment)
            myParentFragment.turnOffLoadingProgress()
            myParentFragment.navigateToUploadComplete(isForCPC)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposeObj?.dispose()
    }

    private fun getUploadToken(uploadCode: String): Task<HttpsCallableResult> {
        val data: MutableMap<String, Any> = java.util.HashMap()
        data["ttId"] = Preference.getTtID(TracerApp.AppContext)
        data["uploadCode"] = uploadCode
        data["appVersion"] = Utils.getAppVersion(TracerApp.AppContext)
        val functions = FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION)
        return functions
            .getHttpsCallable("getUploadTokenV2")
            .call(data)
    }

    private fun writeIdentityFile(
        context: Context,
        dir: File,
        fileName: String,
        uploadToken: String
    ): File {
        var gson = Gson()

        var map: MutableMap<String, Any> = HashMap()
        var device: MutableMap<String, Any> = HashMap()
        device["os"] = "android"
        device["model"] = Build.MODEL
        map["device"] = device
        map["ttId"] = Preference.getTtID(context)
        map["token"] = uploadToken

        val mapString = gson.toJson(map)

        val dir = getUploadDir()
        val normalFile = File(dir, fileName)
        var fileOutputStream2 = FileOutputStream(normalFile)
        fileOutputStream2.write(mapString.toByteArray())
        fileOutputStream2.flush()
        fileOutputStream2.close()


        return normalFile
    }

    private fun writeToInternalStorageModular(
        dirToUse: File,
        fileName: String,
        data: Any,
        dataKey: String
    ): File {
        val gson = Gson()
        
        val outputString = gson.toJson(data)

        val normalFile = File(dirToUse, fileName)
        var fileOutputStream2 = FileOutputStream(normalFile)
        fileOutputStream2.write(outputString.toByteArray())
        fileOutputStream2.flush()
        fileOutputStream2.close()

        return normalFile
    }


    private fun uploadToCloudStorage(context: Context, fileToUpload: File): UploadTask {
        CentralLog.d(TAG, "Uploading to Cloud Storage")

        val bucketName = BuildConfig.FIREBASE_UPLOAD_BUCKET
        val storage = FirebaseStorage.getInstance("gs://${bucketName}")
        var storageRef = storage.getReferenceFromUrl("gs://${bucketName}")

        val dateString = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(Date())
        var streetPassRecordsRef =
            storageRef.child("streetPassRecords/$dateString/${fileToUpload.name}")

        val fileUri = Uri.fromFile(fileToUpload)

        var uploadTask = streetPassRecordsRef.putFile(fileUri)
        uploadTask.addOnCompleteListener {
            try {
//                fileToUpload.delete()
                prepareUploadDir()
                CentralLog.i(TAG, "upload file deleted")
            } catch (e: Exception) {
                val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
                CentralLog.e(loggerTAG, "Failed to delete upload file")
                DBLogger.e(
                    DBLogger.LogType.UPLOAD,
                    loggerTAG,
                    "Failed to delete upload file",
                    null
                )

            }
        }
        return uploadTask
    }

    override fun didProcessBack(): Boolean {
        return false
    }

    private fun prepareUploadDir() {
        val uploadDir = File(TracerApp.AppContext.filesDir, "upload")
        if (uploadDir.exists()) {
            uploadDir.deleteRecursively()
        }

        val abandonedUploadDir =
            TracerApp.AppContext.getDir("upload", Context.MODE_PRIVATE)

        if (abandonedUploadDir.exists()) {
            abandonedUploadDir.deleteRecursively()
        }

        uploadDir.mkdirs()

        val dir =
            ContextCompat.getExternalFilesDirs(
                TracerApp.AppContext,
                Environment.DIRECTORY_DOCUMENTS
            )[0]
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dir.mkdirs()

    }

    fun zip(files: List<File>, zipFile: File, rootPath: String) {
        val BUFFER_SIZE = 2048
        val out = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))

        val fileQueue: Queue<File> = LinkedList()
        fileQueue.addAll(files)

        val buffer = ByteArray(BUFFER_SIZE)

        try {

            while (fileQueue.isNotEmpty()) {
                val file = fileQueue.poll() ?: continue

                if (file.isDirectory) {
                    val subFiles = file.listFiles()

                    subFiles?.forEach {
                        fileQueue.offer(it)
                    }

                } else {

                    val fileInputStream = FileInputStream(file)
                    var origin: BufferedInputStream =
                        BufferedInputStream(fileInputStream, BUFFER_SIZE)
                    val filePath = file.absolutePath
                    try {

//                    val hmm = filePath.substring(filePath.lastIndexOf("/") + 1)
                        val hmm = filePath.substring(rootPath.length)
                        CentralLog.i(TAG, "Creating zip entry: $hmm")
                        val entry = ZipEntry(hmm)

                        out.putNextEntry(entry)
                        var count: Int
                        while (origin.read(buffer, 0, BUFFER_SIZE).also { count = it } != -1) {
                            out.write(buffer, 0, count)
                        }
                        out.flush()
                        out.closeEntry()
                    } finally {
                        origin.close()
                    }
                }

            }
        } finally {
            out.close()
        }
    }

    private fun getUploadDir() : File{
//        val dir = ContextCompat.getExternalFilesDirs(
//            TracerApp.AppContext,
//            Environment.DIRECTORY_DOCUMENTS
//        )[0]

        val dir = File(TracerApp.AppContext.filesDir, "upload")

        dir.mkdirs()
        return dir
    }
}
