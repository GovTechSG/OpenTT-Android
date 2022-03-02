package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import kotlinx.android.synthetic.main.fragment_onboarding_love.*
import sg.gov.tech.bluetrace.AnalyticsUtils
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import sg.gov.tech.bluetrace.config.LanguageAdapter
import sg.gov.tech.bluetrace.config.OnItemClickListener
import sg.gov.tech.bluetrace.logging.CentralLog
import java.util.*

class OnboardingWithLoveFragment : Fragment() {
    private val TAG: String = "OnboardingWithLoveFragment"
    private lateinit var mImgLetter: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_onboarding_love, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_ON_BOARD_WITH_LOVE)
        mImgLetter = view.findViewById<ImageView>(R.id.img_with_love)
        mImgLetter.setOnClickListener(View.OnClickListener {
            findNavController().navigate(R.id.action_onboardingWithLoveFragment_to_onboardingDearPeopleFragment)
        })

        setupLanguageSelector()
    }

    private fun setupLanguageSelector() {
        val languagesSupported = resources.getStringArray(R.array.languages)
        val languages = languagesSupported.toMutableList()

        val languageCodesSupported = resources.getStringArray(R.array.languageCodes)
        val languageCodes = languageCodesSupported.toMutableList()

//        val layoutManager = GridLayoutManager(requireActivity(), 3)

        var layoutManager = FlexboxLayoutManager(requireContext())
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.FLEX_START

        val languageAdapter = LanguageAdapter(languages, languageCodes)
        languageAdapter.apply {
            listener = object : OnItemClickListener {
                override fun onItemClick(prevView: View?, view: View, position: Int) {
                    val selectedCode = languageCodes[position]
                    CentralLog.i(TAG, "Language selected: $selectedCode")
                    prevView?.isSelected = false
                    view.isSelected = true
                    setLocale(selectedCode)
                }
            }
        }

        language_selector.layoutManager = layoutManager
        language_selector.adapter = languageAdapter

//        language_selector.apply {
//            layoutManager = layoutManager
//            adapter = languageAdapter
//        }

//        language_selector.recycledViewPool.setMaxRecycledViews(0, 4)

        language_selector.post {

            //mark the current selected one
            val prefLanguageCode = Preference.getPreferredLanguageCode(TracerApp.AppContext)
            var index = languageCodesSupported.indexOf(prefLanguageCode)
            //if can't find, default to english. should never happen unless the prefs / codes array botch up
            if (index < 0) {
                index = 0
            }

            layoutManager.findViewByPosition(index)?.callOnClick()
        }
    }

    private fun setLocale(lang: String) {
        val myLocale = Locale(lang)
        val dm = resources.displayMetrics
        val conf = resources.configuration
        conf.setLocale(myLocale)
        resources.updateConfiguration(conf, dm)
        Preference.putPreferredLanguageCode(TracerApp.AppContext, lang)
        resetTexts()
    }

    private fun resetTexts() {
        tv_main_title.setText(R.string.to_sg_with_love)
        tv_desc.setText(R.string.top_to_open)
    }
}
