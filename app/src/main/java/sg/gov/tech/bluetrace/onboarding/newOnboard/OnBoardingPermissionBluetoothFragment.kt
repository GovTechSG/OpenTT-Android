package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_help_you_note_possible_exposure.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.extentions.makeLinks

class OnBoardingPermissionBluetoothFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_BT_PERMISSION)
        permission_bt_btn_allow.setOnClickListener {
            (activity as MainOnboardingActivity?)?.requestPermissions()
        }

        val msg = getString(R.string.prominent_android_needs_location_permission) +
                "\n\n${getString(R.string.prominent_bluetooth_data_deleted)}" +
                "\n\n${getString(R.string.how_it_works_2_text_content_3)}"

        tv_details.text = msg
        tv_details.makeLinks(
            Pair(resources.getString(R.string.serious_offences), View.OnClickListener {
                (activity as MainOnboardingActivity?)?.goToWebViewFragment(BuildConfig.SERIOUS_OFFENCES_URL)
            }),
            Pair(resources.getString(R.string.privacy_safeguards), View.OnClickListener {
                (activity as MainOnboardingActivity?)?.goToWebViewFragment(BuildConfig.PRIVACY_SAFEGUARDS_URL)
            })
        )

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q){
            val locationPermission: TextView = view.findViewById<TextView>(R.id.tv_location_permission)
            locationPermission.text = getString(R.string.location_permission_adr11, getString(R.string.location_permission), getString(R.string.allow_while_using_app))
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help_you_note_possible_exposure, container, false)
    }
}
