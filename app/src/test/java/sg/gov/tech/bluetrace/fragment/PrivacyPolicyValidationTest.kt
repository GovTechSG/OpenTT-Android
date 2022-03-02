package sg.gov.tech.bluetrace.fragment

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import sg.gov.tech.bluetrace.fragment.model.PrivacyPolicyValidationModel
import sg.gov.tech.bluetrace.fragment.model.PrivacyStatementModel
import java.util.stream.Stream

class PrivacyPolicyValidationTest {
    private val model = PrivacyPolicyValidationModel()

    companion object {
        //Valid fields
        var policyVersion = "\"policyVersion\":\"2021-03-05\""
        var hideRemindMeDate = "\"hideRemindMeDate\":\"2021-03-15\""
        var header = "\"header\":{\"text\":\"With the amendments to the COVID-19 (Temporary Measures) Act, we've updated our privacy policy to continue protecting your data.\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"
        var body = "\"body\":{\"points\":[{\"text\":\"Data shared with MOH can only be used for contact tracing. The only exception is when the Police needs the data to investigate serious offences.\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":127,\"length\":16}]},{\"text\":\"If contact tracing data from your device is needed by the Police, you'll be informed and guided on how to upload the data.\",\"urls\":[]},{\"text\":\"Once contact tracing is no longer needed, we'll stop collecting personal contact tracing data. All personal contact tracing data uploaded will be deleted.\",\"urls\":[]}]}"
        var footer = "\"footer\":{\"text\":\"By tapping Agree, you accept the new privacy policy. After 15 03 2021, you'll need to accept this update to continue using TraceTogether. See Help if you'd like more details\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":37,\"length\":14},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":142,\"length\":4}]}"
        var en = "\"en\":{$header,$body,$footer}"
        var zh = "\"zh\":{\"header\":{\"text\":\"以后只要到我国人流量高的地方都离不开“合力追踪”了。\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":19,\"length\":4}]},\"body\":{\"points\":[{\"text\":\"抗疫跨部门工作小组昨天（20日）宣布，下来两个月将有更多场所要求访客必须以“合力追踪”（TraceTogether）手机应用或携手防疫器（token）登记入场，而不是扫描身份证或SafeEntry QR码。\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":38,\"length\":4}]},{\"text\":\"“合力追踪”是一项协助新加坡政府加强冠病患者密切接触者追踪工作的计划。“合力追踪”应用于今年3月20日发布，携手防疫器则在今年6月28日推出，方便没有手机或手机功能不足的公众使用。\",\"urls\":[]},{\"text\":\"两个系统相辅相成，协助卫生部把追踪工作从平均所需的四天减少至两天，大幅提高追踪病例接触者的速度和准确性。\",\"urls\":[]}]},\"footer\":{\"text\":\"点击同意即表示您接受新的隐私保护措施。 在 15 March 2021 之后，您需要接受此更新才能继续使用TraceTogether。如需更多详细信息，请参阅“帮助”。\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":37,\"length\":18},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":2}]}}"

        var defaultCorrectJson = "{$policyVersion, $hideRemindMeDate, $en, $zh}"


        fun convertToPrivacyStatementModel(jsonString: String): PrivacyStatementModel {
            val gson = Gson()
            return gson.fromJson(
                jsonString,
                PrivacyStatementModel::class.java
            )
        }

        fun convertToPrivacyModel(jsonString: String): PrivacyStatementModel.PrivacyModel {
            val gson = Gson()
            return gson.fromJson(
                jsonString,
                PrivacyStatementModel.PrivacyModel::class.java
            )
        }

        fun convertToBodyModel(jsonString: String): PrivacyStatementModel.BodyModel {
            val gson = Gson()
            return gson.fromJson(
                jsonString,
                PrivacyStatementModel.BodyModel::class.java
            )
        }

        fun convertToTextModel(jsonString: String): PrivacyStatementModel.TextModel {
            val gson = Gson()
            return gson.fromJson(
                jsonString,
                PrivacyStatementModel.TextModel::class.java
            )
        }

        @JvmStatic
        fun isValidJSON(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, defaultCorrectJson), //All valid
                Arguments.of(false, "{"),
                Arguments.of(false, "}"),
                Arguments.of(false, ",1"),
                Arguments.of(false, "\"\""),
                Arguments.of(false, "asdasd"),
                Arguments.of(false, "{as:,"),
                Arguments.of(false, "{sd:\"}"),
                Arguments.of(false, " ")
            )

        @JvmStatic
        fun isValidField(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, "{\"policyVersion\":\"2021-03-05\",\"hideRemindMeDate\":\"2021-03-15\",$en}"), //All valid
                Arguments.of(false, "{\"policyVersion\":\"\",\"hideRemindMeDate\":\"2021-03-15\",$en}"), //Empty policyVersion
                Arguments.of(false, "{\"hideRemindMeDate\":\"2021-03-15\",$en}"), //Missing policyVersion
                Arguments.of(false, "{\"policyVersion\":\"2021-03-05\",\"hideRemindMeDate\":\"\",$en}"), //Empty hideRemindMeDate
                Arguments.of(false, "{\"policyVersion\":\"2021-03-05\",$en}"), //Missing hideRemindMeDate
                Arguments.of(false, "{\"policyVersion\":\"2021-03-05\",\"hideRemindMeDate\":\"2021-03-15\"}"), //Missing en
                Arguments.of(false, "{\"policyVersion\":\"\",\"hideRemindMeDate\":\"\",$en}"), //Both policyVersion and hideRemindMeDate empty
                Arguments.of(false, "{$en}"), //Both policyVersion and hideRemindMeDate missing
                Arguments.of(false, "{\"policyVersion\":\"2021-03-05\"}"), //Both hideRemindMeDate & en missing
                Arguments.of(false, "{\"hideRemindMeDate\":\"2021-03-05\"}"), //Both policyVersion and en missing
                Arguments.of(false, "{}"), //All missing
                //Typo test - E.g. policyversion instead of policyVersion
                Arguments.of(false, "{\"policyversion\":\"2021-03-05\",\"hideremindmedate\":\"2021-03-15\",$en}")
            )

        @JvmStatic
        fun isValidPrivacyModel(): Stream<Arguments> =
            Stream.of(
                Arguments.of(true, "{$header,$body,$footer}"), //All Valid
                Arguments.of(false, "{\"header\":{},$body,$footer}"), //Empty header
                Arguments.of(false, "{$body,$footer}"), //Missing header
                Arguments.of(false, "{$header,\"body\":{},$footer}"), //Empty body
                Arguments.of(false, "{$header,$footer}"), //Missing body
                Arguments.of(false, "{$header,$body,\"footer\":{}}"), //Empty footer
                Arguments.of(false, "{$header,$body}"), //Missing footer
                Arguments.of(false, "{\"header\":{},\"body\":{},$footer}"), //Empty header & body
                Arguments.of(false, "{$header,\"body\":{},\"footer\":{}}"), //Empty body & footer
                Arguments.of(false, "{\"header\":{},$body,\"footer\":{}}"), //Empty header & footer
                Arguments.of(false, "{\"header\":{},\"body\":{},\"footer\":{}}"), //All empty
                Arguments.of(false, "{$footer}"), //Missing header & body
                Arguments.of(false, "{$header}"), //Missing body & footer
                Arguments.of(false, "{$body}"), //Missing header & footer
                Arguments.of(false, "{}"), //All missing
                //Typo test - Wrong header field
                Arguments.of(false, "{\"heaer\":{\"text\":\"With the amendments to the COVID-19 (Temporary Measures) Act, we've updated our privacy policy to continue protecting your data.\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]},$body,$footer}")
            )

        @JvmStatic
        fun isValidBodyModel(): Stream<Arguments> =
            Stream.of(
                //All Valid
                Arguments.of(true, "{\"points\":[{\"text\":\"Data shared with MOH can only be used for contact tracing. The only exception is when the Police needs the data to investigate serious offences.\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":127,\"length\":16}]},{\"text\":\"If contact tracing data from your device is needed by the Police, you'll be informed and guided on how to upload the data.\",\"urls\":[]},{\"text\":\"Once contact tracing is no longer needed, we'll stop collecting personal contact tracing data. All personal contact tracing data uploaded will be deleted.\",\"urls\":[]}]}"), //All Valid
                Arguments.of(false, "{\"points\":[]}"), //Empty points
                Arguments.of(false, "{\"points\":[{}]}"), //1 pointModel but empty inside
                Arguments.of(false, "{\"points\":[{},{}]}"), //2 pointModel but both empty inside
                //2 pointModel but first empty
                Arguments.of(false, "{\"points\":[{},{\"text\":\"Data shared with MOH can only be used for contact tracing. The only exception is when the Police needs the data to investigate serious offences.\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":127,\"length\":16}]}]}"),
                Arguments.of(false, "{}"), //Totally empty
                //Typo test - point instead of points
                Arguments.of(false, "{\"point\":[{\"text\":\"Data shared with MOH can only be used for contact tracing. The only exception is when the Police needs the data to investigate serious offences.\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":127,\"length\":16}]},{\"text\":\"If contact tracing data from your device is needed by the Police, you'll be informed and guided on how to upload the data.\",\"urls\":[]},{\"text\":\"Once contact tracing is no longer needed, we'll stop collecting personal contact tracing data. All personal contact tracing data uploaded will be deleted.\",\"urls\":[]}]}") //All Valid
            )

        @JvmStatic
        fun isValidTextModel(): Stream<Arguments> =
            Stream.of(
                //All Valid
                Arguments.of(true, "{\"text\":\"Data shared with MOH can only be used for contact tracing. The only exception is when the Police needs the data to investigate serious offences.\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":127,\"length\":16}]}"),
                //Empty text
                Arguments.of(false, "{\"text\":\"\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":127,\"length\":16}]}"),
                //Missing text
                Arguments.of(false, "{\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":127,\"length\":16}]}"),
                Arguments.of(false, "{}"), //Totally empty
                //Typo test - tex instead of text
                Arguments.of(false, "{\"tex\":\"Data shared with MOH can only be used for contact tracing. The only exception is when the Police needs the data to investigate serious offences.\",\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":127,\"length\":16}]}")
            )

        var textExample = "\"text\":\"With the amendments to the COVID-19 (Temporary Measures) Act, we've updated our privacy policy to continue protecting your data.\""
        @JvmStatic
        fun isValidUrl(): Stream<Arguments> =
            Stream.of(
                //All Valid
                Arguments.of(true, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //Empty first url
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //Missing first url
                Arguments.of(false, "{$textExample,\"urls\":[{\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //Missing first startIndex
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //Missing first length
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //Missing first startIndex and length
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\"},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //Total empty for first url
                Arguments.of(false, "{$textExample,\"urls\":[{},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //Empty 2nd url
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"\",\"startIndex\":80,\"length\":14}]}"),
                //Missing 2nd url
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"startIndex\":80,\"length\":14}]}"),
                //Missing 2nd startIndex
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"length\":14}]}"),
                //Missing 2nd length
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80}]}"),
                //Missing 2nd startIndex and length
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\"}]}"),
                //Total empty for 2nd url
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{}]}"),
                //Both url empty
                Arguments.of(false, "{$textExample,\"urls\":[{},{}]}"),
                //urls empty *note: url can be empty
                Arguments.of(true, "{$textExample,\"urls\":[]}"),
                //urls missing
                Arguments.of(true, "{$textExample}"),
                //Test on isValidStartIndexAndLength
                //For 1st url, startIndex too big
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":1000,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //For 1st url, length too big
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":1000},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //For 1st url, startIndex and length too big
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":1000,\"length\":1000},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}"),
                //For 2nd url, startIndex too big
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":1000,\"length\":14}]}"),
                //For 2nd url, length too big
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":1000}]}"),
                //For 2nd url, startIndex and length too big
                Arguments.of(false, "{$textExample,\"urls\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":1000,\"length\":1000}]}"),
                //Typo test - url instead of urls - Will treat urls as missing which is allowed so true
                Arguments.of(true, "{$textExample,\"url\":[{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":27,\"length\":33},{\"url\":\"https://www.tracetogether.gov.sg\",\"startIndex\":80,\"length\":14}]}")

        )

        @JvmStatic
        fun isDateFormatCorrect(): Stream<Arguments> =
            Stream.of(
                //Valid date format yyyy-MM-dd
                Arguments.of(true, "2021-03-05"),
                Arguments.of(true, "2021-12-24"),
                //Invalid number
                Arguments.of(false, "2021-13-05"),
                Arguments.of(false, "2021-12-32"),
                Arguments.of(false, "2021-12-00"),
                Arguments.of(false, "2021-00-12"),
                //Invalid format
                Arguments.of(false, "2021-3-05"),
                Arguments.of(false, "2021-03-5"),
                Arguments.of(false, "2021-3-5"),
                Arguments.of(false, "21-03-05"),
                Arguments.of(false, "2021 03 05"),
                Arguments.of(false, "2021 3 05"),
                Arguments.of(false, "2021 03 5"),
                Arguments.of(false, "2021 3 5"),
                Arguments.of(false, "05-03-2021"),
                Arguments.of(false, "5-03-2021"),
                Arguments.of(false, "05-3-2021"),
                Arguments.of(false, "5-3-2021"),
                Arguments.of(false, "05-03-21"),
                Arguments.of(false, "05 03 2021"),
                Arguments.of(false, "5 03 2021"),
                Arguments.of(false, "05 3 2021"),
                Arguments.of(false, "5 3 2021"),
                Arguments.of(false, "5 Mar 2021"),
                Arguments.of(false, "5 Mar 21"),
                Arguments.of(false, "5 March 2021"),
                Arguments.of(false, "5 March 21"),
                Arguments.of(false, "5-Mar-2021"),
                Arguments.of(false, "5-Mar-21")

            )
    }

    /**
     * Check for valid json and malformed JSON only
     */
    @ParameterizedTest
    @MethodSource("isValidJSON")
    fun validJSON(expected: Boolean, jsonString: String) {
        val result = model.isValidJSON(jsonString)
        assertEquals(expected, result)
    }

    /**
     * Check for policyVersion, hideRemindMeDate and en only (Without checking date format)
     */
    @ParameterizedTest
    @MethodSource("isValidField")
    fun validField(expected: Boolean, jsonString: String) {
        val mModel: PrivacyStatementModel = convertToPrivacyStatementModel(jsonString)
        val result = model.isValidField(mModel)
        assertEquals(expected, result)
    }

    /**
     * Check for header, body, footer only
     */
    @ParameterizedTest
    @MethodSource("isValidPrivacyModel")
    fun validPrivacyModel(expected: Boolean, jsonString: String) {
        val mModel: PrivacyStatementModel.PrivacyModel = convertToPrivacyModel(jsonString)
        val result = model.isValidPrivacyModel(mModel)
        assertEquals(expected, result)
    }

    /**
     * Check for points only
     */
    @ParameterizedTest
    @MethodSource("isValidBodyModel")
    fun validBodyModel(expected: Boolean, jsonString: String) {
        val mModel: PrivacyStatementModel.BodyModel = convertToBodyModel(jsonString)
        val result = model.isValidBodyModel(mModel)
        assertEquals(expected, result)
    }

    /**
     * Check for text only
     */
    @ParameterizedTest
    @MethodSource("isValidTextModel")
    fun validTextModel(expected: Boolean, jsonString: String) {
        val mModel: PrivacyStatementModel.TextModel = convertToTextModel(jsonString)
        val result = model.isValidTextModel(mModel, "test")
        assertEquals(expected, result)
    }

    /**
     * Check for url and isValidStartIndexAndLength only
     */
    @ParameterizedTest
    @MethodSource("isValidUrl")
    fun validUrl(expected: Boolean, jsonString: String) {
        val mModel: PrivacyStatementModel.TextModel = convertToTextModel(jsonString)
        val result = model.isValidUrl(mModel, "test")
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource("isDateFormatCorrect")
    fun dateFormatCorrect(expected: Boolean, dateString: String) {
        val result = model.isDateFormatCorrect(dateString)
        assertEquals(expected, result)
    }
}
