package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.extentions.makeLinks
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.FragmentInsidePagedNavigation


class OnboardingHowItWorkSecondFragment : Fragment(), FragmentInsidePagedNavigation {
    private lateinit var parentActivity: HowItWorksActivity
    private val nextActionId =
        R.id.action_onboardingHowItWorkSecondFragment_to_mainOnboardingActivity
    private val currentPageNumber = 1

    private lateinit var tvPrivacyDisclaimer: AppCompatTextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(
            activity as Activity,
            AnalyticsKeys.SCREEN_NAME_ON_BOARD_DATA_USED_ONLY
        )
        tvPrivacyDisclaimer = view.findViewById(R.id.how_it_works_4_text_content)
        tvPrivacyDisclaimer.makeLinks(
            Pair(resources.getString(R.string.serious_offences), View.OnClickListener {
                parentActivity.goToWebView(BuildConfig.SERIOUS_OFFENCES_URL)
            }),
            Pair(resources.getString(R.string.privacy_safeguards), View.OnClickListener {
                parentActivity.goToWebView(BuildConfig.PRIVACY_SAFEGUARDS_URL)
            })
        )
    }

    override fun updateBottomNavigationBar() {
        parentActivity = activity as HowItWorksActivity
        parentActivity.nextActionId = nextActionId
        parentActivity.showDotIndicator(true)
        parentActivity.currentPageNumber = currentPageNumber
        parentActivity.setSelectedDot(currentPageNumber)
        parentActivity.setRightNavigationButtonText(R.string.lets_go)
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationBar()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_onboarding_how_it_works_2, container, false)
    }

}
