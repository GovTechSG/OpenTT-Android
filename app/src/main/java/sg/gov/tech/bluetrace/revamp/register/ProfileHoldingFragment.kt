package sg.gov.tech.bluetrace.revamp.register

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.fragment.DateTools
import sg.gov.tech.bluetrace.onboarding.newOnboard.Fragments
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.revamp.api.ErrorCode.RESOURCE_EXHAUSTED
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder
import sg.gov.tech.revamp.responseModel.PassportStatus

class ProfileHoldingFragment : Fragment() {

    private val viewModel: ProfileHoldingViewModel by viewModel()
    private lateinit var errorHandler: ErrorHandler

    private lateinit var tvSafeTravel: AppCompatTextView
    private lateinit var tvBirthValue: AppCompatTextView
    private lateinit var tvNationalityValue: AppCompatTextView
    private lateinit var tvPassportValue: AppCompatTextView
    private lateinit var tvUserNameValue: AppCompatTextView
    private lateinit var tvEdit: AppCompatTextView
    private lateinit var tvNextStep: AppCompatTextView
    private lateinit var btnActivateApp: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_profile_holding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        errorHandler = ErrorHandler(view.context)
        initViews(view)
        setUserData()
        setSafeTravelLink()
        setClickListeners()
        setObservers()
    }

    private fun initViews(view: View) {
        tvSafeTravel = view.findViewById(R.id.txtVisitSafeTravel)
        tvBirthValue = view.findViewById(R.id.txtBirthValue)
        tvNationalityValue = view.findViewById(R.id.txtNationalityValue)
        tvPassportValue = view.findViewById(R.id.txtPassportNumValue)
        tvUserNameValue = view.findViewById(R.id.txtUserName)
        tvEdit = view.findViewById(R.id.txtEdit)
        tvNextStep = view.findViewById(R.id.txtNextStep)
        btnActivateApp = view.findViewById(R.id.btnActivateApp)

        tvNextStep.text = HtmlCompat.fromHtml(
            getString(R.string.txt_next_step),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun setUserData() {
        CoroutineScope(Dispatchers.Main).launch {
            val unactivatedPassportUser = withContext(Dispatchers.IO) {
                Preference.getEncryptedUserData(requireContext())
            }
            unactivatedPassportUser?.let {
                tvUserNameValue.text = unactivatedPassportUser.name
                tvNationalityValue.text = unactivatedPassportUser.nationality
                tvPassportValue.text = unactivatedPassportUser.id
                tvBirthValue.text =
                    DateTools.changeDisplayFormat(unactivatedPassportUser.dateOfBirth)
            }
        }
    }

    private fun setSafeTravelLink() {
        val saveTravelUrlString = resources.getString(R.string.safe_travel_url)
        tvSafeTravel.makeLinks(
            Pair(saveTravelUrlString, View.OnClickListener {
                (activity as MainOnboardingActivity?)?.goToWebViewFragment(BuildConfig.SAFE_TRAVEL_URL)
            })
        )

        val spannable = SpannableString(tvSafeTravel.text)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(activity as Context, R.color.blue_text)),
            tvSafeTravel.text.indexOf(saveTravelUrlString),
            tvSafeTravel.text.indexOf(saveTravelUrlString) + saveTravelUrlString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvSafeTravel.text = spannable
    }

    private fun setClickListeners() {
        tvEdit.setOnClickListener {
            parentFragmentManager.popBackStack()
            (activity as MainOnboardingActivity?)?.goToRegisterUserPassportFragment()
        }

        btnActivateApp.setOnClickListener {
            onActivateAppClick()
        }
    }

    private fun setObservers() {
        viewModel.clearRegistrationResponseData()
        viewModel.registrationResponse.observe(viewLifecycleOwner, Observer { response ->
            (activity as MainOnboardingActivity?)?.setLoadingEnable(false)
            if (response.isSuccess && response.result == PassportStatus.MATCH) {
                /*Navigate to Permission screen*/
                (activity as MainOnboardingActivity?)?.goToPermissionBluetoothFragment()
            } else {
                if (response.code == RESOURCE_EXHAUSTED) {
                    TTAlertBuilder().show(requireContext(), AlertType.TOO_MANY_TRIES_ERROR)
                } else
                    when (response.result) {
                        PassportStatus.NO_MATCH -> {
                            /*Show ICA error dialog*/
                            showICAErrorDialog()
                        }
                        PassportStatus.MATCH_SGR -> {
                            /*Show FIN holder dialog*/
                            showFinHolderDialog()
                        }
                        else -> {
                            /*Show unableToReachServer error dialog*/
                            showUnableToReachServerDialog()
                        }
                    }
            }
        })
    }

    private fun onActivateAppClick() {
        val unactivatedPassportUser = Preference.getEncryptedUserData(requireContext())
        unactivatedPassportUser?.let { userData ->
            errorHandler.handleNetworkConnection {
                if (it) {
                    (activity as MainOnboardingActivity?)?.setLoadingEnable(true)
                    viewModel.registerUser(requireContext(), userData)
                }
            }
        }
    }

    private fun showICAErrorDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val errorDialogLayout = layoutInflater.inflate(R.layout.passport_ica_error_dialog, null)
        builder.setView(errorDialogLayout)
        val dialog = builder.create()
        dialog.setCancelable(false)
        val okButton = errorDialogLayout.findViewById<AppCompatTextView>(R.id.ok_button)
        val helpText = errorDialogLayout.findViewById<AppCompatTextView>(R.id.help_text)
        helpText.paintFlags = helpText.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        okButton.setOnClickListener {
            dialog.dismiss()
        }
        helpText.setOnClickListener {
            dialog.dismiss()
            (activity as MainOnboardingActivity?)?.goToWebViewFragment(BuildConfig.ZENDESK_CANT_ACTIVATE_MY_TT_APP_URL)
        }
        dialog.show()
    }

    private fun showFinHolderDialog() {
        val msgBuilder = StringBuilder()
        msgBuilder.append(getString(R.string.passport_fin_holder_message))
        msgBuilder.append(" ")
        msgBuilder.append(getString(R.string.passport_fin_personal_details_safe))

        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.passport_fin_holder_title))
            .setMessage(msgBuilder.toString())
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.cancel()
                clearAppData()
            }
            .setCancelable(false)
        builder.create().show()
    }

    private fun clearAppData() {
        (activity as MainOnboardingActivity?)?.setLoadingEnable(true)
        CoroutineScope(Dispatchers.IO).launch {
            Utils.clearDataAndStopBTService(requireContext())
            navigateToOnBoarding()
        }
    }

    private fun navigateToOnBoarding() {
        Preference.putCheckpoint(requireContext(), Fragments.SELECT_ID_DOCUMENT.id)
        startActivity(Intent(requireContext(), MainOnboardingActivity::class.java))
        activity?.finish()
    }

    private fun showUnableToReachServerDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.temporarily))
            .setMessage(getString(R.string.we_re_reall))
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(false)
        builder.create().show()
    }
}
