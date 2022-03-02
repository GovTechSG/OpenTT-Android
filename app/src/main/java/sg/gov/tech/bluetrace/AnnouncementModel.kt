package sg.gov.tech.bluetrace

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*


class AnnouncementModel {
    @SerializedName("text")
    @Expose
    val text: TextModel? = null

    @SerializedName("url")
    @Expose
    val url: String? = null

    @SerializedName("id")
    @Expose
    val id: Int = 0

    @SerializedName("minAppVersion")
    @Expose
    val minAppVersion: String? = null

    @SerializedName("maxAppVersion")
    @Expose
    val maxAppVersion: String? = null

    fun getAnnouncementMsg(): String? {
        val local = Locale.getDefault().language
        var msg = getLocalText(local)
        if (msg.isNullOrBlank()) {
            msg = text?.en
        }
        return msg
    }

    fun getLocalText(local: String): String? {
        return when (local) {
            "en" -> text?.en
            "ms" -> text?.ms
            "ta" -> text?.ta
            "zh" -> text?.zh
            "bn" -> text?.bn
            "hi" -> text?.hi
            "my" -> text?.my
            "th" -> text?.th
            else -> text?.en
        }
    }

    inner class TextModel {
        @SerializedName("en")
        @Expose
        val en: String? = null

        @SerializedName("ms")
        @Expose
        val ms: String? = null

        @SerializedName("ta")
        @Expose
        val ta: String? = null

        @SerializedName("zh")
        @Expose
        val zh: String? = null

        @SerializedName("bn")
        @Expose
        val bn: String? = null

        @SerializedName("hi")
        @Expose
        val hi: String? = null

        @SerializedName("my")
        @Expose
        val my: String? = null

        @SerializedName("th")
        @Expose
        val th: String? = null
    }
}