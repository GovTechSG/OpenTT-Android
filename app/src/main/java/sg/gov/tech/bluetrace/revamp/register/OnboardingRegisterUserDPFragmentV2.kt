package sg.gov.tech.bluetrace.revamp.register

import android.app.Activity
import android.content.Context
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
import kotlinx.android.synthetic.main.fragment_register_user_fin.*
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.extentions.afterTextChangedListener
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.fragment.model.PrivacyPolicyValidationModel
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.*
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.DpViewModel
import sg.gov.tech.bluetrace.revamp.api.ErrorCode.INVALID_PARAMETERS
import sg.gov.tech.bluetrace.revamp.api.ErrorCode.RESOURCE_EXHAUSTED
import sg.gov.tech.bluetrace.revamp.utils.Cause
import sg.gov.tech.bluetrace.revamp.utils.IDValidationModel
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder

class OnboardingRegisterUserDPFragmentV2 : Fragment() {
    private val TAG = "RegisterDpFragment"

    private val errorHandler: ErrorHandler by inject { parametersOf(requireContext()) }
    val vm: DpViewModel by viewModel()
    private lateinit var eTName: EditText
    private lateinit var eTFin: EditText
    private lateinit var eTSerialNum: EditText
    private lateinit var registerBtn: AppCompatButton
    private lateinit var helpIv: AppCompatImageView
    private lateinit var tvSerialNumError: AppCompatTextView
    private lateinit var tvNameError: AppCompatTextView
    private lateinit var tvFinError: AppCompatTextView
    private lateinit var tvDeclarationError: AppCompatTextView
    private lateinit var tvDeclaration: AppCompatTextView
    private lateinit var tvHowToFind: AppCompatTextView
    private lateinit var chkDeclaration: AppCompatCheckBox
    private lateinit var backBtn: ImageView
    private lateinit var mContext: Context
    private var showNameError = false
    private var showFinError = false
    private var showSerialNoError = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_register_user_dp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mContext = view.context
        AnalyticsUtils().screenAnalytics(
            activity as Activity,
            AnalyticsKeys.SCREEN_NAME_ON_BOARD_PROFILE_WPDP
        )
        init(view)
    }

    private fun init(view: View) {
        eTName = view.findViewById(R.id.et_name)
        eTFin = view.findViewById(R.id.et_fin)
        eTSerialNum = view.findViewById(R.id.et_card_serial)
        registerBtn = view.findViewById(R.id.btn_register)
        helpIv = view.findViewById(R.id.help_fin)
        tvSerialNumError = view.findViewById(R.id.tv_card_serial_error)
        tvNameError = view.findViewById(R.id.tv_name_error)
        tvFinError = view.findViewById(R.id.tv_fin_error)
        tvDeclarationError = view.findViewById(R.id.tv_declaration_error)
        tvDeclaration = view.findViewById(R.id.declaration_txt)
        backBtn = view.findViewById(R.id.back_fin)
        chkDeclaration = view.findViewById(R.id.declaration)
        tvHowToFind = view.findViewById(R.id.how_to_find)
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
            val dialog = WorkPassHowToFindDPDialogFragment()
            dialog.show(childFragmentManager, "HTF")
            dialog.dismissListener =
                DialogInterface.OnDismissListener { tvHowToFind.isEnabled = true }
        }

        helpIv.setOnClickListener {
            helpIv.isEnabled = false
            val dialog = WhyNeedDetailDialogFragment()
            dialog.show(childFragmentManager, "WNDDF")
            dialog.dismissListener =
                DialogInterface.OnDismissListener { helpIv.isEnabled = true }
        }
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
        eTFin.afterTextChangedListener { txt ->
            var result = txt.trim()
            if (txt != result) {
                eTFin.setText(result)
                eTFin.setSelection(result.length)
            }
            vm.postValueToValidateCause(DpViewModel.FIN, result) { isValid ->
                hideShowFinError(isValid)
            }
        }

        eTName.afterTextChangedListener { text ->
            vm.postValue(DpViewModel.NAME, text) { isValid ->
                if (showNameError)
                    hideShowError(isValid, tvNameError, eTName)
                if (isValid)
                    showNameError = true
            }
        }

        eTSerialNum.afterTextChangedListener { text ->
            vm.postValue(DpViewModel.SERIAL_NUMBER, text) { isValid ->
                if (showSerialNoError)
                    hideShowError(isValid, tvSerialNumError, eTName)
                if (isValid)
                    showSerialNoError = true
            }
        }

        chkDeclaration.setOnCheckedChangeListener { _, _ ->
            vm.addHash(DpViewModel.DECLARATION, checkDeclaration())
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

    fun debugAutofill() {
        eTName.setText(IdentityType.FIN_DP.tag)
        eTFin.setText("G5996561T")
        eTSerialNum.setText("K0000005")
    }

    private fun onRegisterClicked() {
        vm.postValueToValidateCause(DpViewModel.FIN,  getNRICString(),isForce = true) { fm ->
            if(fm.cause == Cause.VALID) {
                vm.isAllFieldValid { isValid ->
                    if (isValid) {
                        val registerUserData = vm.getRegisterRequestData(getNRICString(),getPostalCodeString(),
                            getCardSerialString(),et_name.text.toString())
                        errorHandler.handleNetworkConnection {
                            if (it) registerUser(registerUserData)
                        }
                    }
                }
            }else {
                hideShowFinError(IDValidationModel(false, Cause.INVALID_FIN))
            }
        }

    }

    private fun getCardSerialString(): String {
        return eTSerialNum.text.trim().toString()
    }

    private fun getPostalCodeString(): String {
        return ""
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

    private fun getNRICString(): String {
        return eTFin.text.trim().toString()
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
                tvFinError.visibility = View.VISIBLE
                tvFinError.setText(R.string.validation_failed)
                errorTintEditTextView(eTSerialNum)
                errorTintEditTextView(eTFin)
            }
            RESOURCE_EXHAUSTED -> {
                TTAlertBuilder().show(requireContext(), AlertType.TOO_MANY_TRIES_ERROR)
            }
            else -> errorHandler.unableToReachServer()
        }
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

    private fun hideShowFinError(isValid: IDValidationModel) {
        if(isValid.cause == Cause.INCOMPLETE)
            return
        if (isValid.isValid || isValid.cause == Cause.PARTIAL_VALID) {
            tvFinError.visibility = View.GONE
            defaultTintEditTextView(eTFin)
            return
        } else if (isValid.cause == Cause.USE_NRIC) {
            tvFinError.text = getString(R.string.tab_back_sg_profile)
        } else
            tvFinError.text = getString(R.string.invalid_fin)

        if (showFinError || tvFinError.text.toString() == getString(R.string.tab_back_sg_profile))
            tvFinError.visibility = View.VISIBLE
        if (eTFin.text.isNotEmpty())
            showFinError = true
        else
            tvFinError.visibility = View.GONE
        errorTintEditTextView(eTFin)
    }
}
