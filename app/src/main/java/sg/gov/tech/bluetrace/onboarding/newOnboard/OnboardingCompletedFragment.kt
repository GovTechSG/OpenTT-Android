package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.fragment_onboarding_complete.*
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.logging.CentralLog


class OnboardingCompletedFragment : Fragment() {
    private val TAG: String = "OnboardingPermissionFragment"
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_COMPLETED)

        btn_yes.setOnClickListener(View.OnClickListener {
            val act: Activity? = activity
            if (act is MainOnboardingActivity) {
                CentralLog.d(TAG, "OnButtonClick 2")
                Preference.putCheckpoint(view.context, 0)
                Preference.putIsOnBoarded(view.context, true)
                Preference.putOnBoardedWithIdentity(view.context, true)
                Preference.putLastAppUpdatedShown(view.context, BuildConfig.LATEST_UPDATE)

                var intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context?.startActivity(intent)
                (context as MainOnboardingActivity?)?.finish()
            }
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_onboarding_complete, container, false)
    }

}
