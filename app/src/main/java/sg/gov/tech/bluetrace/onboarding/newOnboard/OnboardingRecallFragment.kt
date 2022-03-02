package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_onboarding_recall.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.FragmentInsidePagedNavigation

class OnboardingRecallFragment : Fragment(), FragmentInsidePagedNavigation {
    private val nextActionId =
        R.id.action_onboardingRecallFragment2_to_onboardingHowItWorkSecondFragment
    lateinit var parentActivity: HowItWorksActivity
    private val currentPageNumber = 0

    private val TAG: String = "OnboardingRecallFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_RECALL)
        img_safer.text = HtmlCompat.fromHtml(
            getString(R.string.tracetogether_fancy),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_onboarding_recall, container, false)
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationBar()
    }

    override fun updateBottomNavigationBar() {
        parentActivity = activity as HowItWorksActivity
        parentActivity.nextActionId = nextActionId
        parentActivity.showDotIndicator(true)
        parentActivity.currentPageNumber = currentPageNumber
        parentActivity.setSelectedDot(currentPageNumber)
        parentActivity.setRightNavigationButtonText(R.string.next)
    }
}
