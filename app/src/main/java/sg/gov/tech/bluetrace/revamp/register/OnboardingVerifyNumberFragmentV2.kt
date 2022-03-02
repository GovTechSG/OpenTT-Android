package sg.gov.tech.bluetrace.revamp.register

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.hbb20.CountryCodePicker
import com.hbb20.CountryCodePicker.CustomDialogTextProvider
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.ErrorHandler
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.revamp.utils.Reason

class OnboardingVerifyNumberFragmentV2 : Fragment() {
    private val TAG: String = "OnboardingRegistrationFragmentV2"

    private val vm: OnboardingVerifyNumberViewModel by viewModel()
    private val errorHandler: ErrorHandler by inject { parametersOf(requireContext()) }
    private lateinit var etPhoneNumber: AppCompatEditText
    private lateinit var btnVerifyByOTP: AppCompatButton
    private lateinit var tvPhoneError: AppCompatTextView
    private lateinit var countryCodePicker: CountryCodePicker
    private lateinit var tvDisclaimer: AppCompatTextView

    private var mView: View? = null
    var selectedCountryNameCode = "SG"

    override fun onStart() {
        super.onStart()
        vm.updateRemoteConfig(activity as Activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_onboarding_verify_number, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(
            activity as Activity,
            AnalyticsKeys.SCREEN_NAME_ON_BOARD_MOBILE_NUMBER
        )
        mView = view

        initView(view)
        checkCurrentSelectedCountry()
    }

    private fun initView(view: View){
        etPhoneNumber = view.findViewById(R.id.et_phone_number)
        btnVerifyByOTP = view.findViewById(R.id.btn_verify_by_otp)
        tvPhoneError = view.findViewById(R.id.tv_phone_error)
        countryCodePicker = view.findViewById(R.id.country_code)
        tvDisclaimer = view.findViewById(R.id.tv_disclaimer)

        initListener()
        setCountrySelectionDialogTitle()
    }

    private fun initListener() {
        countryCodePicker.setOnCountryChangeListener {
            checkCurrentSelectedCountry()
        }

        etPhoneNumber.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                tvPhoneError.visibility = View.INVISIBLE
                if (count > 0) {
                    btnVerifyByOTP.isEnabled = true
                }
            }
        })

        btnVerifyByOTP.setOnClickListener {
            onVerifyByOTPBtnClicked()
        }
    }

    private fun onVerifyByOTPBtnClicked() {
        if (mView != null) {
            val phoneNumber = etPhoneNumber.text.toString()
            vm.validatePhoneNumber(countryCodePicker.selectedCountryNameCode, phoneNumber) { result ->
                if (result.isValid) {
                    requestForOTP("${countryCodePicker.selectedCountryCodeWithPlus}${phoneNumber}")
                }
                else {
                    when (result.reason) {
                        Reason.EMPTY -> updatePhoneError(getString(R.string.required))
                        Reason.INVALID_LENGTH -> updatePhoneError(getString(R.string.invalid_length))
                        Reason.INVALID_NUMBER -> updatePhoneError(getString(R.string.invalid_number))
                    }

                    etPhoneNumber.requestFocus()
                }
            }
        }
    }

    private fun requestForOTP(fullNumber: String) {
        errorHandler.handleNetworkConnection {
            if (it) {
                (activity as MainOnboardingActivity?)?.selectedCountryNameCode = selectedCountryNameCode
                (activity as MainOnboardingActivity?)?.requestForOTP(fullNumber)
            }
        }
    }

    fun updatePhoneError(text: String) {
        tvPhoneError.text = text
        tvPhoneError.visibility = View.VISIBLE
    }

    private fun checkCurrentSelectedCountry() {
        selectedCountryNameCode = countryCodePicker.selectedCountryNameCode
        if (selectedCountryNameCode != "SG")
            showDisclaimer(true)
        else
            showDisclaimer(false)
    }

    private fun showDisclaimer(show: Boolean) {
        if (show)
            tvDisclaimer.visibility = View.VISIBLE
        else
            tvDisclaimer.visibility = View.GONE
    }

    private fun setCountrySelectionDialogTitle() {
        countryCodePicker.setCustomDialogTextProvider(object : CustomDialogTextProvider {
            override fun getCCPDialogTitle(
                language: CountryCodePicker.Language,
                defaultTitle: String
            ): String {
                return getString(R.string.select_country_region)
            }

            override fun getCCPDialogSearchHintText(
                language: CountryCodePicker.Language,
                defaultSearchHintText: String
            ): String {
                return defaultSearchHintText
            }

            override fun getCCPDialogNoResultACK(
                language: CountryCodePicker.Language,
                defaultNoResultACK: String
            ): String {
                return defaultNoResultACK
            }
        })
    }
}
