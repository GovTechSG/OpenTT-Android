package sg.gov.tech.bluetrace.revamp.utils

import org.junit.Assert.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EmptySource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PassportNumberValidatorTest {
    private val validator = PassportNumberValidator()

    companion object {
        @JvmStatic
        fun passportNumWithSpaceInputs(): Stream<Arguments> =
            Stream.of(
                //Single space check
                Arguments.of(false, " "),
                Arguments.of(true, " A11111111"),
                Arguments.of(true, "A11111111 "),
                Arguments.of(true, " A11111111 "),
                Arguments.of(true, " A11 11 111 1 "),
                //Multi-space check
                Arguments.of(false, "     "),
                Arguments.of(true, "A1111  1111"),
                Arguments.of(true, "  A11111111  "),
                Arguments.of(true, "  A1111   1111  ")
            )

        @JvmStatic
        fun passportNumInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, "A11111111"),
                Arguments.of(true, "B22222222"),
                Arguments.of(true, "ABC"),
                Arguments.of(true, "123"),
                Arguments.of(true, "ABC123"),
                Arguments.of(false, "!@#$%^&"),
                Arguments.of(false, "@#$123"),
                Arguments.of(false, "!@#ABC"),
                Arguments.of(false, "!@#ABC123"),
                Arguments.of(false, " ,&A0% $ "),
                Arguments.of(false, " 面包224 "),
                Arguments.of(false, " A1.E ")
            )
    }

    @ParameterizedTest
    @MethodSource("passportNumInputs")
    fun shouldOnlyAllowAlphanumeric(expected: Boolean, input: String) {
        val result = validator.isValid(input)
        assertEquals(expected, result.isValid)
    }

    @ParameterizedTest
    @MethodSource("passportNumWithSpaceInputs")
    fun shouldAllowSpaces(expected: Boolean, input: String) {
        val result = validator.isValid(input)
        assertEquals(expected, result.isValid)
    }

    @ParameterizedTest
    @EmptySource
    fun shouldNotBeEmpty(input: String) {
        val result = validator.isValid(input)
        assertFalse(result.isValid)
    }
}
