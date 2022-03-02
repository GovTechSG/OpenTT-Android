package sg.gov.tech.bluetrace.onboarding.newOnboard.register

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import kotlinx.android.synthetic.main.dialog_how_to_find_date_of_application.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys

class EPHowToFindDateOfApplicationDialogFragment : DialogWithCloseFragment() {

    override fun getLayoutId(): Int {
        return R.layout.dialog_how_to_find_date_of_application
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        how_to_find_date_of_app_title_tv.text = HtmlCompat.fromHtml(
            getString(R.string.enter_the_date_of_application),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_EP_INFO)
    }
}
