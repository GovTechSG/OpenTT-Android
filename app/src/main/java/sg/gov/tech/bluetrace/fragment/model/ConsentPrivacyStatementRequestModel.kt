package sg.gov.tech.bluetrace.fragment.model

import android.content.Context
import sg.gov.tech.bluetrace.Preference

class ConsentPrivacyStatementRequestModel(
    var ttId: String = "",
    var consentedPrivacyStatementVersion: String = ""
) {

    companion object {
        fun getConsentPrivacyStatementRequestData(context: Context, mConsentedPrivacyStatementVersion: String): ConsentPrivacyStatementRequestModel {
            return ConsentPrivacyStatementRequestModel(
                ttId = Preference.getTtID(context),
                consentedPrivacyStatementVersion = mConsentedPrivacyStatementVersion
            )
        }
    }
}
