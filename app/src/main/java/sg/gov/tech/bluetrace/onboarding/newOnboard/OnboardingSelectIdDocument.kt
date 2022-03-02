package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_select_id_document.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.logging.CentralLog
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.WhyNeedDetailDialogFragment

class OnboardingSelectIdDocument : Fragment() {

    var otpAutoFilled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_id_document, container, false)
    }

    override fun onPause() {
        super.onPause()
        btn_next.setOnClickListener(null)
    }

    override fun onResume() {
        super.onResume()

        CentralLog.d("OnboardingSelectIdDocument", "On Resume in Onboarding Select")

        otp_auto_filled_bar.visibility = if (otpAutoFilled) {
            close.setOnClickListener {
                otp_auto_filled_bar.visibility = View.GONE
            }
            View.VISIBLE
        } else {
            View.GONE
        }

        select_nric.setOnClickListener {
            select_nric.setSelected()
            select_fin_work.setUnselected()
            select_fin_dependent.setUnselected()
            select_fin_student.setUnselected()
            select_fin_ltvp.setUnselected()
            select_passport.setUnselected()
            btn_next.isEnabled = true
        }

        select_fin_work.setOnClickListener {
            select_nric.setUnselected()
            select_fin_work.setSelected()
            select_passport.setUnselected()
            select_fin_dependent.setUnselected()
            select_fin_student.setUnselected()
            select_fin_ltvp.setUnselected()
            btn_next.isEnabled = true
        }

        select_fin_dependent.setOnClickListener {
            select_nric.setUnselected()
            select_fin_work.setUnselected()
            select_fin_dependent.setSelected()
            select_fin_student.setUnselected()
            select_fin_ltvp.setUnselected()
            select_passport.setUnselected()
            btn_next.isEnabled = true
        }

        select_fin_student.setOnClickListener {
            select_nric.setUnselected()
            select_fin_work.setUnselected()
            select_fin_dependent.setUnselected()
            select_fin_student.setSelected()
            select_fin_ltvp.setUnselected()
            select_passport.setUnselected()
            btn_next.isEnabled = true
        }

        select_fin_ltvp.setOnClickListener {
            select_nric.setUnselected()
            select_fin_work.setUnselected()
            select_fin_dependent.setUnselected()
            select_fin_student.setUnselected()
            select_fin_ltvp.setSelected()
            select_passport.setUnselected()
            btn_next.isEnabled = true
        }

        select_passport.setOnClickListener {
            select_nric.setUnselected()
            select_fin_work.setUnselected()
            select_fin_dependent.setUnselected()
            select_fin_student.setUnselected()
            select_fin_ltvp.setUnselected()
            select_passport.setSelected()
            btn_next.isEnabled = true
        }

        help.setOnClickListener {
            help.isEnabled = false
            val dialog = WhyNeedDetailDialogFragment()
            dialog.show(childFragmentManager, "WNDDF")
            dialog.dismissListener = DialogInterface.OnDismissListener { help.isEnabled = true }
        }

        btn_next.setOnClickListener {
            it.isEnabled = false
            when {
                select_nric.isSelected -> {
                    (activity as? MainOnboardingActivity)?.goToRegisterUserNRICFragment()
                }
                select_fin_work.isSelected -> {
                    (activity as? MainOnboardingActivity)?.goToRegisterUserWPFragment()
                }
                select_fin_dependent.isSelected -> {
                    (activity as? MainOnboardingActivity)?.goToRegisterUserDPFragment()
                }
                select_fin_student.isSelected -> {
                    (activity as? MainOnboardingActivity)?.goToRegisterUserStpFragment()
                }
                select_fin_ltvp.isSelected -> {
                    (activity as? MainOnboardingActivity)?.goToRegisterUserLtvpFragment()
                }
                select_passport.isSelected -> {
                    (activity as? MainOnboardingActivity)?.goToRegisterUserPassportFragment()
                }
            }
            it.isEnabled = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_SELECT_PROFILE)

    }
}
