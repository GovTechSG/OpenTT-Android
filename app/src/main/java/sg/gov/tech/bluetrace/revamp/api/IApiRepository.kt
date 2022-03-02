package sg.gov.tech.bluetrace.revamp.api

import io.reactivex.Single
import org.json.JSONObject
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ApiResponseModel

interface IApiRepository {
    fun <T> callSingle(
        data: JSONObject, method: String, clsType: Class<T>? = null, timeOut: Long = 10
    ): Single<ApiResponseModel<T>>

    fun <T> callSingleList(
        data: JSONObject, method: String, clsType: Class<T>? = null, timeOut: Long = 5
    ): Single<ApiResponseModel<T>>
}