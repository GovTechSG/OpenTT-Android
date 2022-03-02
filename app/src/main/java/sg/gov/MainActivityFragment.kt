package sg.gov

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.fragment.app.Fragment
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.TracerApp
import java.util.*

abstract class MainActivityFragment(val customTag: String): Fragment() {
    /**
     * Should pop fragment when it has back stack and return true, otherwise just return false.
     */
    abstract fun didProcessBack(): Boolean

    fun loadLocale(){
        val preferredLanguage = Preference.getPreferredLanguageCode(requireContext())
//        setLocale(preferredLanguage)
        fixLocale(preferredLanguage)
    }

    private fun fixLocale(lang: String){
        val mBackedUpLocale = Locale(lang)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val resources: Resources = resources
            val config: Configuration = resources.configuration
            if (null != mBackedUpLocale && !config.locales.get(0).equals(mBackedUpLocale)) {
                Locale.setDefault(mBackedUpLocale)
                val newConfig = Configuration(config)
                newConfig.setLocale(
                    Locale(
                        mBackedUpLocale.language,
                        mBackedUpLocale.country
                    )
                )
                resources.updateConfiguration(newConfig, null)
            }
            // Also this must be overridden, otherwise for example when opening a dialog the title could have one language and the content other, because
            // different contexts are used to get the resources.
            val appResources: Resources = ContextWrapper(TracerApp.AppContext).applicationContext.resources
            val appConfig: Configuration = appResources.configuration
            if (null != mBackedUpLocale && !appConfig.locales.get(0).equals(mBackedUpLocale)) {
                Locale.setDefault(mBackedUpLocale)
                val newConfig = Configuration(appConfig)
                newConfig.setLocale(
                    Locale(
                        mBackedUpLocale.language,
                        mBackedUpLocale.country
                    )
                )
                appResources.updateConfiguration(newConfig, null)
            }
        }
    }
}
