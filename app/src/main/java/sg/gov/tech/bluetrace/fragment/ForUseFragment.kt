package sg.gov.tech.bluetrace.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_upload_landing.*
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.R

class ForUseFragment : MainActivityFragment("ForUseFragment") {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_upload_landing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        forUseFragmentActionButton.setOnClickListener {
            var myParentFragment: UploadFlowControllerFragment =
                (parentFragment as UploadFlowControllerFragment)
            myParentFragment.goToUploadFragment()
        }
    }

    override fun didProcessBack(): Boolean {
        return false
    }
}
