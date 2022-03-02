package sg.gov.tech.bluetrace.extentions

import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.google.firebase.functions.FirebaseFunctionsException
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.ErrorModel
import java.text.SimpleDateFormat
import java.util.*


fun List<String>.getString(): String {
    val stringBuilder = StringBuilder()
    forEach {
        stringBuilder.append("$it ")
    }
    return stringBuilder.toString()
}

fun getDisplayDate(dateInMillis: Long, dateDisplayType: Int): String {
    dateInMillis.let {
        val dateFormat = DateTools.getDisplayDatePattern(dateDisplayType)
        val formatter = SimpleDateFormat(dateFormat, Locale.ENGLISH)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = it
        return formatter.format(calendar.time)
    }
}

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun EditText.afterTextChangedListener(afterTextChanged: (String) -> Unit): TextWatcher {
    val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    }
    this.addTextChangedListener(textWatcher)
    return textWatcher
}

fun AppCompatTextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
    val spannableString = SpannableString(this.text)
    for (link in links) {
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                Selection.setSelection((view as TextView).text as Spannable, 0)
                view.invalidate()
                link.second.onClick(view)
            }
        }
        val startIndexOfLink = this.text.toString().indexOf(link.first)
        spannableString.setSpan(
            clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    this.movementMethod =
        LinkMovementMethod.getInstance() // without LinkMovementMethod, link can not click
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
}

fun onApiError(e: Throwable): ErrorModel {
    return if (e is FirebaseFunctionsException) {
        val code = e.code
        ErrorModel(false, e.message, code.ordinal)
    } else ErrorModel(false, e.message, 0)
}


