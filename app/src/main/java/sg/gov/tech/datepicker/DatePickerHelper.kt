package sg.gov.tech.datepicker

import androidx.lifecycle.ViewModel
import sg.gov.tech.bluetrace.fragment.DateTools
import java.text.DateFormatSymbols
import java.util.*

class DatePickerHelper : ViewModel() {

    fun getCurrentDateType(day: Int, month: Int, allowBlankDayMonth: Boolean): Int {
        return if (allowBlankDayMonth) {
            if (month == 0)
                DateTools.YEAR_ONLY
            else {
                if (day == 0)
                    DateTools.MONTH_YEAR_ONLY
                else
                    DateTools.FULL_DATE
            }
        } else
            DateTools.FULL_DATE
    }

    fun getMonthMaxValue(allowBlankDayMonth: Boolean, monthDisplayValueSize: Int = 0): Int {
        return if (allowBlankDayMonth)
            12
        else
            monthDisplayValueSize - 1
    }

    fun getMonthValue(allowBlankDayMonth: Boolean, dateDisplayType: Int, selectedMonth: Int): Int {
        return if (allowBlankDayMonth) {
            if (dateDisplayType != DateTools.YEAR_ONLY)
                selectedMonth
            else
                0
        } else {
            selectedMonth
        }
    }

    fun getDayValue(allowBlankDayMonth: Boolean, dateDisplayType: Int, daysInMonth: Int, selectedDay: Int): Int {
        return if (allowBlankDayMonth) {
            if (dateDisplayType == DateTools.FULL_DATE)
                selectedDay.coerceAtMost(daysInMonth)
            else
                0

        } else {
            selectedDay.coerceAtMost(daysInMonth)
        }
    }

    //Return the list of days depending on the month selected
    fun getListOfDays(arrOfDays: Array<String?>, daysInMonth: Int): Array<String?> {
        return arrOfDays.sliceArray(0..daysInMonth)
    }

    fun getArrayOfDays(locale: Locale): Array<String?>
    {
        var arrOfDays: Array<String?> = arrayOfNulls(32)

        for (i in arrOfDays.indices) {
            arrOfDays[i] = String.format(locale, "%d", i)
        }

        arrOfDays[0] = " - "

        return arrOfDays
    }

    fun getArrayOfMonths(locale: Locale): Array<String> {
        val blankArray = arrayOf(" - ")
        val arrOfShortMonths = getShortMonths(locale)
        return blankArray + arrOfShortMonths
    }

    fun getShortMonths(locale: Locale): Array<String> {
        return DateFormatSymbols(locale).shortMonths
    }

    fun getDateInCalendar(selectedDay: Int, selectedMonth: Int, selectedYear: Int, allowBlankDayMonth: Boolean): Calendar {
        val cal = Calendar.getInstance()

        if(allowBlankDayMonth && selectedDay == 0)
            cal.set(Calendar.DAY_OF_MONTH, 1)
        else
            cal.set(Calendar.DAY_OF_MONTH, selectedDay)

        if(allowBlankDayMonth && selectedMonth == 0)
            cal.set(Calendar.MONTH, 0)
        else
            cal.set(Calendar.MONTH, getCalMonthNumber(selectedMonth, allowBlankDayMonth))

        cal.set(Calendar.YEAR, selectedYear)
        return cal
    }

    //month starts from 0.
    //Calendar.JANUARY
    fun getDaysInMonth(month: Int, year: Int, allowBlankDayMonth: Boolean): Int {
        val calMonth = getCalMonthNumber(month, allowBlankDayMonth)
        val mycal: Calendar = GregorianCalendar(year, calMonth, 1)
        val daysInMonth: Int = mycal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return daysInMonth
    }

    /*
    Month start from 0 to 11. Due to "blank" = 0,
    the 2 method below is to adjust to the correct month number
     */
    fun getCalMonthNumber(month: Int, allowBlankDayMonth: Boolean): Int
    {
        return if (allowBlankDayMonth) {
            var calMonth = month - 1
            if (calMonth == -1) calMonth = 0
            calMonth
        } else
            month
    }

    fun getPickerMonthNumber(month: Int, allowBlankDayMonth: Boolean): Int {
        return if (allowBlankDayMonth)
            month + 1
        else
            month
    }
}