package sg.gov.tech.bluetrace.revamp.utils

import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class NRICValidatorTest {
    private val validator = NRICValidator()

    companion object {
        @JvmStatic
        fun nricInputs(): Stream<Arguments> =
            Stream.of(
                //Valid
                Arguments.of(Cause.VALID, "S3188211G"),
                Arguments.of(Cause.VALID, "T0099360Z"),
                //Invalid
                Arguments.of(Cause.INVALID_FORMAT, "1624684R"), //Missing 1st letter
                Arguments.of(Cause.INVALID_FORMAT, "S31882911G"), //More than 7 Digits
                Arguments.of(Cause.INVALID_FORMAT, "S318811G"), //Less than 7 digit
                Arguments.of(Cause.INVALID_FORMAT, "S3188211"), //Missing last letter
                Arguments.of(Cause.INVALID_FORMAT, " S31882 11G "), //Extra space
                Arguments.of(Cause.INVALID_FORMAT, "S3188211G';%$#"), //Other Invalid Characters
                Arguments.of(Cause.INVALID_FORMAT, ""), //Empty
                Arguments.of(Cause.INVALID_FORMAT, " "), //Single space only
                Arguments.of(Cause.INVALID_FORMAT, "  "), //Multi-space only
                Arguments.of(Cause.USE_FIN, "F2476745X"), //Using FIN in NRIC profile
                Arguments.of(Cause.USE_FIN, "G0256213N"),
                Arguments.of(Cause.INVALID_FIN, "S3188211A") //Wrong last letter
            )

        @JvmStatic
        fun finInputs(): Stream<Arguments> =
            Stream.of(
                //Valid
                Arguments.of(Cause.VALID, "G3826103M"),
                Arguments.of(Cause.VALID, "F9286082P"),
                //Invalid
                Arguments.of(Cause.INVALID_FORMAT, "3826103M"), //Missing 1st letter
                Arguments.of(Cause.INVALID_FORMAT, "G599644561T"), //More than 7 Digits
                Arguments.of(Cause.INVALID_FORMAT, "G826103M"), //Less than 7 digit
                Arguments.of(Cause.INVALID_FORMAT, "G3826103"), //Missing last letter
                Arguments.of(Cause.INVALID_FORMAT, " G3826 103M "), //Extra space
                Arguments.of(Cause.INVALID_FORMAT, "G3826103M';%\$#"), //Other Invalid Characters
                Arguments.of(Cause.INVALID_FORMAT, ""), //Empty
                Arguments.of(Cause.INVALID_FORMAT, " "), //Single space only
                Arguments.of(Cause.INVALID_FORMAT, "  "), //Multi-space only
                Arguments.of(Cause.USE_NRIC, "S3188211G"), //Using FIN in NRIC profile
                Arguments.of(Cause.USE_NRIC, "T0099360Z"),
                Arguments.of(Cause.INVALID_FIN, "G3826103A") //Wrong last letter
            )
    }

    @ParameterizedTest
    @MethodSource("nricInputs")
    fun validNRIC(cause: Cause, input: String){
        val result = validator.isValid(input, isNric = true, isFin = false)
        assertEquals(cause, result.cause)
    }

    @ParameterizedTest
    @MethodSource("finInputs")
    fun validFIN(cause: Cause, input: String){
        val result = validator.isValid(input, isNric = false, isFin = true)
        assertEquals(cause, result.cause)
    }
}