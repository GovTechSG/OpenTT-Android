package sg.gov.tech.bluetrace.revamp.settings

import androidx.lifecycle.ViewModel
import sg.gov.tech.bluetrace.permissions.FeatureChecker

class PermissionViewModel : ViewModel() {

    private lateinit var listOfChecks: Array<Boolean>
    private lateinit var checkResult: FeatureChecker.CheckResult

    fun simpleCheck(featureChecker: FeatureChecker, simpleCheckComplete: (Boolean) -> Unit) {
        listOfChecks = featureChecker.check()
        if (listOfChecks.reduce { acc, b -> acc && b }) { // all features turned on
            simpleCheckComplete.invoke(true)
        } else {
            featureChecker.setDirty()
            simpleCheckComplete.invoke(false)
        }
    }

    fun checkFeatures(
        featureChecker: FeatureChecker,
        checkFeatureComplete: (Boolean) -> Unit
    ): FeatureChecker.CheckResult? {
        listOfChecks = featureChecker.check()
        checkResult = featureChecker.checkFeatures {
            if (it) {
                checkFeatureComplete.invoke(true)
            } else
                checkFeatureComplete.invoke(false)
        } ?: return null

        return checkResult
    }

    fun checkResult(resultComplete: (Boolean) -> Unit) {
        checkResult.let {
            val allGood = it.checks.reduce { acc, b -> acc && b }
            if (!allGood)
                resultComplete.invoke(false)
            else
                resultComplete.invoke(true)
        }

    }

    fun enableFeatures(featureChecker: FeatureChecker, checkId: String?) {
        featureChecker.enableFeatures(checkId)
    }

    fun clearFeaturesChecker(featureChecker: FeatureChecker) {
        featureChecker.clear()
    }

    fun featurePermissionCallback(
        featureChecker: FeatureChecker,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        featureChecker.permissionCallback(requestCode, permissions, grantResults)
    }

    fun getArrayOfChecks(): Array<Boolean> {
        return listOfChecks
    }

    fun getCheckID(): String {
        return checkResult.id
    }

}