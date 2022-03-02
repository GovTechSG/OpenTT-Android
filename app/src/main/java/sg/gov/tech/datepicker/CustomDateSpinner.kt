package sg.gov.tech.datepicker

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.NumberPicker
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.fragment.DateTools
import java.text.NumberFormat
import java.util.*

class CustomDateSpinner : FrameLayout {

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    private var selectedDay: Int = 0
    private var selectedMonth: Int = 0
    private var selectedYear: Int = 1970

    var locale: Locale = Locale.ENGLISH
    private lateinit var nf: NumberFormat

    private var dateDisplayType: Int = DateTools.YEAR_ONLY
    private lateinit var arrOfDays: Array<String?>
    private lateinit var arrOfMonths: Array<String>

    private var dpHelper: DatePickerHelper = DatePickerHelper()

    private lateinit var days: NumberPicker
    private lateinit var month: NumberPicker
    private lateinit var year: NumberPicker

    /*
    ------NOTE------
    The number values for selected day and month changed depending on whether the date allows blank day/month

    Allow blank day & month (AllowBlankDayMonth = true) (Currently for passport registration only)
    selectedDay: 0 = Blank, 1-31 = The day as per normal
    selectedMonth: 0 = Blank, 1-12 = The month.
    (Note that we need -1 for the selectedMonth before converting to Calendar format as E.g. 0 = "Jan" not 1)

    DON'T allow blank day & month (AllowBlankDayMonth = false)
    selectedDay: 1-31 = The day as per normal
    selectedMonth: 0-11 = The month, where 0 = "Jan", 1 = "Feb"
     */
    
    init {
        val view = LayoutInflater.from(context).inflate(
            R.layout.date_picker_spinner,
            this,
            false
        )
        addView(view)
        //setupPicker(Locale.ENGLISH)
    }

    fun setupPicker(localeToUse: Locale, allowBlankDayMonth: Boolean, dateDisplayType: Int) {

        locale = localeToUse
        this.dateDisplayType = dateDisplayType

        nf = NumberFormat.getInstance(locale)
        nf.isGroupingUsed = false

        //setup year first
        year = findViewById(R.id.third)
        month = findViewById(R.id.second)
        days = findViewById(R.id.first)

        setUpArrayOfDaysAndMonths()

        setupYear(year)
        setupMonth(month, allowBlankDayMonth)
        setupDays(days, selectedMonth, selectedYear, allowBlankDayMonth)

        year.setOnValueChangedListener { picker, oldVal, newVal ->
            //need to update days
            selectedYear = newVal
            setupDays(days, selectedMonth, selectedYear, allowBlankDayMonth)
        }

        month.setOnValueChangedListener { picker, oldVal, newVal ->
            selectedMonth = newVal
            setCurrentDateType(selectedDay, selectedMonth, allowBlankDayMonth)
            setupDays(days, selectedMonth, selectedYear, allowBlankDayMonth)
        }

        days.setOnValueChangedListener { picker, oldVal, newVal ->
            selectedDay = newVal
            setCurrentDateType(selectedDay, selectedMonth, allowBlankDayMonth)
        }
    }

    //To constantly update the current date display type depending on the day/month selected
    private fun setCurrentDateType(day: Int, month: Int, allowBlankDayMonth: Boolean) {
        dateDisplayType = dpHelper.getCurrentDateType(day, month, allowBlankDayMonth)
    }

    private fun updatePickerDisplay(allowBlankDayMonth: Boolean) {
        year = findViewById(R.id.third)
        month = findViewById(R.id.second)
        days = findViewById(R.id.first)

        setupYear(year)
        setupMonth(month, allowBlankDayMonth)
        setupDays(days, selectedMonth, selectedYear, allowBlankDayMonth)
    }

    private fun setupYear(year: NumberPicker) {
        year.minValue = 1900
        //in case some guy decides to make his phone year before 1900
        year.maxValue = year.minValue.coerceAtLeast(Calendar.getInstance().get(Calendar.YEAR))
        year.value = selectedYear

        year.setFormatter { i ->
            nf.format(i)
        }
    }

    /*
    If AllowBlankDayMonth = true
    0 = "Blank", 1 = "Jan", etc
    If date display type is not YEAR_ONLY, update the selectedMonth accordingly
    else month value will be blank (Meaning 0)

    If AllowBlankDayMonth = false
    0 = "Jan", 1 = "Feb", etc
     */
    private fun setupMonth(month: NumberPicker, allowBlankDayMonth: Boolean) {
        if (allowBlankDayMonth) {
            month.displayedValues = null
            month.minValue = 0
            month.maxValue = dpHelper.getMonthMaxValue(allowBlankDayMonth)
            month.displayedValues = arrOfMonths

        }
        else {
            month.displayedValues = dpHelper.getShortMonths(locale)
            month.minValue = 0
            month.maxValue = dpHelper.getMonthMaxValue(allowBlankDayMonth, month.displayedValues.size)
        }

        month.value = dpHelper.getMonthValue(allowBlankDayMonth, dateDisplayType, selectedMonth)
    }


    /*
    If AllowBlankDayMonth = true
    0 = "Blank", 1 = "1st", etc
    Min value = 0 <- Default value blank
    If date display type is FULL_DATE, update the selectedDay accordingly
    else day value will be blank (Meaning 0)

    If AllowBlankDayMonth = false
    1 = "1st", 2 = "2nd", etc
    Min value = 1 <- Default value 1st
     */
    private fun setupDays(days: NumberPicker, month: Int, year: Int, allowBlankDayMonth: Boolean)
    {
        val daysInMonth = dpHelper.getDaysInMonth(month, year, allowBlankDayMonth)

        if (allowBlankDayMonth) {
            days.displayedValues = null
            days.minValue = 0
            days.maxValue = daysInMonth
            days.displayedValues = dpHelper.getListOfDays(arrOfDays, daysInMonth)

        }
        else {
            days.minValue = 1
            days.maxValue = daysInMonth
        }

        selectedDay = dpHelper.getDayValue(allowBlankDayMonth, dateDisplayType, daysInMonth, selectedDay)
        days.value = selectedDay

        days.setFormatter { i ->
            nf.format(i)
        }
    }

    private fun setUpArrayOfDaysAndMonths() {
        arrOfDays = dpHelper.getArrayOfDays(locale)
        arrOfMonths = dpHelper.getArrayOfMonths(locale)
    }

    //starts and shows this date
    fun set(calendar: Calendar, allowBlankDayMonth: Boolean) {
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectedMonth = dpHelper.getPickerMonthNumber(calendar.get(Calendar.MONTH), allowBlankDayMonth)
        selectedYear = calendar.get(Calendar.YEAR)
        //Reset the selectedDay/selectedMonth to 0 if not needed
        if (allowBlankDayMonth) {
            if (dateDisplayType == DateTools.YEAR_ONLY) {
                selectedDay = 0
                selectedMonth = 0
            } else if (dateDisplayType == DateTools.MONTH_YEAR_ONLY) {
                selectedDay = 0
            }
        }
        updatePickerDisplay(allowBlankDayMonth)
    }

    fun getSelectedDate(allowBlankDayMonth: Boolean = false): Calendar {
        clearAllFocus()
        return dpHelper.getDateInCalendar(selectedDay, selectedMonth, selectedYear, allowBlankDayMonth)
    }

    /*
    This is to fire the ValueChangedListener in the case where user choose to
    type the day/month/year and did not press enter on keyboard
     */
    private fun clearAllFocus() {
        year.clearFocus()
        month.clearFocus()
        days.clearFocus()
    }

    fun getDateType(): Int {
        return dateDisplayType
    }
}
