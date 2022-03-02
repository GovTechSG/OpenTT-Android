package sg.gov.tech.bluetrace.logging

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.logging.persistence.LogRecord
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.CoroutineContext

object DBLogger : CoroutineScope {

    override val coroutineContext: CoroutineContext get() = Dispatchers.Main + Job()
    private const val PROD_LOG_NAME = "TraceTogetherLogs"
    private const val pageSize = 1000
    private var TAG = "DBLogger"
    enum class LogLevel {
        DEBUG,
        ERROR,
        INFO,
        WARNING
    }

    enum class LogType {
        BLUETRACE,
        BLUETRACELITE,
        SETTINGS,
        SAFEENTRY,
        USERDATAREGISTERATION,
        ENCRYPTION,
        UPLOAD,
        FIREBASE,
        PASSPORT_VALIDATION,
        HEALTHSTATUS
    }

    private fun shouldLog(): Boolean {
        return true
    }

    fun d(type: LogType, tag: String, message: String, metaData: String? = null) {
        //add the debug log to the database
        if (shouldLog()) {
            insertLogRecord(LogLevel.DEBUG.name, type.name, tag, message, metaData)
        }
    }

    fun e(type: LogType, tag: String, message: String, metaData: String? = null) {
        //add the error log to the database
        if (shouldLog()) {
            insertLogRecord(LogLevel.ERROR.name, type.name, tag, message, metaData)
        }
    }

    fun i(type: LogType, tag: String, message: String, metaData: String? = null) {
        //add the info log to the database
        if (shouldLog()) {
            insertLogRecord(LogLevel.INFO.name, type.name, tag, message, metaData)
        }
    }

    fun w(type: LogType, tag: String, message: String, metaData: String? = null) {
        //add the warning log to the database
        if (shouldLog()) {
            insertLogRecord(LogLevel.WARNING.name, type.name, tag, message, metaData)
        }
    }

    private fun insertLogRecord(
        level: String,
        type: String,
        tag: String,
        message: String,
        metaData: String?
    ) {
        launch {
            val logRecord = LogRecord(level, type, tag, message, metaData)
            StreetPassRecordDatabase.getDatabase(TracerApp.AppContext)
                .logRecordDao().insert(logRecord)
        }
    }

    fun  prepareLogFilesForUpload(context: Context, isFilesPrepared : (Boolean) -> Unit) {
        val logDir = getLogDir(context)
        val allFiles = mutableListOf<File>()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
        val dao = StreetPassRecordDatabase.getDatabase(context).logRecordDao()

        val observableLogRecords = Observable.create<List<File>> {
            val noOfDays = BuildConfig.LOG_PURGE_DAYS - 1
            for (i in 0..noOfDays) {

                val startDateCalendar = Calendar.getInstance()
                startDateCalendar.add(Calendar.DATE, -i)
                startDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startDateCalendar.set(Calendar.MINUTE, 0)
                startDateCalendar.set(Calendar.SECOND, 0)
                startDateCalendar.set(Calendar.MILLISECOND, 0)
                val startDate = Date(startDateCalendar.timeInMillis)
                val startTime = startDateCalendar.timeInMillis

                val endDateCalendar = Calendar.getInstance()
                endDateCalendar.add(Calendar.DATE, -i + 1)
                endDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
                endDateCalendar.set(Calendar.MINUTE, 0)
                endDateCalendar.set(Calendar.SECOND, 0)
                endDateCalendar.set(Calendar.MILLISECOND, 0)
                val endTime = endDateCalendar.timeInMillis

                val dateStamp = dateFormat.format(startDate)
                val dateDir = getDateDir(logDir, dateStamp)

                var itemIndex = 0
                var rawPageResults = dao.getPagedRecords(pageSize, itemIndex, startTime, endTime)
                while (rawPageResults.isNotEmpty()) {
                    writeLogData(
                        dateDir,
                        "$itemIndex.json",
                        rawPageResults
                    )
                    itemIndex += rawPageResults.size
                    rawPageResults = dao.getPagedRecords(pageSize, itemIndex, startTime, endTime)
                }
                allFiles.add(dateDir)
            }
            it.onNext(allFiles)
        }
        readyFile(context, observableLogRecords) { isFileReady ->
            isFilesPrepared.invoke(isFileReady)
        }
    }

    private fun getLogDir(context: Context): File {
        val logDir = File(context.filesDir, PROD_LOG_NAME)
        logDir.mkdirs()
        return logDir
    }

    private fun getDateDir(logDir: File, dateStamp: String): File {
        val dateDir = File(logDir, dateStamp)
        dateDir.mkdirs()
        return dateDir
    }

    private fun writeLogData(dateDir: File, fileName: String, data: Any): File {
        val outputString = Gson().toJson(data)
        val file = File(dateDir, fileName)
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(outputString.toByteArray())
        fileOutputStream.flush()
        fileOutputStream.close()
        return file
    }

    @SuppressLint("CheckResult")
    private fun readyFile(
        context: Context,
        observableLogRecords: Observable<List<File>>,
        isFileReady: (Boolean) -> Unit
    ) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        val stringData = Observable.create<String> {
            it.onNext("")
            it.onComplete()
        }

        Observable.zip(observableLogRecords, stringData,
            BiFunction<List<File>, String, List<File>> { logFiles, _ ->
                logFiles
            }
        )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(
                { //onNext
                        logFiles ->
                    try {
                        val dir = getLogDir(context)
                        val identityFile = writeIdentityFile(context, dir)
                        val filesToWrite = logFiles.toMutableList()
                        filesToWrite.add(identityFile)

                        val fileName = getUploadFileName(context)
                        val zipFile = File(dir, fileName)
                        zipFile(filesToWrite, zipFile, dir.absolutePath)
                        deleteFiles(filesToWrite)
                        isFileReady.invoke(true)
                    }catch (e: Throwable){
                        CentralLog.e(TAG, "Failed to prep zip: ${e.localizedMessage}")
                        e(LogType.UPLOAD, loggerTAG, "Failed to prep zip: ${e.localizedMessage}", getStackTraceInJSONArrayString(e as Exception))
                        e.printStackTrace()
                        isFileReady(false)
                    }

                },
                {
                    //onError
                    isFileReady.invoke(false)
                    CentralLog.e(TAG, "Failed to prep zip: ${it.localizedMessage}")
                    e(LogType.UPLOAD, loggerTAG, "Failed to prep zip: ${it.localizedMessage}", getStackTraceInJSONArrayString(it as Exception))
                    it.printStackTrace()
                })
    }

    private fun writeIdentityFile(context: Context, dir: File): File {

        val map: MutableMap<String, Any> = HashMap()
        map["platform"] = "android"
        map["deviceManufacturer"] = Build.MANUFACTURER
        map["deviceModel"] = Build.MODEL
        map["osVersion"] = Build.VERSION.RELEASE
        map["appVersion"] = Utils.getAppVersion(context)
        map["ttId"] = Preference.getTtID(context)
        map["userProfile"] = getIdType(context)

        val identityFile = File(dir, "identity.json")
        val fileOutputStream = FileOutputStream(identityFile)
        fileOutputStream.write(Gson().toJson(map).toByteArray())
        fileOutputStream.flush()
        fileOutputStream.close()

        return identityFile
    }

    private fun getIdType(context: Context): IdentityType {
        val idType = Preference.getUserIdentityType(context)
        return IdentityType.findByValue(idType)
    }

    private fun getUploadFileName(context: Context): String {
        var userId = ""
        Preference.getEncryptedUserData(context)?.let { user ->
            userId = user.id.substring(user.id.length - 4)
        }
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
        return "${sdf.format(System.currentTimeMillis())}_${userId}.zip"
    }

    private fun zipFile(files: List<File>, zipFile: File, rootPath: String) {
        val outputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))
        val fileQueue: Queue<File> = LinkedList()
        fileQueue.addAll(files)
        val bufferSize = 2048
        val byteArray = ByteArray(bufferSize)

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
                    val bufferedInputStream = BufferedInputStream(fileInputStream, bufferSize)
                    try {
                        val entryName = file.absolutePath.substring(rootPath.length)
                        val zipEntry = ZipEntry(entryName)
                        outputStream.putNextEntry(zipEntry)
                        var length: Int
                        while (bufferedInputStream.read(byteArray, 0, bufferSize)
                                .also { length = it } != -1
                        ) {
                            outputStream.write(byteArray, 0, length)
                        }
                        outputStream.flush()
                        outputStream.closeEntry()
                    } finally {
                        bufferedInputStream.close()
                    }
                }
            }
        } finally {
            outputStream.close()
        }
    }

    private fun deleteFiles(filesToSend: List<File>) {
        for (file in filesToSend) {
            if (file.exists()) {
                file.deleteRecursively()
            }
        }
    }

    fun getZipFileForLog(context: Context): File? {
        var zipFile: File? = null
        try {
            val logDir = getLogDir(context)

            logDir.listFiles()?.iterator()?.forEach {
                if (it.name.contains(".zip")) {
                    zipFile = it
                }
            }
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(TAG, "Failed to get zip log file: ${e.localizedMessage}")
            e(LogType.UPLOAD, loggerTAG, "Failed to get zip log file: ${e.localizedMessage}", getStackTraceInJSONArrayString(e))
            e.printStackTrace()
        }

        return zipFile
    }

    /**
     * delete the Log files and directory
     */
    fun deleteLogFileDirectory(context: Context) {
        try {
            val logFileDirectory = getLogDir(context)
            logFileDirectory.listFiles()?.iterator()?.forEach {
                it.delete()
            }
            logFileDirectory.delete()
        } catch (e: Exception) {
            val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
            CentralLog.e(TAG, "Failed to get delete log folder: ${e.localizedMessage}")
            e(LogType.UPLOAD, loggerTAG, "Failed to get delete log folder: ${e.localizedMessage}", getStackTraceInJSONArrayString(e))
            e.printStackTrace()
        }
    }

    fun getStackTraceInJSONArrayString(e: Throwable): String {
        val filteredErrMsg: MutableList<String> = ArrayList()
        val errorTitle: String = e.toString()
        filteredErrMsg.add(errorTitle)

        val elements: Array<StackTraceElement> = e.stackTrace

        for (ele in elements)
        {
            if (ele.toString().contains("sg.gov"))
                filteredErrMsg.add(ele.toString())
            else
                break
        }

        return JSONArray(filteredErrMsg).toString() ?: ""
    }
}