package sg.gov.tech.bluetrace.logging

import android.content.Context
import android.os.Environment
import androidx.core.content.ContextCompat
import sg.gov.tech.bluetrace.TracerApp
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object SDLog {

    private const val APP_NAME = "TraceTogether"
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH)

    private var buffer = StringBuffer()
    private var lastWrite = 0L
    private var cachedDateStamp = ""
    private lateinit var cachedFileWriter: BufferedWriter

    private val isWritable: Boolean
        get() {
            val states = checkSDState()
            return states[0] and states[1]
        }

    private fun checkSDState(): BooleanArray {
        val state = Environment.getExternalStorageState()
        var writeable: Boolean
        var sdcard: Boolean
        when (state) {
            Environment.MEDIA_MOUNTED -> {
                writeable = true
                sdcard = true
            }
            Environment.MEDIA_MOUNTED_READ_ONLY -> {
                writeable = false
                sdcard = true
            }
            else -> {
                writeable = false
                sdcard = false
            }
        }
        return booleanArrayOf(sdcard, writeable)
    }

    fun i(vararg message: String) {
        log("INFO", message)
    }

    fun w(vararg message: String) {
        log("WARN", message)
    }

    fun d(vararg message: String) {
        log("DEBUG", message)
    }

    fun e(vararg message: String) {
        log("ERROR", message)
    }

    private fun createFileWriter(context: Context, dateStamp: String): BufferedWriter {
        val dir = ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_DOCUMENTS)[0]
        dir.mkdirs()
        val file = File(dir, APP_NAME + "_" + dateStamp + ".txt")
        val fw = FileWriter(file, true)
        return fw.buffered()
    }

    private fun getFileWriter(context: Context): BufferedWriter {
        //date stamp for filename
        val dateStamp = dateFormat.format(Date())

        return if (dateStamp == cachedDateStamp) {
            cachedFileWriter
        } else {
            //make sure all the logs from previous day is written to the previous file
            if (::cachedFileWriter.isInitialized) {
                cachedFileWriter.flush()
                cachedFileWriter.close()
            }

            //create a new fileWriter for the day
            cachedFileWriter = createFileWriter(context, dateStamp)
            cachedDateStamp = dateStamp
            cachedFileWriter
        }
    }

    private fun log(label: String, message: Array<out String>) {
        if (!isWritable) {
            return
        }

        if (message == null) {
            return
        }

        val context = TracerApp.AppContext
        if(context == null){
            return
        }

        val timeStamp = timestampFormat.format(Date())
        val line = message.joinToString(" ")
        buffer.append("$timeStamp $label $line\n")

        try {
            val fw = getFileWriter(context)
            fw.write(buffer.toString())
            buffer = StringBuffer()
            if (System.currentTimeMillis() - lastWrite > 10000) {
                fw.flush()
                lastWrite = System.currentTimeMillis()
            }
        } catch (e: IOException) {
            buffer.append("$timeStamp ERROR SDLog ??? IOException while writing to SDLog: ${e.message}\n")
        }
    }
}
