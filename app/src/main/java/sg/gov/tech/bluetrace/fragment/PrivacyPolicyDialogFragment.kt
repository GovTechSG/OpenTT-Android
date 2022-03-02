package sg.gov.tech.bluetrace.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.*
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.fragment.model.ConsentPrivacyStatementRequestModel
import sg.gov.tech.bluetrace.fragment.model.PrivacyPolicyDialogViewModel
import sg.gov.tech.bluetrace.fragment.model.PrivacyStatementModel
import sg.gov.tech.bluetrace.revamp.home.HomeFragmentV3
import java.util.*


class PrivacyPolicyDialogFragment(
    private var privacyStatement: PrivacyStatementModel? = null
) : DialogFragment() {
    private val TAG = "PrivacyPolicyDialogFragment"

    private val vm: PrivacyPolicyDialogViewModel by viewModel()

    private lateinit var tvHeader: AppCompatTextView
    private lateinit var llBody: LinearLayout
    private lateinit var tvFooter: AppCompatTextView
    private lateinit var btnAgree: AppCompatButton
    private lateinit var tvRemindMeAgain: AppCompatTextView
    private lateinit var llWebView: LinearLayout
    private lateinit var webView: WebView
    private lateinit var btnWebViewClose: AppCompatImageView

    private var privacyModel: PrivacyStatementModel.PrivacyModel? = null
    private var policyVersion: String = ""
    private var hideRemindMeDate: String = ""

    override fun onResume() {
        super.onResume()
        val params = dialog!!.window!!.attributes
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog!!.window!!.attributes = params
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.setCancelable(false)
    }

    fun getFragmentTag(): String {
        return "privacy_policy"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.privacy_policy_dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)

        privacyStatement?.let {
            policyVersion = it.policyVersion ?: ""
            hideRemindMeDate = it.hideRemindMeDate ?: ""

            privacyModel = it.getPrivacyStatement()
            privacyModel?.let { model ->
                setupUI(model)
            }

        }
    }

    private fun initView(view: View) {
        tvHeader = view.findViewById(R.id.tv_header)
        llBody = view.findViewById(R.id.ll_body)
        tvFooter = view.findViewById(R.id.tv_footer)
        btnAgree = view.findViewById(R.id.btn_agree)
        tvRemindMeAgain = view.findViewById(R.id.tv_remind_me_again)
        llWebView = view.findViewById(R.id.ll_web_view)
        webView = view.findViewById(R.id.web_view)
        btnWebViewClose = view.findViewById(R.id.btn_web_view_close)

        setListener()
    }

    private fun setListener() {
        btnAgree.setOnClickListener {
            Preference.putPrivacyPolicyPolicyVersion(requireContext(), policyVersion)
            activity?.let {
                val fragment = it.supportFragmentManager.findFragmentById(R.id.content)
                if (fragment is SafeEntryFragment) {
                    fragment.checkIfConsentPrivacyPolicyApiSuccess(
                        ConsentPrivacyStatementRequestModel.getConsentPrivacyStatementRequestData(
                            requireContext(),
                            policyVersion
                        )
                    )
                } else if (fragment is HomeFragmentV3) {
                    fragment.checkIfConsentPrivacyPolicyApiSuccess(
                        ConsentPrivacyStatementRequestModel.getConsentPrivacyStatementRequestData(
                            requireContext(),
                            policyVersion
                        )
                    )
                }
                dismissDialog()
            }
        }

        tvRemindMeAgain.setOnClickListener {
            Preference.putShouldShowPrivacyPolicy(requireContext(), false)
            dismissDialog()
        }

        btnWebViewClose.setOnClickListener {
            closeWebView()
        }

        //To support physical back button to close webview
        dialog?.let {
            it.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    closeWebView()
                }
                true
            }
        }
    }

    private fun setupUI(model: PrivacyStatementModel.PrivacyModel) {
        if (vm.isTodayAfterRemindMeDate(hideRemindMeDate))
            tvRemindMeAgain.visibility = View.GONE
        else
            tvRemindMeAgain.visibility = View.VISIBLE

        model.header?.let { headerModel ->
            setLinkInTextView(tvHeader, headerModel)
        }

        model.body?.let { bodyModel ->
            bodyModel.points?.let { pointModels ->
                for (pointModel in pointModels) {
                    updateBodyLayout(pointModel)
                }
            }
        }

        model.footer?.let { footerModel ->
            setLinkInTextView(tvFooter, footerModel)
        }

    }

    private fun updateBodyLayout(textModel: PrivacyStatementModel.TextModel) {
        val textView = createTextView()
        setLinkInTextView(textView, textModel, true)
        llBody.addView(textView)
    }

    private fun createTextView(): AppCompatTextView {
        val textView = AppCompatTextView(context)
        val param = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        param.setMargins(0, 24, 0, 24)
        textView.layoutParams = param
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_14))
        textView.setLineSpacing(22F, 1F)

        return textView
    }

    private fun setLinkInTextView(
        textView: AppCompatTextView,
        textModel: PrivacyStatementModel.TextModel,
        addBulletPoint: Boolean = false
    ) {
        val text: String? = textModel.text
        val urls: ArrayList<PrivacyStatementModel.UrlModel>? = textModel.urls

        if (text == null || urls == null)
            return

        textView.text = text

        for (urlModel in urls) {
            val url: String? = urlModel.url
            val startIndex: Int = urlModel.startIndex ?: 0
            val length: Int = urlModel.length ?: 0

            if (url != null)
                setLink(textView, url, startIndex, length)
        }

        if (addBulletPoint) {
            addBulletPoint(textView)
        }
    }

    private fun setLink(textView: TextView, url: String, startIndex: Int, length: Int) {
        val spannableString = SpannableString(textView.text)

        val totalLength = spannableString.length
        if (startIndex + length > totalLength)
            return

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                openWebView(url)
            }
        }

        //Set link to be clickable
        spannableString.setSpan(
            clickableSpan, startIndex, startIndex + length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        //Set the hyperlink color
        spannableString.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    activity as Context,
                    R.color.blue_text
                )
            ),
            startIndex,
            startIndex + length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.movementMethod =
            LinkMovementMethod.getInstance() // without LinkMovementMethod, link can not click
        textView.setText(spannableString, TextView.BufferType.SPANNABLE)
    }

    private fun addBulletPoint(textView: AppCompatTextView) {
        val spannable = SpannableString(textView.text)
        spannable.setSpan(
            BulletSpan(
                40,
                ContextCompat.getColor(requireContext(), R.color.black)
            ),
            0, spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.setText(spannable, TextView.BufferType.SPANNABLE)
    }


    private fun openWebView(url: String) {
        llWebView.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun closeWebView() {
        if (llWebView.visibility == View.VISIBLE) {
            llWebView.visibility = View.GONE
            webView.loadUrl("")
        }
    }

    private fun dismissDialog() {
        activity?.let {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.content)
            if (fragment is SafeEntryFragment) {
                fragment.isPrivacyPolicyPopUpShown = false
                fragment.updateAllowQRScanning()
                fragment.activityFragmentManager.dismiss(getFragmentTag())
            } else if (fragment is HomeFragmentV3) {
                fragment.activityFragmentManager.dismiss(getFragmentTag())
            }
        }
    }
}
