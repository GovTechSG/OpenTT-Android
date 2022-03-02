package sg.gov.tech.bluetrace.revamp.requestModel

import android.content.Context
import sg.gov.tech.bluetrace.Preference

class GetPassportStatusRequestModel(
    var ttId: String? = ""
) {

    companion object {
        fun getPassportStatusRequestData(context: Context): GetPassportStatusRequestModel {
            return GetPassportStatusRequestModel(
                ttId = Preference.getTtID(context)
            )
        }
    }
}