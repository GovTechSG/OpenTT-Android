package sg.gov.tech.bluetrace.passport

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.Utils
import sg.gov.tech.bluetrace.onboarding.newOnboard.MainOnboardingActivity

class PassportProfileBlockedFragment : Fragment() {
    private lateinit var tvHelp: AppCompatTextView
    private lateinit var cardDetails: CardView

    private lateinit var tvDetails1: AppCompatTextView
    private lateinit var tvDetails2: AppCompatTextView
    private lateinit var tvDetails3: AppCompatTextView
    private lateinit var tvDetails4: AppCompatTextView
    private lateinit var tvDetails5: AppCompatTextView
    private lateinit var tvDetails6: AppCompatTextView

    private lateinit var tvMoreDetails: AppCompatTextView
    private lateinit var tvSeeLess: AppCompatTextView
    private lateinit var btnReRegister: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_passport_profile_blocked, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewAndListener(view)
    }

    private fun initViewAndListener(view: View) {
        tvHelp = view.findViewById(R.id.tv_help)
        cardDetails = view.findViewById(R.id.card_details)
        tvDetails1 = view.findViewById(R.id.tv_details_1)
        tvDetails2 = view.findViewById(R.id.tv_details_2)
        tvDetails3 = view.findViewById(R.id.tv_details_3)
        tvDetails4 = view.findViewById(R.id.tv_details_4)
        tvDetails5 = view.findViewById(R.id.tv_details_5)
        tvDetails6 = view.findViewById(R.id.tv_details_6)
        btnReRegister = view.findViewById(R.id.btn_re_register)

        tvMoreDetails = view.findViewById(R.id.tv_more_details)
        tvSeeLess = view.findViewById(R.id.tv_see_less)

        setUpTextAndView()
        setOnClickListener()
    }

    private fun setUpTextAndView()
    {
        tvMoreDetails.paintFlags = tvMoreDetails.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        tvSeeLess.paintFlags = tvSeeLess.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        tvDetails1.text = HtmlCompat.fromHtml(
            getString(R.string.pp_profile_details_1),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        tvDetails2.text = HtmlCompat.fromHtml(
            getString(R.string.pp_profile_details_2),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        tvDetails3.text = HtmlCompat.fromHtml(
            getString(R.string.pp_profile_details_3),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        tvDetails4.text = HtmlCompat.fromHtml(
            getString(R.string.pp_profile_details_4),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        tvDetails5.text = HtmlCompat.fromHtml(
            getString(R.string.pp_profile_details_5),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        tvDetails6.text = HtmlCompat.fromHtml(
            getString(R.string.pp_profile_details_6),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    private fun setOnClickListener()
    {
        tvHelp.setOnClickListener {
            (activity as PassportProfileActivity).goToWebViewFragment(BuildConfig.ZENDESK_PP_PROFILE_BLOCKED_HELP_URL)
        }

        tvMoreDetails.setOnClickListener {
            tvMoreDetails.visibility = View.GONE
            cardDetails.visibility = View.VISIBLE
        }

        tvSeeLess.setOnClickListener {
            tvMoreDetails.visibility = View.VISIBLE
            cardDetails.visibility = View.GONE
        }

        btnReRegister.setOnClickListener {
            showReRegistrationDialog()
        }
    }

    private fun showReRegistrationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        var message = getString(R.string.invalid_user_re_registration_detail_text)
        builder.setTitle(getString(R.string.re_registration_title))
            .setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setPositiveButton(getString(R.string.proceed)) { dialog, _ ->
                dialog.cancel()
                clearAppData()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setCancelable(false)
        builder.create().show()
    }

    private fun clearAppData() {
        CoroutineScope(Dispatchers.IO).launch {
            Utils.clearDataAndStopBTService(requireContext())
            navigateToOnBoarding()
        }
    }

    private fun navigateToOnBoarding() {
        startActivity(Intent(requireContext(), MainOnboardingActivity::class.java))
        activity?.finish()
    }
}