package sg.gov.tech.bluetrace.revamp.utils

import java.util.regex.Pattern

class PhoneNumberValidator {

    companion object {
        // validation rules
        val sgNumberRegex = "(^8[0-9]{7}\$)|(^9[0-9]{7}\$)|(^00254238\$)|(^00985768\$)"
    }

    fun isValid(input: String, countryCode: String): PhoneNumberValidationModel {
        val phoneNumber = input.trim()

        if (phoneNumber.isEmpty()) {
            return PhoneNumberValidationModel(false, Reason.EMPTY)
        }
        if (isSGNumber(countryCode) && phoneNumber.length != 8) {
            return PhoneNumberValidationModel(false, Reason.INVALID_LENGTH)
        }
        if (isSGNumber(countryCode) && !validateSgNumber(phoneNumber)) {
            return PhoneNumberValidationModel(false, Reason.INVALID_NUMBER)
        }

        return PhoneNumberValidationModel(true, Reason.VALID)

    }

    fun isSGNumber(countryCode: String) = countryCode == "SG"

    fun validateSgNumber(phoneNumberString: String): Boolean {
        return Pattern.matches(sgNumberRegex, phoneNumberString)
    }
}