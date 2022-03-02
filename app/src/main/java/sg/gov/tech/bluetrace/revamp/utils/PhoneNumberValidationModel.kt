package sg.gov.tech.bluetrace.revamp.utils

data class PhoneNumberValidationModel(var isValid: Boolean, var reason: Reason)

enum class Reason {
    VALID,
    EMPTY,
    INVALID_LENGTH,
    INVALID_NUMBER

}