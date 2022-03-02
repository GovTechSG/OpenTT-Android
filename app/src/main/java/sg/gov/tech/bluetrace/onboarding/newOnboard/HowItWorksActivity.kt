package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import kotlinx.android.synthetic.main.activity_how_it_works.*
import sg.gov.tech.bluetrace.BuildConfig
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.TranslatableActivity

class HowItWorksActivity : TranslatableActivity() {
    var nextActionId: Int = -1
    private val numberOfPages = 2
    var currentPageNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how_it_works)
        handleBottomRightActionButton()
        handleBottomLeftBackButton()
        setNumberOfDot(numberOfPages)
    }

    private fun handleBottomLeftBackButton() {
        btn_back_how_it_works.setOnClickListener {
            when (currentPageNumber) {
                0 -> finish()
                else -> findNavController(how_it_works_navigation_host.id).popBackStack()
            }
        }
    }

    private fun handleBottomRightActionButton() {
        btn_right_action_how_it_works.setOnClickListener {
            if (nextActionId != -1) {
                findNavController(how_it_works_navigation_host.id).navigate(nextActionId)
                nextActionId = -1
            }
        }
    }

    fun setSelectedDot(index: Int) {
        dot_indicator.setSelectedItem(index, true)
    }

    private fun setNumberOfDot(number: Int) {
        dot_indicator.numberOfItems = number
    }

    fun showDotIndicator(shouldShow: Boolean) {
        when (shouldShow) {
            true -> dot_indicator.visibility = View.VISIBLE
            false -> dot_indicator.visibility = View.INVISIBLE
        }
    }

    fun setRightNavigationButtonText(text: Int) {
        btn_right_action_how_it_works.setText(text)
    }

    fun goToWebView(url: String) {
        nextActionId =
            R.id.action_onboardingHowItWorkSecondFragment_to_webViewZendeskSupportFragment
        val bundle = bundleOf("url" to url , "term_privacy" to true)
        showBottomNavBar(false)
        findNavController(how_it_works_navigation_host.id).navigate(nextActionId, bundle)
    }

    private fun showBottomNavBar(show: Boolean) {
        if (show) {
            btn_back_how_it_works.visibility = View.VISIBLE
            dot_indicator.visibility = View.VISIBLE
            btn_right_action_how_it_works.visibility = View.VISIBLE
        }
        else {
            btn_back_how_it_works.visibility = View.GONE
            dot_indicator.visibility = View.GONE
            btn_right_action_how_it_works.visibility = View.GONE
        }
    }

    override fun onBackPressed() {
        showBottomNavBar(true)
        super.onBackPressed()
    }
}
