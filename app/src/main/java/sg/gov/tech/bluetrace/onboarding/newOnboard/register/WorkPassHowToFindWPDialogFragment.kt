package sg.gov.tech.bluetrace.onboarding.newOnboard.register

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import kotlinx.android.synthetic.main.dialog_work_pass_how_to_find.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys

class WorkPassHowToFindWPDialogFragment : DialogWithCloseFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_WP_INFO)

        title_fin.text = HtmlCompat.fromHtml(
            getString(R.string.sn_on_fin),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        title_wp.text = HtmlCompat.fromHtml(
            getString(R.string.for_work_pass),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

    }

    override fun getLayoutId(): Int {
        return R.layout.dialog_work_pass_how_to_find
    }
}
