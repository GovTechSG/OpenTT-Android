package sg.gov.tech.bluetrace.api

import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import io.reactivex.Single
import org.json.JSONObject
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel

object ApiRepositories {

    private val functions by lazy { FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION) }
    private val TAG = "ApiCall"
    fun <T> callSingle(
        data: JSONObject, method: String, clsType: Class<T>? = null
    ): Single<ApiResponseModel<T>> {
        return Single.create { s ->
            functions
                .getHttpsCallable(method)
                .call(data)
                .addOnSuccessListener {
                    val result: HashMap<String, Any> = it.data as HashMap<String, Any>
                    CentralLog.d(TAG, "Api $method success: " + it.data)
                    if (!s.isDisposed) {
                        s.onSuccess(ApiResponseModel(true, result.toObject(clsType)))
                    }
                }.addOnFailureListener { e ->
                    CentralLog.e(TAG, "Api $method failure: ${e.message}")
                    if (!s.isDisposed) {
                        s.onError(e)
                    }
                }
        }
    }

    private fun <T> HashMap<String, Any>.toObject(cls: Class<T>?): T? {
        if (this == null) return null
        val gson = Gson()
        val jsonElement = gson.toJsonTree(this)
        return Gson().fromJson(jsonElement, cls)
    }
}
