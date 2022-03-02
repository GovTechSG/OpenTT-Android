package sg.gov.tech.bluetrace.revamp.register

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.extentions.afterTextChangedListener
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.RegisterUserData
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.WhyNeedDetailDialogFragment
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.PassportViewModel
import sg.gov.tech.bluetrace.view.DateInputBox
import sg.gov.tech.bluetrace.view.OnDateSelectListener

class OnboardWithPassportFragmentV2 : Fragment() {
    private val vm: PassportViewModel by viewModel()
    private var countyCode: String? = null
    private lateinit var tVNameError: AppCompatTextView
    private lateinit var eTName: EditText
    private lateinit var tVPassError: AppCompatTextView
    private lateinit var eTPassportNo: EditText
    private lateinit var tVNationalityError: AppCompatTextView
    private lateinit var nationalityAutoCompleteTv: AppCompatAutoCompleteTextView
    private lateinit var registerBtn: AppCompatButton
    private lateinit var helpPassportIv: AppCompatImageView
    private lateinit var tvDeclaration: AppCompatTextView
    private lateinit var backBtn: ImageView
    private lateinit var dateInputBx: DateInputBox
    private lateinit var chkDeclaration: AppCompatCheckBox
    private lateinit var tvDeclarationError: AppCompatTextView
    private lateinit var tvDobError: AppCompatTextView
    private var showNameError = false
    private var showNationalityError = false
    private var showPassportNoError = false

    companion object {
        private const val TAG = "OnboardWithPassportFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_onboard_with_passport, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_PROFILE_PASSPORT)
        initView(view)
    }

    private fun initView(view: View) {
        tVNameError = view.findViewById(R.id.tv_name_error)
        eTName = view.findViewById(R.id.et_name)
        tVPassError = view.findViewById(R.id.tv_pass_error)
        eTPassportNo = view.findViewById(R.id.et_passport_no)
        tVNationalityError = view.findViewById(R.id.tv_nationality_error)
        nationalityAutoCompleteTv = view.findViewById(R.id.nationalityAutoCompleteTv)
        registerBtn = view.findViewById(R.id.btn_register)
        helpPassportIv = view.findViewById(R.id.help_passport)
        tvDeclaration = view.findViewById(R.id.declaration_txt)
        backBtn = view.findViewById(R.id.back_passport)
        dateInputBx = view.findViewById(R.id.dob)
        chkDeclaration = view.findViewById(R.id.declaration)
        tvDeclarationError = view.findViewById(R.id.tv_declaration_error)
        tvDobError = view.findViewById(R.id.dobError)
        initialiseView()
    }

    private fun initialiseView() {
        setDataToBeEdited()
        handleNationalityField()
        initTextWatcher()
        observeFieldValidation()
        backBtn.setOnClickListener {
            //remove the user data if the passport has not been verified yet
            Preference.clearUserData(requireContext())
            (activity as MainOnboardingActivity).goToSelectIdDocumentFragment()
        }
        dateInputBx.setYearHint(R.string.dd_mmm_yyyy)
        dateInputBx.setAllowBlankDayMonth()
        dateInputBx.setOnDateEventListener(object : OnDateSelectListener {
            override fun onDateSelected() {
                dateInputBx.dateInMillis?.let { it ->
                    vm.postValue(PassportViewModel.DOB, it) {
                        checkDateOfBirth(it)
                    }
                }
            }
        })

        helpPassportIv.setOnClickListener {
            helpPassportIv.isEnabled = false
            val dialog = WhyNeedDetailDialogFragment()
            dialog.show(childFragmentManager, "WNDDF")
            dialog.dismissListener =
                DialogInterface.OnDismissListener { helpPassportIv.isEnabled = true }
        }
        tvDeclaration.makeLinks(
            Pair(resources.getString(R.string.terms_of_Use), View.OnClickListener {
                (activity as MainOnboardingActivity?)?.goToWebViewFragment(BuildConfig.TERMS_OF_USE_URL)
            }),
            Pair(resources.getString(R.string.privacy_statement), View.OnClickListener {
                (activity as MainOnboardingActivity?)?.goToWebViewFragment(BuildConfig.PRIVACY_URL)
            })
        )
        registerBtn.setOnClickListener {
            onRegisterClicked()
        }
    }

    private fun setDataToBeEdited() {
        CoroutineScope(Dispatchers.Main).launch {
            val unactivatedPassportUser = withContext(Dispatchers.IO){
                Preference.getEncryptedUserData(requireContext())
            }
            unactivatedPassportUser?.let {
                eTName.setText(unactivatedPassportUser.name)
                nationalityAutoCompleteTv.setText(unactivatedPassportUser.nationality)
                eTPassportNo.setText(unactivatedPassportUser.id)
                dateInputBx.dateInMillis = DateTools.convertPassportProfileDOBToMs(unactivatedPassportUser.dateOfBirth)
                dateInputBx.inputBox.setText(DateTools.changeDisplayFormat(unactivatedPassportUser.dateOfBirth))
                dateInputBx.setAllowBlankDayMonth(unactivatedPassportUser.dateOfBirth)
                dateInputBx.dateInMillis?.let { it ->
                    vm.postValue(PassportViewModel.DOB, it) {
                        checkDateOfBirth(it)
                    }
                }
                chkDeclaration.isChecked = true
            }
        }
    }

    private fun handleNationalityField() {
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(activity!!, android.R.layout.select_dialog_item, vm.countries)
        nationalityAutoCompleteTv.threshold = 2
        nationalityAutoCompleteTv.setAdapter(adapter)
        nationalityAutoCompleteTv.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    vm.isValidNationality(nationalityAutoCompleteTv.text.toString()) {
                        if (!it) {
                            nationalityAutoCompleteTv.setText("")
                        }
                    }
                }
            }
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

    // Initialise InputBox Watchers and Listeners
    private fun initTextWatcher() {
        eTName.afterTextChangedListener { txt ->
            vm.postValue(PassportViewModel.NAME, txt) { isValid ->
                if (showNameError)
                    hideShowError(isValid, tVNameError, eTName)
                if (isValid)
                    showNameError = true
            }
        }

        nationalityAutoCompleteTv.afterTextChangedListener {
            vm.postValue(PassportViewModel.NATIONALITY, it) { isValid ->
                if (showNationalityError)
                    hideShowError(isValid, tVNationalityError, nationalityAutoCompleteTv)
                if (isValid) {
                    countyCode = vm.getCountryCode(nationalityAutoCompleteTv.text.toString())
                    showNationalityError = true
                }
            }

        }

        eTPassportNo.afterTextChangedListener {
            vm.postValue(PassportViewModel.PASSPORT, it) { isValid ->
                if (showPassportNoError)
                    hideShowError(isValid, tVPassError, eTPassportNo)
                if (isValid)
                    showPassportNoError = true
            }
        }

        chkDeclaration.setOnCheckedChangeListener { _, _ ->
            vm.addHash(PassportViewModel.DECLARATION, checkDeclaration())
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

    private fun getNameString(): String {
        return eTName.text.trim().toString()
    }

    private fun checkDeclaration(): Boolean {
        return if (chkDeclaration.isChecked) {
            //hide error
            tvDeclarationError.visibility = View.GONE
            true
        } else {
            //show error
            tvDeclarationError.visibility = View.VISIBLE
            false
        }
    }

    private fun onRegisterClicked() {
        vm.isAllFieldValid {
            val registerUserData = RegisterUserData(
                IdentityType.PASSPORT,
                eTPassportNo.text.toString(),
                null,
                dateInputBx.getDateStringForPassport().orEmpty(),
                Build.MODEL,
                "",
                "",
                getNameString(),
                nationalityAutoCompleteTv.text.toString()
            )
            //open the holding fragment with all user details
            Preference.saveEncryptedUserData(requireContext(),registerUserData)
            parentFragmentManager.popBackStack()
            (activity as MainOnboardingActivity?)?.goToPassportHoldingFragment()
            /*errorHandler.handleNetworkConnection {
                if (it) registerUser(registerUserData)
            }*/
        }
    }

    private fun checkDateOfBirth(isValid: Boolean) {
        if (isValid) {
            tvDobError.visibility = View.GONE
            dateInputBx.defaultUnderlineEffect()

        } else {
            tvDobError.visibility = View.VISIBLE
            dateInputBx.errorUnderlineEffect()
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
}
