package sg.gov.tech.bluetrace.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_manage_family_members.barcode_header
import kotlinx.android.synthetic.main.fragment_submit_logs.*
import org.koin.android.viewmodel.ext.android.viewModel
import sg.gov.MainActivityFragment
import sg.gov.tech.bluetrace.ErrorHandler
import sg.gov.tech.bluetrace.MainActivity
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.SafeEntryActivity
import sg.gov.tech.bluetrace.onboarding.newOnboard.viewModels.APIResponse
import sg.gov.tech.bluetrace.utils.AlertType
import sg.gov.tech.bluetrace.utils.TTAlertBuilder

class SubmitLogsFragment : MainActivityFragment("SubmitLogFragment") {

    private val viewModel: SubmitLogViewModel by viewModel()
    private lateinit var errorHandler: ErrorHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_submit_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        errorHandler = ErrorHandler(view.context)
        barcode_header.setTitle(getString(R.string.submit_error_logs))
        barcode_header.showBackNavigationImage()
        barcode_header.setBarcodeClickListener(object : OnBarcodeClick {
            override fun showBarCode() {
                val intent = Intent(activity, SafeEntryActivity::class.java)
                intent.putExtra(SafeEntryActivity.INTENT_EXTRA_PAGE_NUMBER,SafeEntryActivity.ID_FRAGMENT)
                startActivity(intent)
            }

            override fun onBackPress() {
                (activity as MainActivity).onBackPressed()
            }
        })
        submitErrorLogsButton.setOnClickListener {
            submitLogsButtonClicked()
        }
        viewModel.uploadResponse.observe(viewLifecycleOwner, Observer {
            //hide the loading screen
            submitLogFragmentLoadingProgressBarFrame.visibility = View.GONE
            when (it) {
                is APIResponse.Error -> {
                    when (it.message) {
                        viewModel.ZIP_FILE_CREATION_FAILED -> {
                            //display the temporarily unavailable dialog without retry
                            activity?.let { context ->
                                TTAlertBuilder().show(
                                    context,
                                    AlertType.UNABLE_TO_REACH_SERVER
                                )
                            }
                        }
                        viewModel.UNABLE_TO_REACH_SERVER -> {
                            errorHandler.unableToReachServer()
                        }
                    }
                }
                is APIResponse.Success -> {
                    setViewUploadSuccessful()
                }
            }
        })
    }

    private fun setViewUploadSuccessful() {
        scrlUploadLogs.visibility = View.GONE
        rootViewUploadSuccessful.visibility = View.VISIBLE
        submitErrorLogsButton.text = getString(R.string.back_to_home)
    }

    private fun submitLogsButtonClicked() {
        when (submitErrorLogsButton.text) {
            getString(R.string.submit_error_logs) -> {
                //call the function to generate log file and upload it to the server
                activity?.let { context ->
                    errorHandler.handleNetworkConnection {
                        if (it) {
                            submitLogFragmentLoadingProgressBarFrame.visibility = View.VISIBLE
                            viewModel.uploadErrorLogs(context)
                        }
                    }
                }
            }
            getString((R.string.back_to_home)) -> {
                //take the user back to the home
                var parentActivity = activity as MainActivity
                parentActivity.goToHome()
            }
        }
    }

    override fun didProcessBack(): Boolean {
        return false
    }
}
