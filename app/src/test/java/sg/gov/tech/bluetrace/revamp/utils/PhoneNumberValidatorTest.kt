package sg.gov.tech.bluetrace.revamp.utils

import org.junit.Assert.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PhoneNumberValidatorTest {
    private val validator = PhoneNumberValidator()

    companion object {
        @JvmStatic
        fun sgPhoneNumInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of(Reason.VALID, "91111111"),
                Arguments.of(Reason.VALID, "88887777"),
                Arguments.of(Reason.VALID, " 91111111"),
                Arguments.of(Reason.VALID, "91111111 "),
                Arguments.of(Reason.VALID, " 91111111 "),
                Arguments.of(Reason.EMPTY, ""),
                Arguments.of(Reason.EMPTY, " "),
                Arguments.of(Reason.EMPTY, "  "),
                Arguments.of(Reason.INVALID_LENGTH, "91111"),
                Arguments.of(Reason.INVALID_LENGTH, "911111111"),
                Arguments.of(Reason.INVALID_LENGTH, "(/),*#"),
                Arguments.of(Reason.INVALID_NUMBER, "71111111"),
                Arguments.of(Reason.INVALID_NUMBER, "11111111"),
                Arguments.of(Reason.INVALID_NUMBER, "(/),*#+.")
            )

        @JvmStatic
        fun otherCountriesPhoneNumInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of(Reason.VALID, "3444555666", "PH"),
                Arguments.of(Reason.VALID, "22222", "SH"),
                Arguments.of(Reason.VALID, "44444", "SH"),
                Arguments.of(Reason.EMPTY, "", "PH"),
                Arguments.of(Reason.EMPTY, " ", "PH"),
                Arguments.of(Reason.EMPTY, "  ", "PH")
            )

        @JvmStatic
        fun countryCodeInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, "SG"),
                Arguments.of(false, "SH"),
                Arguments.of(false, "PH"),
                Arguments.of(false, "MY")
            )


        @JvmStatic
        fun isSGPhoneNumberInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, "91111111"),
                Arguments.of(true, "88887777"),
                Arguments.of(false, " 91111111"),
                Arguments.of(false, "91111111 "),
                Arguments.of(false, " 91111111 "),
                Arguments.of(false, ""),
                Arguments.of(false, " "),
                Arguments.of(false, "  "),
                Arguments.of(false, "91111"),
                Arguments.of(false, "911111111"),
                Arguments.of(false, "71111111"),
                Arguments.of(false, "11111111"),
                Arguments.of(false, "(/),*#+."),
                Arguments.of(false, "(/)#+.")
            )
    }

    @ParameterizedTest
    @MethodSource("sgPhoneNumInputs")
    fun validSGPhoneNumber(expected: Reason, input: String) {
        val result = validator.isValid(input, "SG")
        assertEquals(expected, result.reason)
    }

    @ParameterizedTest
    @MethodSource("otherCountriesPhoneNumInputs")
    fun validOtherCountriesPhoneNumber(expected: Reason, input: String, countryCode: String) {
        val result = validator.isValid(input, countryCode)
        assertEquals(expected, result.reason)
    }

    @ParameterizedTest
    @MethodSource("countryCodeInputs")
    fun isSGCountryCode(expected: Boolean, input: String) {
        val result = validator.isSGNumber(input)
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource("isSGPhoneNumberInputs")
    fun isSGPhoneNumber(expected: Boolean, input: String) {
        val result = validator.validateSgNumber(input)
        assertEquals(expected, result)
    }
}
