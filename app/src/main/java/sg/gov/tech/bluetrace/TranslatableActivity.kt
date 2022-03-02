package sg.gov.tech.bluetrace

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.*


open class TranslatableActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //load up language preference
        loadLocale()

        TracerApp.setupDynamicShortcuts(this)
    }

    private fun loadLocale() {
        val preferredLanguage = Preference.getPreferredLanguageCode(this)
//        setLocale(preferredLanguage)
        fixLocale(preferredLanguage)
    }

    private fun fixLocale(lang: String) {
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
            val appResources: Resources = applicationContext.resources
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

//    private fun setLocale(lang: String) {
//        val myLocale = Locale(lang)
//        val dm = applicationContext.resources.displayMetrics
//        val conf = applicationContext.resources.configuration
//        conf.setLocale(myLocale)
//        applicationContext.resources.updateConfiguration(conf, dm)
//        Preference.putPreferredLanguageCode(TracerApp.AppContext, lang)
//    }

    override fun onBackPressed() {
        loadLocale()
        super.onBackPressed()
    }


}
