package sg.gov.tech.bluetrace.revamp.register

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_register_user_ltvp.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.ErrorHandler
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.extentions.afterTextChangedListener
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.*
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.LtvpViewModel
import sg.gov.tech.bluetrace.revamp.api.ErrorCode.INVALID_PARAMETERS
import sg.gov.tech.bluetrace.revamp.api.ErrorCode.RESOURCE_EXHAUSTED
import sg.gov.tech.bluetrace.revamp.utils.Cause
import sg.gov.tech.bluetrace.revamp.utils.IDValidationModel
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder
import sg.gov.tech.bluetrace.view.DateInputBox
import sg.gov.tech.bluetrace.view.OnDateSelectListener

class OnboardingRegisterUserLtvpFragmentV2 : Fragment() {

    private val TAG = "RegisterSTPLTVPFragment"
    private val vm: LtvpViewModel by viewModel()
    private val errorHandler: ErrorHandler by inject { parametersOf(requireContext()) }
    private lateinit var eTName: EditText
    private lateinit var eTLtvp: EditText
    private lateinit var eTSerialNum: EditText
    private lateinit var registerBtn: AppCompatButton
    private lateinit var helpIv: AppCompatImageView
    private lateinit var tvSerialNumError: AppCompatTextView
    private lateinit var tvNameError: AppCompatTextView
    private lateinit var tvLtvpError: AppCompatTextView
    private lateinit var tvDeclarationError: AppCompatTextView
    private lateinit var tvDeclaration: AppCompatTextView
    private lateinit var tvHowToFind: AppCompatTextView
    private lateinit var tvHowToFindSerialNum: AppCompatTextView
    private lateinit var chkDeclaration: AppCompatCheckBox
    private lateinit var backBtn: ImageView
    private lateinit var issuedDateBx: DateInputBox
    private lateinit var tvDoiError: AppCompatTextView
    private var showNameError = false
    private var showLtvpError = false
    private var showSerialNoError = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_register_user_ltvp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_PROFILE_STPLTVP)
        init(view)
    }


    private fun init(view: View) {
        eTName = view.findViewById(R.id.et_name)
        eTLtvp = view.findViewById(R.id.et_nric_fin)
        eTSerialNum = view.findViewById(R.id.et_card_serial)
        registerBtn = view.findViewById(R.id.btn_register)
        helpIv = view.findViewById(R.id.help_ltvp)
        tvSerialNumError = view.findViewById(R.id.serial_number_error)
        tvNameError = view.findViewById(R.id.tv_name_error)
        tvLtvpError = view.findViewById(R.id.tv_nric_error)
        tvDeclarationError = view.findViewById(R.id.tv_declaration_error)
        tvDeclaration = view.findViewById(R.id.declaration_txt)
        backBtn = view.findViewById(R.id.back_ltvp)
        chkDeclaration = view.findViewById(R.id.declaration)
        tvHowToFind = view.findViewById(R.id.how_to_find)
        tvHowToFindSerialNum = view.findViewById(R.id.serial_number_how_to_find)
        issuedDateBx = view.findViewById(R.id.date_of_issue)
        tvDoiError = view.findViewById(R.id.doi_error)
        issuedDateBx.setYearHint(R.string.dd_mmm_yyyy)
        initialiseView()
    }

    private fun initialiseView() {
        initTextWatcher()
        if (BuildConfig.DEBUG) {
            debugAutofill()
        }
        observeFieldValidation()
        registerBtn.setOnClickListener {
            onRegisterClicked()
        }

        backBtn.setOnClickListener {
            (activity as MainOnboardingActivity).goBack()
        }

        tvHowToFind.setOnClickListener {
            tvHowToFind.isEnabled = false
            val dialog = LTVPHowToFindDOIDialogFragment()
            dialog.show(childFragmentManager, "HTF")
            dialog.dismissListener =
                DialogInterface.OnDismissListener { tvHowToFind.isEnabled = true }
        }

        tvHowToFindSerialNum.setOnClickListener {
            tvHowToFindSerialNum.isEnabled = false
            val dialog = LTVPHowToFindSerialDialog()
            dialog.show(childFragmentManager, "WNDDF")
            dialog.dismissListener =
                DialogInterface.OnDismissListener { tvHowToFindSerialNum.isEnabled = true }
        }

        helpIv.setOnClickListener {
            helpIv.isEnabled = false
            val dialog = WhyNeedDetailDialogFragment()
            dialog.show(childFragmentManager, "HTF")
            dialog.dismissListener =
                DialogInterface.OnDismissListener { helpIv.isEnabled = true }
        }

        issuedDateBx.setOnDateEventListener(object : OnDateSelectListener {
            override fun onDateSelected() {
                issuedDateBx.dateInMillis?.let {
                    vm.postValue(LtvpViewModel.ISSUED_DATE, it) {
                        validOrNotDateUI(it)
                    }
                }
            }
        })

        tvDeclaration.makeLinks(
            Pair(resources.getString(R.string.terms_of_Use), View.OnClickListener {
                (activity as MainOnboardingActivity?)?.goToWebViewFragment(BuildConfig.TERMS_OF_USE_URL)

            }),
            Pair(resources.getString(R.string.privacy_statement), View.OnClickListener {
                (activity as MainOnboardingActivity?)?.goToWebViewFragment(BuildConfig.PRIVACY_URL)

            })
        )
    }

    private fun initTextWatcher() {
        eTLtvp.afterTextChangedListener { txt ->
            var result = txt.trim()
            if (txt != result) {
                eTLtvp.setText(result)
                eTLtvp.setSelection(result.length)
            }
            vm.postValueToValidateCause(LtvpViewModel.LTVP, result) { isValid ->
                hideShowFinError(isValid)
            }
        }

        eTName.afterTextChangedListener { text ->
            vm.postValue(LtvpViewModel.NAME, text) { isValid ->
                if (showNameError)
                    hideShowError(isValid, tvNameError, eTName)
                if (isValid)
                    showNameError = true
            }
        }

        eTSerialNum.afterTextChangedListener { text ->
            vm.postValue(LtvpViewModel.SERIAL_NUMBER, text) { isValid ->
                if (showSerialNoError)
                    hideShowError(isValid, tvSerialNumError, eTSerialNum)
                if (isValid)
                    showSerialNoError = true
            }
        }

        chkDeclaration.setOnCheckedChangeListener { _, _ ->
            vm.addHash(LtvpViewModel.DECLARATION, checkDeclaration())
        }
    }


    private fun observeFieldValidation() {
        vm.checksIsRegisterEnable.observe(viewLifecycleOwner, Observer { hash ->
            vm.isFormComplete(
                hash
            ) {
                enableDisableRegButton(it)
            }
        })
    }

    private fun enableDisableRegButton(isEnable: Boolean) {
        if (isEnable) {
            registerBtn.isEnabled = true
            registerBtn.setTextColor(Color.WHITE)
        } else {
            registerBtn.isEnabled = false
            registerBtn.setTextColor(
                ContextCompat.getColor(requireActivity(), R.color.unselected_text)
            )
        }
    }


    private fun debugAutofill() {
        eTName.setText("FIN_LTVP")
        eTLtvp.setText("G1624684R")
        eTSerialNum.setText("abc123")
        issuedDateBx.dateInMillis = 1588636800000
    }

    private fun getCardSerialString(): String {
        return eTSerialNum.text.trim().toString()
    }

    private fun onRegisterClicked() {
        vm.postValueToValidateCause(LtvpViewModel.LTVP, getFinString(),isForce = true) { fm ->
            if (fm.cause == Cause.VALID) {
                vm.isAllFieldValid { isValid ->
                    if (isValid) {
                        val registerUserData = vm.getRegisterRequestData(
                            getFinString(),
                            date_of_issue.getDateString(),
                            getPostalCodeString(),
                            getCardSerialString(),
                            et_name.text.toString()
                        )
                        errorHandler.handleNetworkConnection {
                            if (it) registerUser(registerUserData)
                        }
                    }
                }
            } else {
                hideShowFinError(IDValidationModel(false, Cause.INVALID_FIN))
            }
        }

    }

    private fun validOrNotDateUI(isValid: Boolean) {
        if (!isValid) {
            tvDoiError.visibility = View.VISIBLE
            issuedDateBx.errorUnderlineEffect()
        } else {
            tvDoiError.visibility = View.GONE
            issuedDateBx.defaultUnderlineEffect()
        }
    }

    private fun getPostalCodeString(): String {
        return ""
    }

    private fun getNameString(): String {
        return eTName.text.trim().toString()
    }

    private fun checkDeclaration(): Boolean {
        return if (declaration.isChecked) {
            //hide error
            tvDeclarationError.visibility = View.GONE
            true
        } else {
            //show error
            tvDeclarationError.visibility = View.VISIBLE
            false
        }
    }

    private fun getFinString(): String {
        return eTLtvp.text.trim().toString()
    }

    private fun hideShowError(isValid: Boolean, textView: AppCompatTextView, editText: EditText) {
        if (!isValid) {
            textView.visibility = View.VISIBLE
            errorTintEditTextView(editText)
        } else {
            textView.visibility = View.GONE
            defaultTintEditTextView(editText)
        }
    }

    private fun errorTintEditTextView(view: EditText) {
        view.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.error_underline)
    }

    private fun defaultTintEditTextView(view: EditText) {
        view.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.default_underline)
    }

    private fun registerUser(registerUserData: UpdateUserInfoWithPolicyVersion) {
        (activity as MainOnboardingActivity?)?.setLoadingEnable(true)
        vm.registerUser(registerUserData)
        vm.registrationData.observe(viewLifecycleOwner, Observer { response ->
            CentralLog.d(TAG, "Api successfully" + response)
            if (response.isSuccess) {
                (activity as MainOnboardingActivity?)?.goToPermissionBluetoothFragment()
            } else onError(response.code)
            (activity as MainOnboardingActivity?)?.setLoadingEnable(false)
        })
    }


    private fun onError(code: Int) {
        when (code) {
            INVALID_PARAMETERS -> {
                tvLtvpError.visibility = View.VISIBLE
                tvLtvpError.setText(R.string.validation_failed)
                issuedDateBx.errorUnderlineEffect()
                errorTintEditTextView(eTLtvp)
                errorTintEditTextView(eTSerialNum)
            }
            RESOURCE_EXHAUSTED -> {
                TTAlertBuilder().show(requireContext(), AlertType.TOO_MANY_TRIES_ERROR)
            }
            else -> errorHandler.unableToReachServer()
        }
    }

    private fun hideShowFinError(isValid: IDValidationModel) {
        if (isValid.cause == Cause.INCOMPLETE)
            return
        if (isValid.isValid || isValid.cause == Cause.PARTIAL_VALID) {
            tvLtvpError.visibility = View.GONE
            defaultTintEditTextView(eTLtvp)
            return
        } else if (isValid.cause == Cause.USE_NRIC) {
            tvLtvpError.text = getString(R.string.tab_back_sg_profile)
        } else
            tvLtvpError.text = getString(R.string.invalid_fin)

        if (showLtvpError || tvLtvpError.text.toString() == getString(R.string.tab_back_sg_profile))
            tvLtvpError.visibility = View.VISIBLE
        if (eTLtvp.text.isNotEmpty())
            showLtvpError = true
        else
            tvLtvpError.visibility = View.GONE
        errorTintEditTextView(eTLtvp)
    }
}
