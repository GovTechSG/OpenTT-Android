package sg.gov.tech.revamp.utils

import android.content.Context
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import org.koin.core.KoinComponent
import org.koin.core.inject
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.CountriesLocalDataProvider
import sg.gov.tech.bluetrace.revamp.utils.*

import java.util.*


class FieldValidationsV2 : KoinComponent {

    private val nricValidator: NRICValidator by inject()
    private val ppNumValidator: PassportNumberValidator by inject()
    private val phoneNumValidator: PhoneNumberValidator by inject()
    val countries: MutableList<String> by lazy {
        CountriesLocalDataProvider().provideFiltered(arrayOf("Singapore"))
    }

    fun isValidDateOfBirth(dob: Long?): Boolean {
        return dob != null && dob <= Date().time
    }

    fun isValidDate(dob: Long?): Boolean {
        return dob != null && dob <= Date().time
    }

    fun isValidNationality(nationality: String): Boolean {
        return nationality in countries
    }

    fun isValidName(name: String): Boolean {
        return !name.isBlank()
    }

    fun isValidPassportNumber(passportNum: String): Boolean {
        return ppNumValidator.isValid(passportNum).isValid
    }

    fun isValidPhoneNumber(number: String, countryCode: String): PhoneNumberValidationModel {
        return phoneNumValidator.isValid(number, countryCode)
    }

    fun validateNric(inputString: String): Boolean {
        val nricToTest = inputString.toUpperCase()
        val result = NRICValidator().isValid(nricToTest)
        return result.isValid
    }

    fun notEmptyString(text: String): Boolean {
        return !text.isNullOrBlank()
    }

    fun Context.hideShowErrorUI(isValid: Boolean, textView: AppCompatTextView, editText: EditText) {
        if (!isValid) {
            textView.visibility = View.VISIBLE
            errorTintEditTextView(this, editText)
        } else {
            textView.visibility = View.GONE
            defaultTintEditTextView(this, editText)
        }
    }

    private fun errorTintEditTextView(context: Context, view: EditText) {
        view.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.error_underline)
    }

    private fun defaultTintEditTextView(context: Context, view: EditText) {
        view.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.default_underline)
    }

    fun validNRICWithCause(nric: String, isNric: Boolean = false, isFin: Boolean = false, forceCheck:Boolean  = false): IDValidationModel {
        return when{
            (nric.length >= 9||forceCheck) ->
                nricValidator.isValid(nric, isNric, isFin)
            (nric.length <= 1) ->
                nricValidator.isValidCharacter(nric, isNric, isFin)

            else -> IDValidationModel(true,Cause.INCOMPLETE)
        }


    }
}