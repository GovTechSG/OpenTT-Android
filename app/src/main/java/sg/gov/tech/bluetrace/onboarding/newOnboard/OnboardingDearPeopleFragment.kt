package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys

class OnboardingDearPeopleFragment : Fragment() {
    private val TAG: String = "OnboardingDearPeopleFragment"
    private lateinit var mImgNext: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_DEAR_SG)
        mImgNext = view.findViewById<ImageView>(R.id.img_next)
        mImgNext.setOnClickListener(View.OnClickListener {
            findNavController().navigate(R.id.action_onboardingDearPeopleFragment_to_howItWorksActivity)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_onboarding_dear, container, false)
    }
}
