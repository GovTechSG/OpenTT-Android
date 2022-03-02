package sg.gov.tech.bluetrace.onboarding.newOnboard.register

open class RegisterUserData(
    @Transient
    var idTypeEnum: IdentityType,
    var id: String,
    var idDateOfIssue: String?,
    var dateOfBirth: String,
    val model: String,
    @Transient
    var postalCode: String = "",
    var cardSerialNumber: String?,
    var name: String = "",
    var nationality: String = "",
    var idDateOfApplication: String = ""
) {
    val homeAddress = HomeAddress(postalCode)
    var idType = idTypeEnum.tag


    override fun toString(): String {
        return "idType: $idType, id: $id, dateOfIssue: $idDateOfIssue, dateOfBirth: $dateOfBirth, mode: $model, name: $name"
    }

    fun setPassportToVerified() {
        idTypeEnum = IdentityType.PASSPORT_VERIFIED
        idType = IdentityType.PASSPORT_VERIFIED.tag
    }

    companion object{

        fun isValidPassportUser(userIdType: String?): Boolean {
            if (userIdType == null) {
                return false
            }
            val idType = IdentityType.findByValue(userIdType)
            return idType == IdentityType.PASSPORT_VERIFIED
        }

        fun isInvalidUser(userIdType: String?): Boolean {
            if (userIdType == null) {
                return true
            }
            val idType = IdentityType.findByValue(userIdType)
            return idType == IdentityType.ERROR
        }

        fun isInvalidPassportOrInvalidUser(user: RegisterUserData?) : Boolean {
            if(user == null){
                return true
            }

            return when (IdentityType.findByValue(user.idType)) {
                IdentityType.PASSPORT -> true
                IdentityType.PASSPORT_VERIFIED -> false
                IdentityType.ERROR -> true
                else -> false
            }
        }

        fun isInvalidPassportOrInvalidUser(userIdType: String?) : Boolean {
            if(userIdType == null){
                return true
            }

            return when (IdentityType.findByValue(userIdType)) {
                IdentityType.PASSPORT -> true
                IdentityType.PASSPORT_VERIFIED -> false
                IdentityType.ERROR -> true
                else -> false
            }
        }

        fun isInvalidPassportUser(userIdType: String?): Boolean {
            if (userIdType == null) {
                return false
            }
            return when (IdentityType.findByValue(userIdType)) {
                IdentityType.PASSPORT -> true
                IdentityType.PASSPORT_VERIFIED -> false
                else -> false
            }
        }
    }
}


enum class IdentityType(val tag: String) {
    NRIC("nric"),
    FIN_STP("finSTP"),
    FIN_DP("finDP"),
    FIN_WP("finWP"),
    FIN_LTVP("finLTVP"),
    PASSPORT("passport"),
    PASSPORT_VERIFIED("passportVerified"),
    ERROR("");

    companion object {
        private val types = IdentityType.values().associate { it.tag to it }
        fun findByValue(value: String?): IdentityType {
            value?.let {
                return types[it] ?: ERROR
            }
            return ERROR
        }

    }
}


data class HomeAddress(val postalCode: String)

class UpdateUserInfoWithPolicyVersion(
    @Transient
    var idTypeEnumUserInfo: IdentityType,
    id: String,
    idDateOfIssue: String?,
    dateOfBirth: String,
    model: String,
    @Transient
    var postalCodeUserInfo: String = "",
    cardSerialNumber: String?,
    name: String = "",
    nationality: String = "",
    idDateOfApplication: String = "",
    var consentedPrivacyStatementVersion: String?
) : RegisterUserData(
    idTypeEnumUserInfo,
    id,
    idDateOfIssue,
    dateOfBirth,
    model,
    postalCodeUserInfo,
    cardSerialNumber,
    name,
    nationality,
    idDateOfApplication
)
