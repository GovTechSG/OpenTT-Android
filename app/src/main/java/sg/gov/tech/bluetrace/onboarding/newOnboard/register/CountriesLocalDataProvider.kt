package sg.gov.tech.bluetrace.onboarding.newOnboard.register

import java.util.*
import kotlin.collections.ArrayList


class CountriesLocalDataProvider(language: String = "EN") : LocalDataProvider<MutableList<String>> {
    private val countriesList: MutableList<String> = ArrayList()
    init {
        val isoCountries: Array<String> = Locale.getISOCountries()
        isoCountries.forEach {
            val locale = Locale(Locale.getDefault().language, it)
            countriesList.add(locale.getDisplayCountry(locale))
            if (!Locale.getDefault().language.contentEquals("en"))
                countriesList.add(locale.getDisplayCountry(Locale.ENGLISH))
        }
    }

    override fun provide(): MutableList<String> = countriesList
    fun provideFiltered(countriesToFilter: Array<String>): MutableList<String> {
        countriesList.removeAll {
            it in countriesToFilter
        }
        return countriesList
    }

}
