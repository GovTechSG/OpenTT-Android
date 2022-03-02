package sg.gov.tech.bluetrace.settings

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.logging.DBLogger
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.APIResponse
import sg.gov.tech.bluetrace.revamp.api.ApiHandler
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SubmitLogViewModel(val apiHandler: ApiHandler) : ViewModel() {

    var uploadResponse: MutableLiveData<APIResponse<UploadTask.TaskSnapshot>> = MutableLiveData()
    var ZIP_FILE_CREATION_FAILED = "ZIP_FILE_CREATION_FAILED"
    var UNABLE_TO_REACH_SERVER = "UNABLE_TO_REACH_SERVER"
    var TAG = "UploadErrorLogs"

    /**
     * method to upload the error log to the server
     */
    fun uploadErrorLogs(context: Context) {
        val loggerTAG = "${javaClass.simpleName} -> ${object{}.javaClass.enclosingMethod?.name}"
        viewModelScope.launch {
            var zipFile = withContext(Dispatchers.IO) { generateZipFile(context) }
            if (zipFile != null) {
                var uploadTask = withContext(Dispatchers.IO) {
                    apiHandler.uploadLogToCloudStorage(
                        context,
                        zipFile
                    )
                }
                uploadTask.addOnFailureListener {
                    CentralLog.e(TAG, "failed to upload error logs: ${it.localizedMessage}")
                    DBLogger.e(
                        DBLogger.LogType.UPLOAD,
                        loggerTAG,
                        "failed to upload error logs: ${it.localizedMessage}",
                        DBLogger.getStackTraceInJSONArrayString(it)
                    )
                    uploadResponse.value = APIResponse.Error(message = UNABLE_TO_REACH_SERVER)
                }
                uploadTask.addOnSuccessListener {
                    uploadResponse.value = APIResponse.Success(it)
                }
                uploadTask.addOnCompleteListener {
                    //remove the log files from internal storage
                    DBLogger.deleteLogFileDirectory(context)
                }
            } else {
                uploadResponse.value = APIResponse.Error(message = ZIP_FILE_CREATION_FAILED)
            }
        }
    }

    /**
     * generate the zip file
     */
    private suspend fun generateZipFile(context: Context): File? {
        return suspendCoroutine { continuation ->
            DBLogger.prepareLogFilesForUpload(context) {
                if (it) {
                    continuation.resume(DBLogger.getZipFileForLog(context))
                } else {
                    continuation.resume(null)
                }
            }
        }
    }
}
