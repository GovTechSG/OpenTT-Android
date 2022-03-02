package sg.gov.tech.bluetrace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.datepicker.DatePickerHelper
import java.util.*
import java.util.stream.Stream

class DatePickerHelperTest {
    private val helper = DatePickerHelper()

    companion object {
        private const val BLANK_DAY_MONTH_TRUE = true
        private const val BLANK_DAY_MONTH_FALSE = false

        /*
        Date Test cases
        ---- Allow Blank Day/Month case ----
        Day: 0 = Blank, 1 = 1st
        Month: 0 = Blank, 1 = Jan
        FULL_DATE
        1 Jan 1970 : Day = 1, Month = 1, Year = 1970
        1 Feb 1970 : Day = 1, Month = 2, Year = 1970
        31 Dec 1970 : Day = 31, Month = 12, Year = 1970
        MONTH_YEAR_ONLY
        Jan 1970 : Day = 0, Month = 1, Year = 1970
        Dec 1970 : Day = 0, Month = 12, Year = 1970
        YEAR_ONLY
        1970 : Day = 0, Month = 0, Year = 1970

        ---- DONT Allow Blank Day/Month case ----
        Day: 1 = 1st, 2 = 2nd
        Month: 0 = Jan, 1 = Feb
        FULL_DATE
        1 Jan 1970 : Day = 1, Month = 0, Year = 1970
        1 Feb 1970 : Day = 1, Month = 1, Year = 1970
        31 Dec 1970 : Day = 31, Month = 11, Year = 1970
         */

        @JvmStatic
        fun inputsForGetCurrentDateType(): Stream<Arguments> =
            Stream.of(
                //ValidDateType, AllowBlankDayMonth, Day, Month
                //For blank day/month
                Arguments.of(DateTools.FULL_DATE, BLANK_DAY_MONTH_TRUE, 1, 1),
                Arguments.of(DateTools.FULL_DATE, BLANK_DAY_MONTH_TRUE, 1, 2),
                Arguments.of(DateTools.FULL_DATE, BLANK_DAY_MONTH_TRUE, 31, 12),
                Arguments.of(DateTools.MONTH_YEAR_ONLY, BLANK_DAY_MONTH_TRUE, 0, 1),
                Arguments.of(DateTools.MONTH_YEAR_ONLY, BLANK_DAY_MONTH_TRUE, 0, 12),
                Arguments.of(DateTools.YEAR_ONLY, BLANK_DAY_MONTH_TRUE, 0, 0),
                //For no blank day/month
                Arguments.of(DateTools.FULL_DATE, BLANK_DAY_MONTH_FALSE, 1, 0),
                Arguments.of(DateTools.FULL_DATE, BLANK_DAY_MONTH_FALSE, 1, 1),
                Arguments.of(DateTools.FULL_DATE, BLANK_DAY_MONTH_FALSE, 31, 11),
                //For weird case
                Arguments.of(DateTools.FULL_DATE, BLANK_DAY_MONTH_FALSE, 0, 0)
            )

        @JvmStatic
        fun inputsForGetMonthMaxValue(): Stream<Arguments> =
            Stream.of(
                //Correct Max Value, AllowBlankDayMonth, Displayed Month size
                Arguments.of(12, BLANK_DAY_MONTH_TRUE, 0),
                Arguments.of(11, BLANK_DAY_MONTH_FALSE, 12)
            )

        @JvmStatic
        fun inputsForGetMonthValue(): Stream<Arguments> =
            Stream.of(
                //Correct Value, AllowBlankDayMonth, Date display type , Selected Month
                //For blank day/month
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.YEAR_ONLY, 0),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.YEAR_ONLY, 1),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.YEAR_ONLY, 12),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.MONTH_YEAR_ONLY, 0),
                Arguments.of(1, BLANK_DAY_MONTH_TRUE, DateTools.MONTH_YEAR_ONLY, 1),
                Arguments.of(12, BLANK_DAY_MONTH_TRUE, DateTools.MONTH_YEAR_ONLY, 12),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.FULL_DATE, 0),
                Arguments.of(1, BLANK_DAY_MONTH_TRUE, DateTools.FULL_DATE, 1),
                Arguments.of(12, BLANK_DAY_MONTH_TRUE, DateTools.FULL_DATE, 12),
                //For no blank day/month
                Arguments.of(0, BLANK_DAY_MONTH_FALSE, DateTools.YEAR_ONLY, 0),
                Arguments.of(11, BLANK_DAY_MONTH_FALSE, DateTools.YEAR_ONLY, 11),
                Arguments.of(0, BLANK_DAY_MONTH_FALSE, DateTools.MONTH_YEAR_ONLY, 0),
                Arguments.of(11, BLANK_DAY_MONTH_FALSE, DateTools.MONTH_YEAR_ONLY, 11),
                Arguments.of(0, BLANK_DAY_MONTH_FALSE, DateTools.FULL_DATE, 0),
                Arguments.of(11, BLANK_DAY_MONTH_FALSE, DateTools.FULL_DATE, 11)
            )

        @JvmStatic
        fun inputsForGetDayValue(): Stream<Arguments> =
            Stream.of(
                //Correct Value, AllowBlankDayMonth, Number of days in the month , selectedDay
                //For blank day/month
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.YEAR_ONLY, 31, 0),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.YEAR_ONLY, 31, 1),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.YEAR_ONLY, 31, 31),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.MONTH_YEAR_ONLY, 31, 0),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.MONTH_YEAR_ONLY, 31, 1),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.MONTH_YEAR_ONLY, 31, 31),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, DateTools.FULL_DATE, 31, 0),
                Arguments.of(1, BLANK_DAY_MONTH_TRUE, DateTools.FULL_DATE, 31, 1),
                Arguments.of(31, BLANK_DAY_MONTH_TRUE, DateTools.FULL_DATE, 31, 31),
                //For no blank day/month
                Arguments.of(1, BLANK_DAY_MONTH_FALSE, DateTools.YEAR_ONLY, 31, 1),
                Arguments.of(31, BLANK_DAY_MONTH_FALSE, DateTools.YEAR_ONLY, 31, 31),
                Arguments.of(1, BLANK_DAY_MONTH_FALSE, DateTools.MONTH_YEAR_ONLY, 31, 1),
                Arguments.of(31, BLANK_DAY_MONTH_FALSE, DateTools.MONTH_YEAR_ONLY, 31, 31),
                Arguments.of(1, BLANK_DAY_MONTH_FALSE, DateTools.FULL_DATE, 31, 1),
                Arguments.of(31, BLANK_DAY_MONTH_FALSE, DateTools.FULL_DATE, 31, 31),
                //For weird cases where selectedday > daysInMonth
                Arguments.of(28, BLANK_DAY_MONTH_FALSE, DateTools.FULL_DATE, 28, 31)
            )

        @JvmStatic
        fun inputsForGetDateInCalendar(): Stream<Arguments> =
            Stream.of(
                //correctDay, correctMonth, correctYear,  allowBlankDayMonth, selectedDay, selectedMonth, selectedYear
                //For blank day/month
                Arguments.of(1, 0, 1970, BLANK_DAY_MONTH_TRUE, 1, 1, 1970),
                Arguments.of(1, 1, 1970, BLANK_DAY_MONTH_TRUE, 1, 2, 1970),
                Arguments.of(31, 11, 1970, BLANK_DAY_MONTH_TRUE, 31, 12, 1970),
                Arguments.of(1, 0, 1970, BLANK_DAY_MONTH_TRUE, 0, 1, 1970),
                Arguments.of(1, 11, 1970, BLANK_DAY_MONTH_TRUE, 0, 12, 1970),
                Arguments.of(1, 0, 1970, BLANK_DAY_MONTH_TRUE, 0, 0, 1970),
                //For no blank day/month
                Arguments.of(1, 0, 1970, BLANK_DAY_MONTH_FALSE, 1, 0, 1970),
                Arguments.of(1, 1, 1970, BLANK_DAY_MONTH_FALSE, 1, 1, 1970),
                Arguments.of(31, 11, 1970, BLANK_DAY_MONTH_FALSE, 31, 11, 1970)
            )

        @JvmStatic
        fun inputsForGetDaysInMonth(): Stream<Arguments> =
            Stream.of(
                //correctValue, allowBlankDayMonth, selectedMonth, selectedYear
                //For blank day/month
                Arguments.of(31, BLANK_DAY_MONTH_TRUE, 0, 1970), //Blank month will follow Jan
                Arguments.of(31, BLANK_DAY_MONTH_TRUE, 1, 1970),
                Arguments.of(28, BLANK_DAY_MONTH_TRUE, 2, 1970),
                //For no blank day/month
                Arguments.of(31, BLANK_DAY_MONTH_FALSE, 0, 1970),
                Arguments.of(28, BLANK_DAY_MONTH_FALSE, 1, 1970),
                Arguments.of(31, BLANK_DAY_MONTH_FALSE, 2, 1970)
            )

        @JvmStatic
        fun inputsForGetCalMonthNumber(): Stream<Arguments> =
            Stream.of(
                //correctValue, allowBlankDayMonth, selectedMonth
                //For blank day/month
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, 0),
                Arguments.of(0, BLANK_DAY_MONTH_TRUE, 1),
                Arguments.of(1, BLANK_DAY_MONTH_TRUE, 2),
                Arguments.of(11, BLANK_DAY_MONTH_TRUE, 12),
                //For no blank day/month
                Arguments.of(0, BLANK_DAY_MONTH_FALSE, 0),
                Arguments.of(1, BLANK_DAY_MONTH_FALSE, 1),
                Arguments.of(2, BLANK_DAY_MONTH_FALSE, 2),
                Arguments.of(11, BLANK_DAY_MONTH_FALSE, 11)
            )

        @JvmStatic
        fun inputsForGetPickerMonthNumber(): Stream<Arguments> =
            Stream.of(
                //correctValue, allowBlankDayMonth, selectedMonth
                //For blank day/month
                Arguments.of(1, BLANK_DAY_MONTH_TRUE, 0),
                Arguments.of(2, BLANK_DAY_MONTH_TRUE, 1),
                Arguments.of(3, BLANK_DAY_MONTH_TRUE, 2),
                Arguments.of(12, BLANK_DAY_MONTH_TRUE, 11),
                //For no blank day/month
                Arguments.of(0, BLANK_DAY_MONTH_FALSE, 0),
                Arguments.of(1, BLANK_DAY_MONTH_FALSE, 1),
                Arguments.of(11, BLANK_DAY_MONTH_FALSE, 11)
            )
    }

    @ParameterizedTest
    @MethodSource("inputsForGetCurrentDateType")
    fun shouldGetCorrectDateType(validDateType: Int, allowBlankDayMonth: Boolean, day: Int, month: Int) {
        val dateType = helper.getCurrentDateType(day, month, allowBlankDayMonth)
        assertEquals(validDateType, dateType)
    }

    @ParameterizedTest
    @MethodSource("inputsForGetMonthMaxValue")
    fun shouldGetCorrectMonthMaxValue(correctValue: Int, allowBlankDayMonth: Boolean, monthDisplayValueSize: Int = 0) {
        val maxValue = helper.getMonthMaxValue(allowBlankDayMonth, monthDisplayValueSize)
        assertEquals(correctValue, maxValue)
    }

    @ParameterizedTest
    @MethodSource("inputsForGetMonthValue")
    fun shouldGetCorrectMonthValue(correctValue: Int, allowBlankDayMonth: Boolean, dateDisplayType: Int, selectedMonth: Int) {
        val value = helper.getMonthValue(allowBlankDayMonth, dateDisplayType, selectedMonth)
        assertEquals(correctValue, value)
    }

    @ParameterizedTest
    @MethodSource("inputsForGetDayValue")
    fun shouldGetCorrectDayValue(correctValue: Int, allowBlankDayMonth: Boolean, dateDisplayType: Int, daysInMonth: Int, selectedDay: Int) {
        val value = helper.getDayValue(allowBlankDayMonth, dateDisplayType, daysInMonth, selectedDay)
        assertEquals(correctValue, value)
    }

    @ParameterizedTest
    @MethodSource("inputsForGetDateInCalendar")
    fun shouldGetCorrectDateInCalendar(correctDay: Int, correctMonth: Int, correctYear: Int,  allowBlankDayMonth: Boolean, selectedDay: Int, selectedMonth: Int, selectedYear: Int) {
        val cal: Calendar = helper.getDateInCalendar(selectedDay, selectedMonth, selectedYear, allowBlankDayMonth)
        val result = (cal.get(Calendar.DAY_OF_MONTH) == correctDay
                && cal.get(Calendar.MONTH) == correctMonth
                && cal.get(Calendar.YEAR) == correctYear)

        assertTrue(result)
    }

    @ParameterizedTest
    @MethodSource("inputsForGetDaysInMonth")
    fun shouldGetCorrectDaysInMonth(correctValue: Int, allowBlankDayMonth: Boolean, month: Int, year: Int) {
        val value = helper.getDaysInMonth(month, year, allowBlankDayMonth)
        assertEquals(correctValue, value)
    }

    @ParameterizedTest
    @MethodSource("inputsForGetCalMonthNumber")
    fun shouldGetCorrectCalMonthNumber(correctValue: Int, allowBlankDayMonth: Boolean, month: Int) {
        val value = helper.getCalMonthNumber(month, allowBlankDayMonth)
        assertEquals(correctValue, value)
    }

    @ParameterizedTest
    @MethodSource("inputsForGetPickerMonthNumber")
    fun shouldGetCorrectPickerMonthNumber(correctValue: Int, allowBlankDayMonth: Boolean, month: Int) {
        val value = helper.getPickerMonthNumber(month, allowBlankDayMonth)
        assertEquals(correctValue, value)
    }

}