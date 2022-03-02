package sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels

data class ApiResponseModel<T>(
    var isSuccess: Boolean, var result: T?, var code: Int = 0
)

sealed class APIResponse<T>(
    val data: T? = null,
    val message: String? = null,
    val code: String = ""
) {
    class Success<T>(data: T?) : APIResponse<T>(data)
    class Error<T>(data: T? = null, message: String, code: String = "") :
        APIResponse<T>(data, message, code)
}

data class ErrorModel(
    var isSuccess: Boolean, var message: String?, var code: Int = 0
)
