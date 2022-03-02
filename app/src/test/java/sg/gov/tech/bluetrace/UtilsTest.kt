package sg.gov.tech.bluetrace

import org.junit.Assert.*
import org.junit.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class UtilsTest {

    companion object {
        @JvmStatic
        fun getSafeEntryCheckInOutDateFromMsInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("4 Jan 2021/3:22 PM", getDateInMilli("4 Jan 2021 3:22 PM")),
                Arguments.of("23 Sep 2000/9:45 AM", 969673500000)
            )

        @JvmStatic
        fun getSafeEntryCheckInOutDateInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("4 Jan 2021/3:22 PM", getDateInISO("4 Jan 2021 3:22 PM")),
                Arguments.of("20 Jul 2020/1:10 AM", getDateInISO("20 Jul 2020 1:10 AM"))
            )

        @JvmStatic
        fun getDateInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("04/01/2021 15:22:00.000", getDateInMilli("4 Jan 2021 3:22 PM")),
                Arguments.of("20/07/2020 01:10:00.000", getDateInMilli("20 Jul 2020 1:10 AM"))
            )

        @JvmStatic
        fun getTimeInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("3:22 PM", getDateInMilli("4 Jan 2021 3:22 PM")),
                Arguments.of("1:10 AM", getDateInMilli("20 Jul 2020 1:10 AM"))
            )

        const val A_LATER_THAN_B = 1
        const val A_EARLIER_THAN_B = -1
        const val SAME = 0

        @JvmStatic
        fun compareDateInputs(): Stream<Arguments> =
            Stream.of(
                //Year diff, check that even with different day/month, it will not affect the correct result
                Arguments.of(A_LATER_THAN_B, getDateInMilli("4 Jan 2022 3:22 PM"), getDateInMilli("4 Jan 2020 3:22 PM")),
                Arguments.of(A_LATER_THAN_B, getDateInMilli("4 Jan 2022 3:22 PM"), getDateInMilli("10 Jul 2020 3:22 PM")),
                Arguments.of(A_LATER_THAN_B, getDateInMilli("10 Jul 2022 3:22 PM"), getDateInMilli("4 Jan 2020 3:22 PM")),
                Arguments.of(A_EARLIER_THAN_B, getDateInMilli("4 Jan 2020 3:22 PM"), getDateInMilli("4 Jan 2022 3:22 PM")),
                Arguments.of(A_EARLIER_THAN_B, getDateInMilli("10 Jul 2020 3:22 PM"), getDateInMilli("4 Jan 2022 3:22 PM")),
                Arguments.of(A_EARLIER_THAN_B, getDateInMilli("4 Jan 2020 3:22 PM"), getDateInMilli("10 Jul 2022 3:22 PM")),
                //Month diff
                Arguments.of(A_LATER_THAN_B, getDateInMilli("4 Apr 2020 3:22 PM"), getDateInMilli("4 Feb 2020 3:22 PM")),
                Arguments.of(A_LATER_THAN_B, getDateInMilli("4 Apr 2020 3:22 PM"), getDateInMilli("10 Feb 2020 3:22 PM")),
                Arguments.of(A_LATER_THAN_B, getDateInMilli("10 Apr 2020 3:22 PM"), getDateInMilli("4 Feb 2020 3:22 PM")),
                Arguments.of(A_EARLIER_THAN_B, getDateInMilli("4 Feb 2020 3:22 PM"), getDateInMilli("4 Apr 2020 3:22 PM")),
                Arguments.of(A_EARLIER_THAN_B, getDateInMilli("4 Feb 2020 3:22 PM"), getDateInMilli("10 Apr 2020 3:22 PM")),
                Arguments.of(A_EARLIER_THAN_B, getDateInMilli("10 Feb 2020 3:22 PM"), getDateInMilli("4 Apr 2020 3:22 PM")),
                //Day diff
                Arguments.of(A_LATER_THAN_B, getDateInMilli("10 Jan 2020 3:22 PM"), getDateInMilli("4 Jan 2020 3:22 PM")),
                Arguments.of(A_EARLIER_THAN_B, getDateInMilli("4 Jan 2020 3:22 PM"), getDateInMilli("10 Jan 2020 3:22 PM")),
                Arguments.of(SAME, getDateInMilli("4 Jan 2021 3:22 PM"), getDateInMilli("4 Jan 2021 3:22 PM")),
                //Time should not affect the result
                Arguments.of(SAME, getDateInMilli("4 Jan 2020 1:00 PM"), getDateInMilli("4 Jan 2020 3:00 PM")),
                Arguments.of(SAME, getDateInMilli("4 Jan 2020 3:00 PM"), getDateInMilli("4 Jan 2020 1:00 PM"))
            )

        @JvmStatic
        fun getDateFromUnixInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("2021-01-04_15-22-00", getDateInMilli("4 Jan 2021 3:22 PM")),
                Arguments.of("2020-07-20_01-10-00", getDateInMilli("20 Jul 2020 1:10 AM"))
            )

        @JvmStatic
        fun withCommaInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("1", 1),
                Arguments.of("10", 10),
                Arguments.of("100", 100),
                Arguments.of("1,000", 1000),
                Arguments.of("10,000", 10000),
                Arguments.of("100,000", 100000),
                Arguments.of("1,000,000", 1000000),
                Arguments.of("10,000,000", 10000000),
                Arguments.of("100,000,000", 100000000),
                Arguments.of("1,000,000,000", 1000000000)
            )

        @JvmStatic
        fun getShortDateWithComaAfterDayInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("Mon, 4 Jan", getDateInMilli("4 Jan 2021 3:22 PM")),
                Arguments.of("Tue, 21 Jul", getDateInMilli("21 Jul 2020 1:10 AM")),
                Arguments.of("Sun, 3 Jan", getDateInMilli("3 Jan 2021 5:00 PM"))
            )

        @JvmStatic
        fun getDayAndHourWithComaAfterDayInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("4 Jan, 3:22PM", getDateInMilli("4 Jan 2021 3:22 PM")),
                Arguments.of("21 Jul, 1:10AM", getDateInMilli("21 Jul 2020 1:10 AM")),
                Arguments.of("3 Jan, 5:00PM", getDateInMilli("3 Jan 2021 5:00 PM"))
            )

        @JvmStatic
        fun getHourPmAmInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("3:22PM", getDateInMilli("4 Jan 2021 3:22 PM")),
                Arguments.of("1:10AM", getDateInMilli("20 Jul 2020 1:10 AM"))
            )

        @JvmStatic
        fun maskIdWithDotInputs() = Stream.of(
            //Parameter: Valid, Expected result, input
            Arguments.of(true, "S⦁⦁⦁⦁211G", "S3188211G"),
            Arguments.of(true, "A⦁⦁⦁⦁1111", "A11111111")
        )

        @JvmStatic
        fun maskIdWithCrossInputs() = Stream.of(
            //Parameter: Valid, Expected result, input
            Arguments.of(true, "SXXXX211G", "S3188211G"),
            Arguments.of(true, "AXXXX1111", "A11111111")
        )


        private fun getDateInMilli(dateString: String): Long{
            val sdf = SimpleDateFormat("d MMM yyyy h:mm a")
            val mDate: Date? = sdf.parse(dateString)
            return mDate?.time ?: 0L
        }

        //ISO 8601 format
        private fun getDateInISO(dateString: String): String {
            val date = SimpleDateFormat("d MMM yyyy h:mm a").parse(dateString)
            return SimpleDateFormat("dd-MMM-yyyy'T'HH:mm:ssZ").format(date)
        }
    }

    @ParameterizedTest
    @MethodSource("getSafeEntryCheckInOutDateFromMsInputs")
    fun validDateFromMs(expectedDate: String, inputDate: Long) {
        val listDate : List<String> = Utils.getSafeEntryCheckInOutDateFromMs(inputDate)
        assertEquals(expectedDate, listDate[0] + "/" + listDate[1])
    }

    @ParameterizedTest
    @MethodSource("getSafeEntryCheckInOutDateInputs")
    fun validDateFromString(expectedDate: String, inputDate: String) {
        val listDate : List<String> = Utils.getSafeEntryCheckInOutDate(inputDate)
        assertEquals(expectedDate, listDate[0] + "/" + listDate[1])
    }

    @ParameterizedTest
    @MethodSource("getDateInputs")
    fun validGetDate(expectedDate: String, dateInMs: Long) {
        val date : String = Utils.getDate(dateInMs)
        assertEquals(expectedDate, date)
    }

    @ParameterizedTest
    @MethodSource("getTimeInputs")
    fun validGetTime(expectedTime: String, dateInMs: Long) {
        val time : String = Utils.getTime(dateInMs)
        assertEquals(expectedTime, time)
    }

    @ParameterizedTest
    @MethodSource("compareDateInputs")
    fun validResultFromCompareDate(expectedResult: Int, dateInMs1: Long, dateInMs2: Long ) {
        val result : Int = Utils.compareDate(dateInMs1, dateInMs2)
        if (expectedResult == A_LATER_THAN_B && result >= A_LATER_THAN_B)
            assert(true)
        else if (expectedResult == A_EARLIER_THAN_B && result <= A_EARLIER_THAN_B)
            assert(true)
        else
            assertEquals(expectedResult, result) //For '0' case. Meaning both date is the same

    }

    @ParameterizedTest
    @MethodSource("getDateFromUnixInputs")
    fun validGetDateFromUnix(expectedDate: String, dateInMs: Long) {
        val date : String? = Utils.getDateFromUnix(dateInMs)
        assertEquals(expectedDate, date)
    }

    @ParameterizedTest
    @MethodSource("withCommaInputs")
    fun validWithComma(expectedString: String, count: Int) {
        val countInString : String = Utils.withComma(count)
        assertEquals(expectedString, countInString)
    }

    @ParameterizedTest
    @MethodSource("getShortDateWithComaAfterDayInputs")
    fun validGetShortDateWithComaAfterDay(expectedDate: String, dateInMs: Long) {
        val date : String = Utils.getShortDateWithComaAfterDay(dateInMs)
        assertEquals(expectedDate, date)
    }

    @ParameterizedTest
    @MethodSource("getDayAndHourWithComaAfterDayInputs")
    fun validGetDayAndHourWithComaAfterDay(expectedDate: String, dateInMs: Long) {
        val date : String = Utils.getDayAndHourWithComaAfterDay(dateInMs)
        assertEquals(expectedDate, date)
    }

    @ParameterizedTest
    @MethodSource("getHourPmAmInputs")
    fun validGetHourPmAm(expectedDate: String, dateInMs: Long) {
        val date : String = Utils.getHourPmAm(dateInMs)
        assertEquals(expectedDate, date)
    }

    @ParameterizedTest
    @MethodSource("maskIdWithDotInputs")
    fun validMaskIdWithDot (valid: Boolean, expectedValue: String, input: String) {
        val maskedId = Utils.maskIdWithDot(input)
        val result = maskedId == expectedValue
        assertEquals(valid, result)
    }

    @ParameterizedTest
    @MethodSource("maskIdWithCrossInputs")
    fun validMaskIdWithCross (valid: Boolean, expectedValue: String, input: String) {
        val maskedId = Utils.maskIdWithCross(input)
        val result = maskedId == expectedValue
        assertEquals(valid, result)
    }
}
