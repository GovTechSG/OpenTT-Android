package sg.gov.tech.revamp.responseModel

data class RegisterModel(
    var ttId: String,
    var status: String,
    var passportStatus: String? = null,
    var correctedPassport: String? = null
)

enum class PassportStatus(val tag: String) {
    MATCH("MATCH"),
    MATCH_SGR("MATCH - SGR"),
    NO_MATCH("NO MATCH");
}