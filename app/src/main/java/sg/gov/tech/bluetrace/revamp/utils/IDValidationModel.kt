package sg.gov.tech.bluetrace.revamp.utils

data class IDValidationModel(var isValid: Boolean, var cause: Cause)

enum class Cause {
    VALID,
    INVALID_FIN,
    ALREADY_ADDED,
    INVALID_FORMAT,
    UNHANDLED_ERROR,
    USE_FIN,
    USE_NRIC,
    PARTIAL_VALID,
    INCOMPLETE

}
