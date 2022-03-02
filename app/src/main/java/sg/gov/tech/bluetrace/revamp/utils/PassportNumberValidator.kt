package sg.gov.tech.bluetrace.revamp.utils

import java.util.regex.Pattern

class PassportNumberValidator {

    companion object {
        // validation rules
        //Possible to have single space between each letter, start and end of string
        val ppTypeRegex = "^( +)?[a-zA-Z0-9]+( +[a-zA-Z0-9]+)*( +)?\$"
    }

    fun isValid(input: String): IDValidationModel {
        return if (!Pattern.compile(ppTypeRegex).matcher(input).matches()) {
            IDValidationModel(false, Cause.INVALID_FORMAT)
        } else
            IDValidationModel(true, Cause.VALID)
    }
}
