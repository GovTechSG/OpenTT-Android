package sg.gov.tech.bluetrace.revamp.utils

import org.junit.Assert.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import sg.gov.tech.revamp.utils.FieldValidationsV2
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream


class FieldValidationsV2Test {
    private val fV = FieldValidationsV2()
    /*
    For isValidPassportNumber(), test is done in PassportNumberValidatorTest
    For validNRICWithCause(), test is done in NRICValidatorTest
     */

    companion object {
        @JvmStatic
        fun dateInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, FieldValidationsV2Test().getDateInMilli("10 Dec 2020")), //Before current time
                Arguments.of(true, Date().time), //Current time
                Arguments.of(false, FieldValidationsV2Test().getDateInMilli("10 Dec 2099")) //After current time
            )

        @JvmStatic
        fun nameInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, "JACK"),
                Arguments.of(true, "Myname, Myanothername"),
                Arguments.of(true, "面包")
            )


        @JvmStatic
        fun stringInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of(false, ""),
                Arguments.of(true, "K0000004")
            )
    }

    @ParameterizedTest
    @MethodSource("dateInputs")
    fun validDob(expected: Boolean, input: Long) {
        val result = fV.isValidDateOfBirth(input)
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource("dateInputs")
    fun validDate(expected: Boolean, input: Long) {
        val result = fV.isValidDate(input)
        assertEquals(expected, result)
    }

    fun getDateInMilli(dateString: String): Long{
        val sdf = SimpleDateFormat("dd MMM yyyy")
        val mDate: Date? = sdf.parse(dateString)
        return mDate?.time ?: 0L
    }

    @ParameterizedTest
    @MethodSource("nameInputs")
    fun validName(expected: Boolean, input: String) {
        val result = fV.isValidName(input)
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource("stringInputs")
    fun shouldNotBeEmpty(expected: Boolean, input: String) {
        val result = fV.notEmptyString(input)
        assertEquals(expected, result)
    }
}