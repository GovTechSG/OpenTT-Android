package sg.gov.tech.bluetrace.onboarding.newOnboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import sg.gov.tech.bluetrace.*
import sg.gov.tech.bluetrace.analytics.AnalyticsKeys
import java.util.*


class AppUpdatedV2Fragment : Fragment() {
    private val TAG: String = "AppUpdatedFragment"
    var selected = 0

    private lateinit var contentFrame: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        contentFrame = FrameLayout(requireContext())
//        var inflatedView =  inflater.inflate(R.layout.fragment_app_updated, container, false)
        return inflateContent(contentFrame)
    }

    private fun inflateContent(root: ViewGroup): View {
        return LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_app_updated_v2, root, true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsUtils().screenAnalytics(activity as Activity, AnalyticsKeys.SCREEN_NAME_RE_ON_BOARD)
        setupButtons(view)
    }

    fun setupButtons(view: View) {

        view.findViewById<View>(R.id.btn_ok).setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            Preference.putLastAppUpdatedShown(requireContext(), BuildConfig.LATEST_UPDATE)
            context?.startActivity(intent)
        }

        view.findViewById<View>(R.id.btn_languages).setOnClickListener {
            showPopup()
        }
    }

    fun showPopup() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.change_language))

        val languages = resources.getStringArray(R.array.languages)
        val languageCodesSupported = resources.getStringArray(R.array.languageCodes).toMutableList()

        //get and check current selected language
        val prefLanguageCode = Preference.getPreferredLanguageCode(TracerApp.AppContext)
        var index = languageCodesSupported.indexOf(prefLanguageCode)
        //if can't find, default to english. should never happen unless the prefs / codes array botch up
        if (index < 0) {
            index = 0
        }

        selected = index
        val checkedItem = selected

        builder.setSingleChoiceItems(
            languages,
            checkedItem,
            { dialog, which ->
                selected = which
            })
        builder.setPositiveButton(R.string.ok,
            { dialog, which ->
                val languageSelected = languageCodesSupported[selected]
                setLocale(languageSelected)

                //refresh view?
                contentFrame.removeAllViews()
                inflateContent(contentFrame)
                setupButtons(contentFrame)
            })
        builder.setNegativeButton(R.string.cancel, null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    private fun setLocale(lang: String) {
        val myLocale = Locale(lang)
        val dm = resources.displayMetrics
        val conf = resources.configuration
        conf.setLocale(myLocale)
        resources.updateConfiguration(conf, dm)
        Preference.putPreferredLanguageCode(TracerApp.AppContext, lang)
    }

}
