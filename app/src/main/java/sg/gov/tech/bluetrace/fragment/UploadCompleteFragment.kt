package sg.gov.tech.bluetrace.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_upload_uploadcomplete.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys

class UploadCompleteFragment : Fragment() {

    var isForCPC = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundleArg ->
            isForCPC = bundleArg.getBoolean(ARG_IS_FOR_CPC, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_uploadcomplete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(
            activity as Activity,
            AnalyticsKeys.SCREEN_NAME_UPLOAD_COMPLETED
        )
        uploadCompleteFragmentActionButton.setOnClickListener {
            var parentActivity = activity as MainActivity
            parentActivity.goToHome()
        }

        val takeCareText : TextView = view.findViewById(R.id.take_care)

        takeCareText?.let{
            if(isForCPC){
                it.setText(R.string.stay_safe)
            }
            else{
                it.setText(R.string.take_care)
            }
        }

    }

    companion object {
        const val ARG_IS_FOR_CPC = "ARG_IS_FOR_CPC"
    }
}
