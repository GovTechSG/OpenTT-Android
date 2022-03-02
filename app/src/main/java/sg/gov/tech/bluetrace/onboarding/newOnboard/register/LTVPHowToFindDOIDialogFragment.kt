package sg.gov.tech.bluetrace.onboarding.newOnboard.register

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.dialog_stp_ltvp_how_to_find.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys

class LTVPHowToFindDOIDialogFragment : DialogWithCloseFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_STPLTVPP_INFO)
        // add rounded corner for OR background. Not using drawable since it is not reusable anywhere.
        val shape = GradientDrawable()
        shape.cornerRadius = 2f
        shape.color =
            ColorStateList.valueOf(ContextCompat.getColor(context!!, R.color.unselected_accent))
        tOr.background = shape
    }

    override fun getLayoutId(): Int {
        return R.layout.dialog_stp_ltvp_how_to_find
    }

}
