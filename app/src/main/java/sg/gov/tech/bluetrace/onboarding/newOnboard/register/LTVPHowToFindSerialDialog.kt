package sg.gov.tech.bluetrace.onboarding.newOnboard.register

import android.app.Activity
import android.os.Bundle
import android.view.View
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys

class LTVPHowToFindSerialDialog : DialogWithCloseFragment() {
    override fun getLayoutId(): Int {
        return R.layout.dialog_ltvp_serial_how_to_find
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_LTVP_INFO)
    }

}
