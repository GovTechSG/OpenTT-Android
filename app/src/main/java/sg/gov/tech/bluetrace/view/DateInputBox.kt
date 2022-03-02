package sg.gov.tech.bluetrace.view

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.extentions.getDisplayDate
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.datepicker.CustomDatePicker
import java.text.SimpleDateFormat
import java.util.*


class DateInputBox : FrameLayout, View.OnClickListener {

    var dateInMillis: Long? = null
    var inputBox: AppCompatEditText = LayoutInflater.from(context).inflate(
        R.layout.date_input_box,
        this,
        false
    ) as AppCompatEditText
    lateinit var mListener: OnDateSelectListener

    var allowBlankDayMonth: Boolean = false //Boolean to check if blank day & month is needed
    var dateDisplayType: Int = DateTools.FULL_DATE

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    init {
        inputBox.setOnClickListener(this)
        addView(inputBox)
    }

    override fun onClick(v: View?) {
        inputBox.isEnabled = false
        openDatePicker()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("super", super.onSaveInstanceState())
        bundle.putString("value", inputBox.text.toString())
        bundle.putLong("time_millis", dateInMillis ?: 0)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val superBundle = state.getParcelable<Parcelable>("super")
            super.onRestoreInstanceState(superBundle)
            val value = state.getString("value")
            inputBox.setText(value)
            dateInMillis = if (TextUtils.isEmpty(value)) null else state.getLong("time_millis")
        }
    }

    private fun openDatePicker() {

        val cal = Calendar.getInstance()
        cal.timeInMillis = dateInMillis ?: 0

        val birthdayPicker =
            CustomDatePicker.createDialog(context, Locale.ENGLISH, cal, allowBlankDayMonth, dateDisplayType) { selectedDate, dateDisplayType ->
                inputBox.isEnabled = true
                this.dateDisplayType = dateDisplayType
                dateInMillis = selectedDate.timeInMillis
                inputBox.setText(getDisplayDate(selectedDate.timeInMillis, dateDisplayType))
                mListener.onDateSelected()
            }

        birthdayPicker.setOnDismissListener {
            inputBox.isEnabled = true
        }

        birthdayPicker.show()
    }

    /*private fun openDatePicker2() {
        val birthdaySetListener = object : SupportedDatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.YEAR, year)

                inputBox.isEnabled = true
                dateInMillis = cal.timeInMillis
                inputBox.setText(getDisplayDate(cal.timeInMillis))
                mListener.onDateSelected()
            }
        }

        val cal = Calendar.getInstance()
        cal.timeInMillis = dateInMillis ?: 0

        val birthdayPicker = SupportedDatePickerDialog(
            context,
            R.style.DatePicker,
            birthdaySetListener,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        birthdayPicker.setOnDismissListener {
            inputBox.isEnabled = true
        }

        birthdayPicker.datePicker.maxDate = Date().time

        birthdayPicker.show()

    }*/

    fun getDisplayDate(): String {
        return inputBox.text.toString()
    }

    fun getDateString(): String? {

        dateInMillis?.let {
            val dateFormat = "dd-MM-yyyy"
            // Create a DateFormatter object for displaying date in specified format.
            val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
            // Create a calendar object that will convert the date and time value in milliseconds to date.
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it
            return formatter.format(calendar.time)
        }
        return null
    }

    fun getDateStringForPassport(): String? {

        dateInMillis?.let {
            val dateFormat = DateTools.getSavedDatePattern(dateDisplayType)
            // Create a DateFormatter object for displaying date in specified format.
            val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
            // Create a calendar object that will convert the date and time value in milliseconds to date.
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = it
            var date = formatter.format(calendar.time)
            return DateTools.getDateToSaved(dateDisplayType, date)
        }
        return null
    }

    fun setOnDateEventListener(eventListener: OnDateSelectListener) {
        mListener = eventListener
    }

    fun errorUnderlineEffect() {
        inputBox.isEnabled = true
        inputBox.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.error_underline)
    }

    fun defaultUnderlineEffect() {
        inputBox.isEnabled = true
        inputBox.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.default_underline)
    }

    fun setYearHint(id: Int) {
        inputBox.setHint(id)
    }

    fun clear() {
        inputBox.text?.clear()
        dateInMillis = null
    }

    /*
    dateOfBirth = "" -> Default empty date, not from holding screen (Edit)
    dateOfBirth != "" -> Came from holding screen (Edit). Get the correct date type
     */
    fun setAllowBlankDayMonth(dateOfBirth: String = "")
    {
        allowBlankDayMonth = true
        dateDisplayType = if (dateOfBirth == "") {
            DateTools.YEAR_ONLY
        } else
            DateTools.getDisplayDateType(dateOfBirth)
    }
}

interface OnDateSelectListener {
    fun onDateSelected()
}
