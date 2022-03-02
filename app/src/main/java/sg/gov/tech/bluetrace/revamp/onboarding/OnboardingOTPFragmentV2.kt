package sg.gov.tech.bluetrace.revamp.onboarding

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.chaos.view.PinView
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity
import sg.gov.tech.bluetrace.revamp.requestModel.CreateUserRequestModel


class OnboardingOTPFragmentV2 : Fragment() {
    private val TAG: String = "OnboardingOTPFragmentV2"

    companion object {
        private const val COUNTDOWN_DURATION = 60L
    }

    private lateinit var mOtp: String
    private var stopWatch: CountDownTimer? = null
    private lateinit var act: MainOnboardingActivity
    private lateinit var mPhoneNumber: String
    private lateinit var mSelectedCountryNameCode: String
    private lateinit var mRequestId: String

    private val vm: OnboardingOTPViewModel by viewModel()
    private val errorHandler: ErrorHandler by inject { parametersOf(requireContext()) }

    private lateinit var tvSentTo: AppCompatTextView
    private lateinit var tvWrongNumber: AppCompatTextView
    private lateinit var pinView: PinView
    private lateinit var tvError: AppCompatTextView
    private lateinit var btnResendCode: AppCompatTextView
    private lateinit var btnVerify: AppCompatButton
    private lateinit var tvDisclaimer: AppCompatTextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_onboarding_otp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        act = (activity as MainOnboardingActivity)
        AnalyticsUtils().screenAnalytics(act, AnalyticsKeys.SCREEN_NAME_ON_BOARD_OTP)

        initViews(view)
        startTimer()
    }

    private fun initViews(view: View) {
        tvSentTo = view.findViewById(R.id.sent_to)
        tvWrongNumber = view.findViewById(R.id.wrongNumber)
        pinView = view.findViewById(R.id.firstPinView)
        tvError = view.findViewById(R.id.tv_error)
        btnResendCode = view.findViewById(R.id.resendCode)
        btnVerify = view.findViewById(R.id.btn_verify)
        tvDisclaimer = view.findViewById(R.id.tv_disclaimer)

        setListener(view)
    }

    private fun setListener(view: View) {

        tvWrongNumber.setOnClickListener { act.goBack() }

        pinView.setAnimationEnable(true)
        pinView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                tvError.visibility = View.INVISIBLE
                if (s.length >= 6) {
                    btnVerify.isEnabled = true
                    Utils.hideKeyboardFrom(activity as Context, view)
                } else {
                    btnVerify.isEnabled = false
                }
            }
        })

        btnResendCode.setOnClickListener {
            errorHandler.handleNetworkConnection {
                if (it) {
                    clearPin()
                    act.resendCode(mPhoneNumber)
                }
            }
            restartTimer()
        }

        btnVerify.setOnClickListener {
            mOtp = pinView.text.toString().trim()
            vm.validateOTP(mOtp) {
                if (it)
                    signIn()
                else
                    updateOTPError(getString(R.string.must_be_six_digit))
            }
        }
    }

    private fun signIn() {
        errorHandler.handleNetworkConnection {
            if (it) {
                if (FirebaseRemoteConfig.getInstance()
                        .getBoolean(RemoteConfigUtils.REMOTE_CONFIG_USE_TT_OTP)
                ) {
                    act.setLoadingEnable(true)
                    mRequestId = act.getRequestID()
                    val createUserRequestData =
                        CreateUserRequestModel.getCreateUserRequestData(
                            act,
                            mRequestId,
                            mPhoneNumber,
                            mOtp
                        )
                    act.createUser(createUserRequestData)
                } else {
                    act.signInWithPhone(
                        mOtp
                    )
                }
            }
        }
    }

    fun updateOTPError(text: String) {
        tvError.text = text
        tvError.visibility = View.VISIBLE
    }

    private fun onUpdateArgument() {
        arguments?.getString("phone_number")?.let { mPhoneNumber = it }
        arguments?.getString("country_name_code")?.let { mSelectedCountryNameCode = it }

        onUpdatePhoneNumber(mPhoneNumber)
        onUpdateCountryNameCode(mSelectedCountryNameCode)
    }

    private fun onUpdatePhoneNumber(num: String) {
        var phoneNumber = num
        if (phoneNumber == null) {
            phoneNumber = ""
        }
        tvSentTo.text = HtmlCompat.fromHtml(
            getString(R.string.otp_sent, "<b>${phoneNumber}</b>"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun onUpdateCountryNameCode(selectedCountryNameCode: String) {
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

    private fun resetTimer() {
        stopWatch?.cancel()
    }

    private fun restartTimer() {
        resetTimer()
        startTimer()
    }

    private fun clearPin() {
        pinView.setText("")
    }

    fun setGreyBgForResendCodeBtn() {
        context?.let {
            btnResendCode.setBackgroundColor(ContextCompat.getColor(it, R.color.grey_10))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWatch?.cancel()
        stopWatch = null
    }

    private fun startTimer() {
        try {
            stopWatch = object : CountDownTimer(COUNTDOWN_DURATION * 1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val finalNumberOfSecondsString = vm.calculateNumOfSecLeft(millisUntilFinished)
                    if (stopWatch != null) {
                        btnResendCode.text = vm.getResendCountDownText(getString(R.string.resend), finalNumberOfSecondsString)
                        btnResendCode.isEnabled = false
                        setGreyBgForResendCodeBtn()
                    }
                }

                override fun onFinish() {
                    if (stopWatch != null) {
                        btnResendCode.text = getString(R.string.resend_otp)
                        btnResendCode.isEnabled = true
                    }
                }
            }
            stopWatch?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            CentralLog.e(TAG, "Timer Error: " + e.message)
        }
        onUpdateArgument()
    }
}
