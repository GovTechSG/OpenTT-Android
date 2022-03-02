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
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_register_user_nric.*
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
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.NricViewModel
import sg.gov.tech.bluetrace.revamp.api.ErrorCode.INVALID_PARAMETERS
import sg.gov.tech.bluetrace.revamp.api.ErrorCode.RESOURCE_EXHAUSTED
import sg.gov.tech.bluetrace.revamp.utils.Cause
import sg.gov.tech.bluetrace.revamp.utils.IDValidationModel
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder
import sg.gov.tech.bluetrace.view.DateInputBox
import sg.gov.tech.bluetrace.view.OnDateSelectListener


class OnboardingRegisterUserNRICFragmentV2 : Fragment() {

    private val TAG = "RegisterNRICFragment"
    private val errorHandler: ErrorHandler by inject { parametersOf(requireContext()) }
    private val vm: NricViewModel by viewModel()
    var clear = false
    private lateinit var tVNameError: AppCompatTextView
    private lateinit var eTName: EditText
    private lateinit var tVNricFinError: AppCompatTextView
    private lateinit var eTNricFin: EditText
    private lateinit var registerBtn: AppCompatButton
    private lateinit var helpIv: AppCompatImageView
    private lateinit var backBtn: ImageView
    private lateinit var dateInputBxDob: DateInputBox
    private lateinit var tVDobError: AppCompatTextView
    private lateinit var dateInputBxIssuedOn: DateInputBox
    private lateinit var chkDeclaration: AppCompatCheckBox
    private lateinit var tvDeclaration: AppCompatTextView
    private lateinit var tvDeclarationError: AppCompatTextView
    private lateinit var tvHowToFind: AppCompatTextView
    private lateinit var tvMinorDoi: AppCompatTextView
    private lateinit var tvnNricDoiHolder: LinearLayout
    private lateinit var tvDoiError: AppCompatTextView
    private var showNameError = false
    private var showNricError = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_register_user_nric, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_PROFILE_NRIC)
        initView(view)
    }

    private fun initView(view: View) {
        tVNameError = view.findViewById(R.id.tv_name_error)
        eTName = view.findViewById(R.id.et_name)
        tVNricFinError = view.findViewById(R.id.tv_nric_error)
        eTNricFin = view.findViewById(R.id.et_nric_fin)
        registerBtn = view.findViewById(R.id.btn_register)
        helpIv = view.findViewById(R.id.help_nric)
        tvDeclaration = view.findViewById(R.id.declaration_txt)
        backBtn = view.findViewById(R.id.back_nric)
        dateInputBxDob = view.findViewById(R.id.nric_dob)
        dateInputBxIssuedOn = view.findViewById(R.id.date_of_issue)
        chkDeclaration = view.findViewById(R.id.declaration)
        tvDeclarationError = view.findViewById(R.id.tv_declaration_error)
        tvHowToFind = view.findViewById(R.id.how_to_find)
        tvnNricDoiHolder = view.findViewById(R.id.nric_doi_holder)
        tvMinorDoi = view.findViewById(R.id.minor_doi_text)
        tvDoiError = view.findViewById(R.id.doi_error)
        tVDobError = view.findViewById(R.id.nric_dob_error)
        initialiseView()
    }

    fun initialiseView() {
        initListenerWatcher()
        if (BuildConfig.DEBUG) {
            debugAutofill()
        }
        observeFieldValidation()
        dateInputBxDob.setYearHint(R.string.dd_mmm_yyyy)
        dateInputBxIssuedOn.setYearHint(R.string.dd_mmm_yyyy)
        backBtn.setOnClickListener {
            (activity as MainOnboardingActivity).goBack()
        }
        registerBtn.setOnClickListener {
            onRegisterClicked()
        }

        tvHowToFind.setOnClickListener {
            tvHowToFind.isEnabled = false
            val dialog = NRICHowToFindDialogFragment()
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

    private fun initListenerWatcher() {
        eTName.afterTextChangedListener { txt ->
            vm.postValue(NricViewModel.NAME, txt) { isValid ->
                if (showNameError)
                    hideShowError(isValid, tVNameError, eTName)
                if (isValid)
                    showNameError = true
            }
        }
        eTNricFin.afterTextChangedListener { txt ->
            var result = txt.trim()
            if (txt != result) {
                eTNricFin.setText(result)
                eTNricFin.setSelection(result.length)
            }
            vm.postValueToValidateCause(NricViewModel.NRIC, result) { fvModel ->
                hideShowErrorForNRIC(fvModel)
            }
        }

        chkDeclaration.setOnCheckedChangeListener { _, _ ->
            vm.addHash(NricViewModel.DECLARATION, checkDeclaration())
        }

        dateInputBxIssuedOn.setOnDateEventListener(object : OnDateSelectListener {
            override fun onDateSelected() {
                dateInputBxIssuedOn.dateInMillis?.let {
                    vm.postValue(NricViewModel.DATE_ISSUED, it) { isValid ->
                        validOrNotDateUI(isValid, dateInputBxIssuedOn, tvDoiError)
                    }
                }
            }
        })

        dateInputBxDob.setOnDateEventListener(object : OnDateSelectListener {
            override fun onDateSelected() {
                if (dateInputBxDob.dateInMillis != null) {
                    when {
                        vm.isMinor(dateInputBxDob.dateInMillis!!) -> {
                            tvnNricDoiHolder.visibility = View.GONE
                            tvMinorDoi.visibility = View.VISIBLE
                            dateInputBxIssuedOn.dateInMillis = null
                            dateInputBxIssuedOn.inputBox.setText("")
                        }
                        else -> {
                            tvnNricDoiHolder.visibility = View.VISIBLE
                            tvMinorDoi.visibility = View.GONE
                        }
                    }

                    vm.postValue(NricViewModel.DOB, dateInputBxDob.dateInMillis!!) { isValid ->
                        validOrNotDateUI(isValid, dateInputBxDob, tVDobError)
                    }
                }
            }
        })
    }

    fun observeFieldValidation() {
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

    private fun onRegisterClicked() {
        vm.postValueToValidateCause(NricViewModel.NRIC, getNRICString(), isForce = true) { fm ->
            if (fm.cause == Cause.VALID) {
                vm.isAllFieldValid { isValid ->
                    if (isValid) {
                        val registerUserData = vm.getRegisterRequestData(
                            getNRICString(),
                            dateInputBxIssuedOn.getDateString().orEmpty(),
                            dateInputBxDob.getDateString().orEmpty(),
                            getPostalCodeString(),
                            eTName.text.toString()
                        )
                        errorHandler.handleNetworkConnection {
                            if (it) registerUser(registerUserData)
                        }
                    }

                }
            } else {
                hideShowErrorForNRIC(IDValidationModel(false, Cause.INVALID_FIN))
            }
        }
    }


    private fun validOrNotDateUI(
        isValid: Boolean,
        dateBx: DateInputBox,
        errorTv: AppCompatTextView
    ) {
        if (!isValid) {
            errorTv.visibility = View.VISIBLE
            dateBx.errorUnderlineEffect()
        } else {
            errorTv.visibility = View.GONE
            dateBx.defaultUnderlineEffect()
        }
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
        return et_nric_fin.text.trim().toString()
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
                CentralLog.d(
                    TAG, "Retrieved HandShakePin successfully"
                )
                (activity as MainOnboardingActivity?)?.goToPermissionBluetoothFragment()
            } else onError(response.code)
            (activity as MainOnboardingActivity?)?.setLoadingEnable(false)
        })
    }

    private fun onError(code: Int) {
        when (code) {
            INVALID_PARAMETERS -> {
                tVNricFinError.visibility = View.VISIBLE
                tVNricFinError.setText(R.string.validation_failed)
                dateInputBxDob.errorUnderlineEffect()
                dateInputBxIssuedOn.errorUnderlineEffect()
                errorTintEditTextView(eTNricFin)
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

    private fun hideShowErrorForNRIC(fvModel: IDValidationModel) {
        if (fvModel.cause == Cause.INCOMPLETE)
            return
        if (fvModel.isValid || fvModel.cause == Cause.PARTIAL_VALID) {
            tVNricFinError.visibility = View.GONE
            defaultTintEditTextView(eTNricFin)
            return
        } else if (fvModel.cause == Cause.USE_FIN)
            tVNricFinError.text = getString(R.string.fin_entered_in_nric_error)
        else
            tVNricFinError.text = getString(R.string.invalid_nric)

        if (showNricError || tVNricFinError.text.toString() == getString(R.string.fin_entered_in_nric_error))
            tVNricFinError.visibility = View.VISIBLE
        if (eTNricFin.text.isNotEmpty())
            showNricError = true
        else
            tVNricFinError.visibility = View.GONE
        errorTintEditTextView(eTNricFin)
    }

    private fun debugAutofill() {
        eTName.setText("HelloName")
        eTNricFin.setText("S3188211G")
        dateInputBxDob.dateInMillis = 895593600000
        dateInputBxIssuedOn.dateInMillis = 1577808000000
    }
}
