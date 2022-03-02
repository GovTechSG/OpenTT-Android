package sg.gov.tech.bluetrace

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class AnnouncementModelTest {

    companion object {

        private const val jsonStringWithEngOnly = "{\"minAppVersion\":\"2.3.8\",\"maxAppVersion\":\"2.4\",\"id\":4,\"text\":{\"en\":\"This is a test announcement\",\"ms\":null,\"ta\":null,\"zh\":null,\"bn\":null,\"hi\":null,\"my\":null,\"th\":null},\"url\":null}"
        private const val jsonStringWithoutText = "{\"minAppVersion\":\"2.3.8\",\"maxAppVersion\":\"2.4\",\"id\":4,\"url\":null}"
        private const val jsonStringWithAllLanguage = "{\"minAppVersion\":\"2.3.8\",\"maxAppVersion\":\"2.4\",\"id\":4,\"text\":{\"en\":\"To the people of Singapore, with love\",\"ms\":\"Kepada warga Singapura tersayang\",\"ta\":\"சிங்கப்பூர் மக்களுக்கு, அன்புடன்\",\"zh\":\"致我们敬爱的新加坡人民\",\"bn\":\"সিংগাপুরের জনগণের প্রতি, ভালবাসা সহকারে\",\"hi\":\"सिंगापुर के लोगों के लिए सप्रेम\",\"my\":\"စင်္ကာပူ ပြည်သူများသို့၊ မေတ္တာဖြင့်\",\"th\":\"ถึงชาวสิงคโปร์ที่รักทุกท่าน\"},\"url\":null}"

        /*
        To test the method with the default locale. Other locale is tested in the other method
         */
        @JvmStatic
        fun announcementInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("This is a test announcement", jsonStringWithEngOnly),
                Arguments.of("To the people of Singapore, with love", jsonStringWithAllLanguage),
                Arguments.of("", jsonStringWithoutText)
            )

        @JvmStatic
        fun localTextInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("This is a test announcement", jsonStringWithEngOnly, "en"),
                Arguments.of("", jsonStringWithEngOnly, "ms"),
                Arguments.of("", jsonStringWithEngOnly, "ta"),
                Arguments.of("", jsonStringWithEngOnly, "zh"),
                Arguments.of("", jsonStringWithEngOnly, "bn"),
                Arguments.of("", jsonStringWithEngOnly, "hi"),
                Arguments.of("", jsonStringWithEngOnly, "my"),
                Arguments.of("", jsonStringWithEngOnly, "my"),
                Arguments.of("", jsonStringWithEngOnly, "th"),
                Arguments.of("To the people of Singapore, with love", jsonStringWithAllLanguage, "en"),
                Arguments.of("Kepada warga Singapura tersayang", jsonStringWithAllLanguage, "ms"),
                Arguments.of("சிங்கப்பூர் மக்களுக்கு, அன்புடன்", jsonStringWithAllLanguage, "ta"),
                Arguments.of("致我们敬爱的新加坡人民", jsonStringWithAllLanguage, "zh"),
                Arguments.of("সিংগাপুরের জনগণের প্রতি, ভালবাসা সহকারে", jsonStringWithAllLanguage, "bn"),
                Arguments.of("सिंगापुर के लोगों के लिए सप्रेम", jsonStringWithAllLanguage, "hi"),
                Arguments.of("စင်္ကာပူ ပြည်သူများသို့၊ မေတ္တာဖြင့်", jsonStringWithAllLanguage, "my"),
                Arguments.of("ถึงชาวสิงคโปร์ที่รักทุกท่าน", jsonStringWithAllLanguage, "th"),
                //For other locale or weird case
                Arguments.of("This is a test announcement", jsonStringWithEngOnly, "jp"),
                Arguments.of("This is a test announcement", jsonStringWithEngOnly, "asdjaskjfaf"),
                Arguments.of("To the people of Singapore, with love", jsonStringWithAllLanguage, "jp"),
                Arguments.of("To the people of Singapore, with love", jsonStringWithAllLanguage, "asdjaskjfaf")
            )
    }

    @ParameterizedTest
    @MethodSource("announcementInputs")
    fun validAnncText(expectedString: String, input: String) {
        val gson = Gson()
        val announcementModel: AnnouncementModel =
            gson.fromJson(input, AnnouncementModel::class.java)
        val anncText = announcementModel.getAnnouncementMsg() ?: ""
        assertEquals(expectedString, anncText)
    }

    @ParameterizedTest
    @MethodSource("localTextInputs")
    fun validLocalString(expectedString: String, input: String, local: String) {
        val gson = Gson()
        val announcementModel: AnnouncementModel =
            gson.fromJson(input, AnnouncementModel::class.java)
        val localText = announcementModel.getLocalText(local) ?: ""
        assertEquals(expectedString, localText)
    }
}