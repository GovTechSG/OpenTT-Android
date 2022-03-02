package sg.gov.tech.bluetrace.fragment

import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class DateToolsTest {
    private val dt = DateTools

    companion object {
        /*
        Date Test cases
        Day: 00 = Blank, 01 = 1st
        Month: 00 = Blank, 01 = Jan
        FULL_DATE
        1 Jan 1970 : 01-01-1970
        1 Feb 1970 : 01-02-1970
        31 Dec 1970 : 31-12-1970
        MONTH_YEAR_ONLY
        Jan 1970 : 00-01-1970
        Dec 1970 : 00-12-1970
        YEAR_ONLY
        1970 : 00-00-1970
         */

        @JvmStatic
        fun inputsForConvertPassportProfileDOBToMs(): Stream<Arguments> =
            Stream.of(
                //validDateString, dateString
                //Note that the default for 0 day = 1st and 0 month = Jan
                Arguments.of("01 Jan 1970", "01-01-1970"),
                Arguments.of("01 Feb 1970", "01-02-1970"),
                Arguments.of("31 Dec 1970", "31-12-1970"),
                Arguments.of("01 Jan 1970", "00-01-1970"),
                Arguments.of("01 Dec 1970", "00-12-1970"),
                Arguments.of("01 Jan 1970", "00-00-1970")
            )

        @JvmStatic
        fun inputsForChangeDisplayFormat(): Stream<Arguments> =
            Stream.of(
                //validDateString, dateString
                Arguments.of("01 Jan 1970", "01-01-1970"),
                Arguments.of("01 Feb 1970", "01-02-1970"),
                Arguments.of("31 Dec 1970", "31-12-1970"),
                Arguments.of("Jan 1970", "00-01-1970"),
                Arguments.of("Dec 1970", "00-12-1970"),
                Arguments.of("1970", "00-00-1970")
            )

        @JvmStatic
        fun inputsForReplaceZeroToOne(): Stream<Arguments> =
            Stream.of(
                //validDateString, dateType, dateString
                Arguments.of("01-01-1970", DateTools.FULL_DATE, "01-01-1970"),
                Arguments.of("01-02-1970", DateTools.FULL_DATE, "01-02-1970"),
                Arguments.of("31-12-1970", DateTools.FULL_DATE, "31-12-1970"),
                Arguments.of("01-01-1970", DateTools.MONTH_YEAR_ONLY, "00-01-1970"),
                Arguments.of("01-12-1970", DateTools.MONTH_YEAR_ONLY, "00-12-1970"),
                Arguments.of("01-01-1970", DateTools.YEAR_ONLY, "00-00-1970")
            )

        @JvmStatic
        fun inputsForGetDisplayDateType(): Stream<Arguments> =
            Stream.of(
                //correctDateType, dateString
                Arguments.of(DateTools.FULL_DATE, "01-01-1970"),
                Arguments.of(DateTools.FULL_DATE, "01-02-1970"),
                Arguments.of(DateTools.FULL_DATE, "31-12-1970"),
                Arguments.of(DateTools.MONTH_YEAR_ONLY, "00-01-1970"),
                Arguments.of(DateTools.MONTH_YEAR_ONLY, "00-12-1970"),
                Arguments.of(DateTools.YEAR_ONLY, "00-00-1970")
            )


    }

    @ParameterizedTest
    @MethodSource("inputsForConvertPassportProfileDOBToMs")
    fun shouldGetCorrectDateInMilli(validDateString: String, dateString: String) {
        val resultDateStringInMilli = dt.convertPassportProfileDOBToMs(dateString)
        assertEquals(getDateInMilli(validDateString), resultDateStringInMilli)
    }

    private fun getDateInMilli(dateString: String): Long{
        val sdf = SimpleDateFormat("dd MMM yyyy")
        val mDate: Date? = sdf.parse(dateString)
        return mDate?.time ?: 0L
    }

    @ParameterizedTest
    @MethodSource("inputsForChangeDisplayFormat")
    fun shouldGetCorrectDisplayDate(validDateString: String, dateString: String) {
        val resultDateString = dt.changeDisplayFormat(dateString)
        assertEquals(validDateString, resultDateString)
    }

    @ParameterizedTest
    @MethodSource("inputsForReplaceZeroToOne")
    fun validDateForReplaceZeroToOne(validDateString: String, dateType: Int, dateString: String) {
        val resultDateString = dt.replaceZeroToOne(dateType, dateString)
        assertEquals(validDateString, resultDateString)
    }

    @ParameterizedTest
    @MethodSource("inputsForGetDisplayDateType")
    fun shouldGetCorrectDisplayDateType(correctDateType: Int, dateString: String) {
        val resultDateType = dt.getDisplayDateType(dateString)
        assertEquals(correctDateType, resultDateType)
    }
}