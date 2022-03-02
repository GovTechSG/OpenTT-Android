package sg.gov.tech.datepicker

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.date_picker.view.*
import sg.gov.tech.bluetrace.R
import java.util.*

class CustomDatePicker : FrameLayout {

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    var onDateSelectedListener: ((calendar: Calendar) -> Unit)? = null

    init {
        val view = LayoutInflater.from(context).inflate(
            R.layout.date_picker,
            this,
            false
        )
        addView(view)

        //date_spinner.setupPicker(Locale.ENGLISH)

        ok.setOnClickListener {
            onDateSelectedListener?.let { listener -> listener(date_spinner.getSelectedDate()) }
            editor.visibility = View.GONE
        }

        cancel.setOnClickListener {
            editor.visibility = View.GONE
        }

        displayText.setOnClickListener {
            editor.visibility = View.VISIBLE
        }
    }

    companion object {
        fun createDialog(
            context: Context,
            locale: Locale,
            defaultDateCalendar: Calendar,
            allowBlankDayMonth: Boolean,
            dateDisplayType: Int,
            onDateSetListener: ((calendar: Calendar, dateDisplayType: Int) -> Unit)
        ): AlertDialog {
            val spinner = CustomDateSpinner(context)
            spinner.setupPicker(locale, allowBlankDayMonth, dateDisplayType)
            spinner.set(defaultDateCalendar, allowBlankDayMonth)
            val builder = AlertDialog.Builder(context)
            builder.setCancelable(true)
                .setView(spinner)
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    onDateSetListener(spinner.getSelectedDate(allowBlankDayMonth), spinner.getDateType())
                }
            return builder.create()
        }
    }
}
