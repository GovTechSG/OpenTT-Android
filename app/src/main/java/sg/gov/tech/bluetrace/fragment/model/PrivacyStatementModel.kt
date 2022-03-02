package sg.gov.tech.bluetrace.fragment.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

class PrivacyStatementModel {
    @SerializedName("policyVersion")
    @Expose
    val policyVersion: String? = null

    @SerializedName("hideRemindMeDate")
    @Expose
    val hideRemindMeDate: String? = null

    @SerializedName("en")
    @Expose
    val privacyEN: PrivacyModel? = null

    @SerializedName("ms")
    @Expose
    val privacyMS: PrivacyModel? = null

    @SerializedName("ta")
    @Expose
    val privacyTA: PrivacyModel? = null

    @SerializedName("zh")
    @Expose
    val privacyZH: PrivacyModel? = null

    @SerializedName("bn")
    @Expose
    val privacyBN: PrivacyModel? = null

    @SerializedName("hi")
    @Expose
    val privacyHI: PrivacyModel? = null

    @SerializedName("my")
    @Expose
    val privacyMY: PrivacyModel? = null

    @SerializedName("th")
    @Expose
    val privacyTH: PrivacyModel? = null

    fun getPrivacyStatement(): PrivacyModel? {
        val model = when (Locale.getDefault().language) {
            "en" -> privacyEN
            "ms" -> privacyMS
            "ta" -> privacyTA
            "zh" -> privacyZH
            "bn" -> privacyBN
            "hi" -> privacyHI
            "my" -> privacyMY
            "th" -> privacyTH
            else -> privacyEN
        }

        return if (model == null || model.isEmpty())
            privacyEN
        else
            model
    }

    inner class PrivacyModel {
        @SerializedName("header")
        @Expose
        val header: TextModel? = null

        @SerializedName("body")
        @Expose
        val body: BodyModel? = null

        @SerializedName("footer")
        @Expose
        val footer: TextModel? = null

        fun isEmpty(): Boolean{
            return header == null || body == null || footer == null
        }
    }

    inner class TextModel {
        @SerializedName("text")
        @Expose
        val text: String? = null

        @SerializedName("urls")
        @Expose
        val urls: ArrayList<UrlModel>? = null
    }

    inner class BodyModel {
        @SerializedName("points")
        @Expose
        val points: ArrayList<TextModel>? = null
    }

    inner class UrlModel {
        @SerializedName("url")
        @Expose
        val url: String? = null

        @SerializedName("startIndex")
        @Expose
        val startIndex: Int? = null

        @SerializedName("length")
        @Expose
        val length: Int? = null
    }
}