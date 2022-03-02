package sg.gov.tech.bluetrace.revamp.api

import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import io.reactivex.Single
import org.json.JSONObject
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel
import java.util.*
import java.util.concurrent.TimeUnit


class ApiRepo : IApiRepository {

    private val functions by lazy { FirebaseFunctions.getInstance(BuildConfig.FIREBASE_REGION) }
    private val TAG = "ApiCall"
    private val DATA = "data"

    override fun <T> callSingle(
        data: JSONObject, method: String, clsType: Class<T>?, timeOut: Long
    ): Single<ApiResponseModel<T>> {
        return Single.create { s ->
            functions
                .getHttpsCallable(method)
                .withTimeout(timeOut, TimeUnit.SECONDS)
                .call(data)
                .addOnSuccessListener {
                    val result: HashMap<String, Any> = it.data as HashMap<String, Any>
                    CentralLog.d(TAG, "Api $method success: " + it.data)
                    if (!s.isDisposed)
                        s.onSuccess(ApiResponseModel(true, result.toObject(clsType)))
                }.addOnFailureListener { e ->
                    CentralLog.d(TAG, "Api $method failure: ${e.message}")
                    /**
                     * The status codes that can be returned from a Callable HTTPS. These are the
                     * canonical error codes for Google APIs, as documented here:
                     * https://firebase.google.com/docs/reference/functions/providers_https_
                     *   firebase convert Http codes to own codes, plz do refer FirebaseFunctionsException for mapping
                     */
                    if (!s.isDisposed)
                        s.onError(e)
                }
        }
    }


    override fun <T> callSingleList(
        data: JSONObject, method: String, clsType: Class<T>?, timeOut: Long
    ): Single<ApiResponseModel<T>> {
        return Single.create { s ->
            functions
                .getHttpsCallable(method)
                .withTimeout(timeOut, TimeUnit.SECONDS)
                .call(data)
                .addOnSuccessListener {
                    val result: ArrayList<HashMap<String, Any>> =
                        it.data as ArrayList<HashMap<String, Any>>
                    var res: HashMap<String, Any> = HashMap<String, Any>()
                    res.putAll(hashMapOf(DATA to result))
                    CentralLog.d(TAG, "Api $method success: " + it.data.toString())
                    if (!s.isDisposed)
                        s.onSuccess(ApiResponseModel(true, res.toObject(clsType)))
                }.addOnFailureListener { e ->
                    CentralLog.d(TAG, "Api $method failure: ${e.message}")
                    /**
                     * The status codes that can be returned from a Callable HTTPS. These are the
                     * canonical error codes for Google APIs, as documented here:
                     * https://firebase.google.com/docs/reference/functions/providers_https_
                     *   firebase convert Http codes to own codes, plz do refer FirebaseFunctionsException for mapping
                     */
                    if (!s.isDisposed)
                        s.onError(e)
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