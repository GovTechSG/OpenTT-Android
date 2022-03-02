package sg.gov.tech.bluetrace.fragment

import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class DateTools {
    companion object {

        //Date display type
        const val FULL_DATE = 0
        const val MONTH_YEAR_ONLY = 1
        const val YEAR_ONLY = 2

        private const val FULL_DATE_DISPLAY_PATTERN = "dd MMM yyyy"
        private const val MONTH_YEAR_ONLY_DISPLAY_PATTERN = "MMM yyyy"
        private const val YEAR_ONLY_PATTERN = "yyyy"

        private const val FULL_DATE_SAVED_PATTERN = "dd-MM-yyyy"
        private const val MONTH_YEAR_ONLY_SAVED_PATTERN = "MM-yyyy"

        fun getStartOfDay(dayInMillis: Long): Calendar {
            val cal = Calendar.getInstance()
            cal.time = Date(dayInMillis)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal
        }

        fun getEndOfDay(dayInMillis: Long): Calendar {
            val cal = getStartOfDay(dayInMillis)
            cal.add(Calendar.DATE, 1)
            cal.add(Calendar.MILLISECOND, -1)
            return cal
        }

        fun daysToMillis(timeInDays: Long): Long {
            return timeInDays * 24 * 60 * 60 * 1000
        }

        fun millisToDays(timeInMillis: Long): Long {
            return timeInMillis / (24 * 60 * 60 * 1000)
        }

        fun hoursToMillis(timeInHours: Long): Long {
            return timeInHours * 60 * 60 * 1000
        }

        fun getStartOfYesterday(dayInMillis: Long): Long {
            val cal = getStartOfDay(dayInMillis)
            cal.add(Calendar.DATE, -1)
            return cal.timeInMillis
        }

        fun getTimeMinutesAgo(minutes: Int): Long {
            val calMinutesAgo = Calendar.getInstance()
            calMinutesAgo.add(Calendar.MINUTE, -minutes)
            return calMinutesAgo.timeInMillis
        }

        fun getTimeHoursAfter(minutes: Int, givenTime: Long): Long {
            val calMinutesAfter = Calendar.getInstance()
            calMinutesAfter.timeInMillis = givenTime
            calMinutesAfter.add(Calendar.MINUTE, minutes)
            return calMinutesAfter.timeInMillis
        }

        fun convertCheckInOutTimeToMs(checkInTime: String): Long {
            val inputDateFormat = SimpleDateFormat(
                "dd-MMM-yyyy'T'HH:mm:ssZ",
                Locale.ENGLISH
            )
            return inputDateFormat.parse(checkInTime)!!.time
        }

        fun convertPassportProfileDOBToMs(dob: String): Long {
            var tempDob = dob
            val dateType = getDisplayDateType(tempDob)
            tempDob = replaceZeroToOne(dateType, tempDob)

            val inputDateFormat = SimpleDateFormat(
                FULL_DATE_SAVED_PATTERN,
                Locale.ENGLISH
            )
            return try{
                val date = inputDateFormat.parse(tempDob)
                date!!.time
            } catch(e: Exception){
                0
            }
        }

        /*
        1. Get the type of the date
        2. Get the pattern of that type of date
        3. If there is any day or month that is "00" change to "01"
        4. Convert to the correct date pattern

        Reason for Point 3, E.g. If we didnt replace the day from "00" to "01" before converting to MMM yyyy,
        the date will be move back by one day. Meaning, if date is 00-02-2020, it will be
        converted to Jan 2020
         */
        fun changeDisplayFormat(date: String): String {
            val dateType = getDisplayDateType(date)
            val pattern = getDisplayDatePattern(dateType)

            var oldFormatDate = date
            oldFormatDate = replaceZeroToOne(dateType, oldFormatDate)

            val givenFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)
            val returnFormat = SimpleDateFormat(pattern, Locale.ENGLISH)

            val newDate: Date = givenFormat.parse(oldFormatDate)
            return returnFormat.format(newDate)
        }

        fun replaceZeroToOne(dateType: Int, date: String): String {
            val dateStr = date.toCharArray()
            if (dateType == YEAR_ONLY)
            {
                dateStr[1] = '1'
                dateStr[4] = '1'
            }
            else if (dateType == MONTH_YEAR_ONLY)
                dateStr[1] = '1'

            return String(dateStr)
        }

        fun getDisplayDateType(date: String): Int {
            val day = date.substring(0,2)
            val month = date.substring(3,5)

            if (month == "00")
                return YEAR_ONLY
            else
            {
                if (day == "00")
                    return MONTH_YEAR_ONLY
                else
                    FULL_DATE
            }

            return FULL_DATE
        }

        fun getDisplayDatePattern(dateDisplayType: Int): String {
            return when (dateDisplayType) {
                FULL_DATE -> FULL_DATE_DISPLAY_PATTERN
                MONTH_YEAR_ONLY -> MONTH_YEAR_ONLY_DISPLAY_PATTERN
                YEAR_ONLY -> YEAR_ONLY_PATTERN
                else -> FULL_DATE_DISPLAY_PATTERN
            }
        }

        fun getSavedDatePattern(dateDisplayType: Int): String {
            return when (dateDisplayType) {
                FULL_DATE -> FULL_DATE_SAVED_PATTERN
                MONTH_YEAR_ONLY -> MONTH_YEAR_ONLY_SAVED_PATTERN
                YEAR_ONLY -> YEAR_ONLY_PATTERN
                else -> FULL_DATE_SAVED_PATTERN
            }
        }

        fun getDateToSaved(dateDisplayType: Int, date: String): String {
            return when (dateDisplayType) {
                FULL_DATE -> date
                MONTH_YEAR_ONLY -> "00-$date"
                YEAR_ONLY -> "00-00-$date"
                else -> date
            }
        }
    }

}