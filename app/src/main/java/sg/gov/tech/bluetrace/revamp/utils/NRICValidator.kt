package sg.gov.tech.bluetrace.revamp.utils

import java.util.*
import java.util.regex.Pattern

class NRICValidator {

    companion object {
        // validation rules
        val nricTypeRegex = "^[STFG]\\d{7}[A-Z]\$"
        val pattern = Regex("^a")
        val weight: IntArray = intArrayOf(2, 7, 6, 5, 4, 3, 2)
        val nricLetterST = "JZIHGFEDCBA".toCharArray()
        val nricLetterFG = "XWUTRQPNMLK".toCharArray()
    }

    fun isValid(input: String, isNric: Boolean = false, isFin: Boolean = false): IDValidationModel {
        var nric = input.toUpperCase()
        //Check if user is using NRIC in a FIN profile
        if (!isNric && (nric.startsWith("S") || nric.startsWith("T")))
            return IDValidationModel(false, Cause.USE_NRIC)

        //Check if user is using FIN in a NRIC profile
        if (!isFin && (nric.startsWith("F") || nric.startsWith("G")))
            return IDValidationModel(false, Cause.USE_FIN)

        return isValidHash(nric)
    }

    fun isValidCharacter(input: String, isNric: Boolean = false, isFin: Boolean = false): IDValidationModel {
        var nric = input.toUpperCase()
        when{
            (!nric.matches(("S|T|F|G").toRegex()))->
                return IDValidationModel(false, Cause.INVALID_FORMAT)
            (!isNric && (nric.startsWith("S") || nric.startsWith("T"))) ->
                return IDValidationModel(false, Cause.USE_NRIC)
            (!isFin && (nric.startsWith("F") || nric.startsWith("G")))->
                return IDValidationModel(false, Cause.USE_FIN)
            else ->
                return IDValidationModel(true, Cause.PARTIAL_VALID)
        }

    }

    fun isValidCharacterToAddMember(input: String): IDValidationModel {
        var nric = input.toUpperCase()
        return when{
            (!nric.matches(("S|T|F|G").toRegex()))->
                IDValidationModel(
                    false,
                    Cause.INVALID_FORMAT
                )
            else ->
                IDValidationModel(
                    false,
                    Cause.PARTIAL_VALID
                )
        }

    }
    fun isValidHash(input: String): IDValidationModel {
        val nric = input.toUpperCase(Locale.getDefault())
        val icArray = CharArray(9)

        //Check nric Start with STFG followed by 7 Digits and End with Character
        if (!Pattern.compile(nricTypeRegex).matcher(nric).matches()) {
            return IDValidationModel(false, Cause.INVALID_FORMAT)
        }

        for (i in 0..8) {
            icArray[i] = nric[i]
        }
        //get first letter of  nric
        var nricType = nric[0]

        //get array of 7 digit
        var nricDigits = nric.substring(1, 8).map {
            Integer.parseInt(it.toString())
        }
        var total = 0

        // multiply each digit in the nric number by it's weight in order
        for (i in nricDigits.indices) {
            total += nricDigits[i] * weight[i]
        }

        // if the nric type is T or G, add 4 to the total
        if (nricType == 'T' || nricType == 'G')
            total += 4

        // check last letter of nric for local
        var letterIndex = total % 11

        var isLastCharaterValid = when (nricType.toString()) {
            "T", "S" -> {
                icArray[8] == nricLetterST[letterIndex]
            }
            "F", "G" -> {
                icArray[8] == nricLetterFG[letterIndex]
            }
            else -> false
        }

        return if (isLastCharaterValid)
            IDValidationModel(true, Cause.VALID)
        else
            IDValidationModel(false, Cause.INVALID_FIN)
    }
}