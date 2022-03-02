package sg.gov.tech.bluetrace.fragment.model

import com.google.gson.Gson
import sg.gov.tech.bluetrace.logging.CentralLog
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class PrivacyPolicyValidationModel {

    private val TAG = "PrivacyPolicyValidation"

    companion object {
        const val INVALID_JSON_STRING = 0
        const val FIELD_EMPTY_MISSING = 1
        const val HYPERLINK_INVALID = 2
        const val DATE_FORMAT_INVALID = 3
    }

    fun isValidJSON(privacyStatementRemoteConfig: String): Boolean {
        try {
            val gson = Gson()
            val privacyStatementModel: PrivacyStatementModel = gson.fromJson(
                privacyStatementRemoteConfig,
                PrivacyStatementModel::class.java
            )

            if (!isValidField(privacyStatementModel))
                return false

            val privacyModel = privacyStatementModel.getPrivacyStatement()

            if (!isValidPrivacyModel(privacyModel))
                return false

        } catch (e: Exception) {
            logFailureReason(INVALID_JSON_STRING)
            return false
        }

        CentralLog.d(TAG, "Privacy Policy JSON Validation Pass")
        return true
    }

    /**
     * Should have minimum policyVersion, hideRemindMeDate, en
     * Date format should be correct
     */
    fun isValidField(privacyStatementModel: PrivacyStatementModel): Boolean {
        val policyVersion = privacyStatementModel.policyVersion
        val hideRemindMeDate = privacyStatementModel.hideRemindMeDate

        if (isNullOrEmptyString(policyVersion)) {
            logFailureReason(FIELD_EMPTY_MISSING, "policyVersion")
            return false
        }
        else if (!isDateFormatCorrect(policyVersion))
        {
            logFailureReason(DATE_FORMAT_INVALID, "policyVersion")
            return false
        }

        if (isNullOrEmptyString(hideRemindMeDate)) {
            logFailureReason(FIELD_EMPTY_MISSING, "hideRemindMeDate")
            return false
        }
        else if (!isDateFormatCorrect(hideRemindMeDate)) {
            logFailureReason(DATE_FORMAT_INVALID, "hideRemindMeDate")
            return false
        }

        if (privacyStatementModel.privacyEN == null) {
            logFailureReason(FIELD_EMPTY_MISSING, "privacyEN")
            return false
        }

        return true
    }

    /**
     * Should have minimum header, body, footer
     * header - text, urls
     * body - points (text, urls)
     * footer - text, urls
     */
    fun isValidPrivacyModel(privacyModel: PrivacyStatementModel.PrivacyModel?): Boolean {

        if (privacyModel == null)
        {
            logFailureReason(FIELD_EMPTY_MISSING, "privacyModel")
            return false
        }
        else {

            if (privacyModel.header == null) {
                logFailureReason(FIELD_EMPTY_MISSING, "header")
                return false
            } else {
                if (!isValidTextModel(privacyModel.header, "header"))
                    return false
            }

            if (privacyModel.body == null) {
                logFailureReason(FIELD_EMPTY_MISSING, "body")
                return false
            } else {
                if (!isValidBodyModel(privacyModel.body))
                    return false
            }

            if (privacyModel.footer == null) {
                logFailureReason(FIELD_EMPTY_MISSING, "footer")
                return false
            } else {
                if (!isValidTextModel(privacyModel.footer, "footer"))
                    return false
            }
        }

        return true
    }

    fun isValidBodyModel(bodyModel: PrivacyStatementModel.BodyModel): Boolean {
        val points = bodyModel.points
        if (points == null || points.size == 0) {
            logFailureReason(FIELD_EMPTY_MISSING, "body - points")
            return false
        }
        else {
            for ((index, pointModel) in points.withIndex()) {
                if (!isValidTextModel(pointModel, "body - points[$index]"))
                    return false
            }
        }

        return true
    }

    fun isValidTextModel(txtModel: PrivacyStatementModel.TextModel, parentName: String): Boolean {
        if (isNullOrEmptyString(txtModel.text))
        {
            logFailureReason(FIELD_EMPTY_MISSING, "$parentName - text")
            return false
        }

        if (!isValidUrl(txtModel, parentName)) {
            return false
        }

        return true
    }

    fun isValidUrl(txtModel: PrivacyStatementModel.TextModel, parentName: String): Boolean {

        val text = txtModel.text ?: ""
        val urls: ArrayList<PrivacyStatementModel.UrlModel>? = txtModel.urls

        if (urls != null) {
            for ((index, urlModel) in urls.withIndex()) {
                val url: String? = urlModel.url
                val startIndex: Int? = urlModel.startIndex
                val length: Int? = urlModel.length

                if (isNullOrEmptyString(url)) {
                    logFailureReason(FIELD_EMPTY_MISSING, "$parentName - urls")
                    return false
                }
                else
                {
                    if (startIndex == null) {
                        logFailureReason(
                            FIELD_EMPTY_MISSING,
                            "$parentName - url[$index] - startIndex"
                        )
                        return false
                    }

                    if (length == null) {
                        logFailureReason(FIELD_EMPTY_MISSING, "$parentName - url[$index] - length")
                        return false
                    }

                    if (!isValidStartIndexAndLength(text, startIndex, length)) {
                        logFailureReason(HYPERLINK_INVALID, "$parentName - url[$index]")
                        return false
                    }
                }
            }
        }

        return true
    }

    private fun isValidStartIndexAndLength(text: String, startIndex: Int, length: Int): Boolean {
        val totalLength = text.length
        return startIndex + length <= totalLength
    }

    private fun logFailureReason(reason: Int, desc: String = ""){
        when (reason) {
            INVALID_JSON_STRING -> {
                CentralLog.d(TAG, "JSON string invalid")
            }

            FIELD_EMPTY_MISSING -> {
                CentralLog.d(TAG, "$desc is empty or missing")
            }

            HYPERLINK_INVALID -> {
                CentralLog.d(
                    TAG,
                    "$desc - Invalid hyperlink. startIndex + length should not be more than totalLength of text "
                )
            }

            DATE_FORMAT_INVALID -> {
                CentralLog.d(TAG, "$desc date format is invalid")
            }
        }
    }

    private fun isNullOrEmptyString(value: String?): Boolean {
        return (value == null || value == "")
    }

    fun isDateFormatCorrect(date: String?): Boolean {
        date?.let {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val correctDate = sdf.parse(date)
                if (correctDate != null && date == sdf.format(correctDate)) {
                    return true
                }
            } catch (ex: ParseException) {
                return false
            }
        }

        return false
    }
}