package sg.gov.tech.bluetrace.revamp.responseModel

data class GetPassportStatusResponseModel(
    val status: String?,
    val message: Boolean?, val correctedPassport: String?
)


