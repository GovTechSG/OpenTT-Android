package sg.gov.tech.bluetrace.passport

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import sg.gov.tech.bluetrace.R

class PassportProfileErrorFragment : Fragment() {

    var errorType: Int = 0

    private lateinit var tvErrorTitle: AppCompatTextView
    private lateinit var tvErrorDesc: AppCompatTextView
    private lateinit var btnRetry: AppCompatButton

    companion object {
        const val NETWORK_ISSUE = 0
        const val SERVER_DOWN = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_passport_profile_error, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewAndListener(view)

        if (errorType == NETWORK_ISSUE)
            setUpForNetworkIssue()
        else if (errorType == SERVER_DOWN)
            setUpForServerDown()
    }

    private fun initViewAndListener(view: View) {
        tvErrorTitle = view.findViewById(R.id.tv_error_title)
        tvErrorDesc = view.findViewById(R.id.tv_error_desc)
        btnRetry = view.findViewById(R.id.btn_retry)

        btnRetry.setOnClickListener {
            (activity as PassportProfileActivity).passportCheck()
        }
    }

    private fun setUpForNetworkIssue() {
        tvErrorTitle.text = getString(R.string.check_your_connection)
        tvErrorDesc.text = getString(R.string.there_seems)
    }

    private fun setUpForServerDown() {
        tvErrorTitle.text = getString(R.string.temporarily)
        tvErrorDesc.text = getString(R.string.we_re_reall)
    }
}