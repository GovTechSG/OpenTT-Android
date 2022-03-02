package sg.gov.tech.bluetrace.revamp.settings

import android.app.AlertDialog
import android.content.Context
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import sg.gov.tech.bluetrace.Preference
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.TracerApp
import sg.gov.tech.bluetrace.onboarding.newOnboard.register.IdentityType
import sg.gov.tech.bluetrace.streetpass.persistence.StreetPassRecordDatabase
import java.util.*

class SettingsViewModel : ViewModel() {
    private var disposable: Disposable? = null


    fun getVersionName(context: Context): String {
        var localAppVersion = context.packageManager?.getPackageInfo(context.packageName, 0)
            ?.versionName?.split("-")
            ?.get(0).orEmpty()
        var version = context.getString(R.string.app_version)
        return "$version $localAppVersion"
    }

    fun setLocale(context: Context, lang: String) {
        val myLocale = Locale(lang)
        val dm = context.resources.displayMetrics
        val conf = context.resources.configuration
        conf.setLocale(myLocale)
        context.resources.updateConfiguration(conf, dm)
        Preference.putPreferredLanguageCode(TracerApp.AppContext, lang)
    }

    fun getUserIdType(): IdentityType {
        val idType = Preference.getUserIdentityType(TracerApp.AppContext)
        return IdentityType.findByValue(idType)
    }

    fun navigateToManageFamily(context: Context, isAdded: (Boolean) -> Unit) {
        if (Preference.isManageFamilyMemNew(context))
            Preference.putIsManageFamilyMemNew(context, false)

        disposable = Observable.create<Int> {
            val count = StreetPassRecordDatabase.getDatabase(context).familyMemberDao()
                .getFamilyMembersCount()
            it.onNext(count)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { count ->
                if (count > 0) {
                    isAdded.invoke(true)
                } else {
                    isAdded.invoke(false)
                }
            }
    }

    fun showPopup(context: Context, onLangSelected: (String) -> Unit) {
        var selected = 0
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.change_language))

        val languages = context.resources.getStringArray(R.array.languages)
        val languageCodesSupported =
            context.resources.getStringArray(R.array.languageCodes).toMutableList()

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
            checkedItem
        ) { _, which ->
            selected = which
        }
        builder.setPositiveButton(
            R.string.ok
        ) { dialog, which ->
            onLangSelected.invoke(languageCodesSupported[selected])
        }
        builder.setNegativeButton(R.string.cancel, null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
    }
}