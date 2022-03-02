package sg.gov.tech.bluetrace.fragment

import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import sg.gov.tech.bluetrace.fragment.model.PrivacyPolicyDialogViewModel
import sg.gov.tech.bluetrace.revamp.di.modules.apiModule
import sg.gov.tech.bluetrace.revamp.di.modules.dbModule
import sg.gov.tech.bluetrace.revamp.di.modules.utilsModule
import sg.gov.tech.bluetrace.revamp.di.modules.viewModelModule
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream

class PrivacyPolicyDialogViewModelTest: KoinTest {
    private val vm by inject<PrivacyPolicyDialogViewModel>()

    @BeforeEach
    fun before() {
        val startKoin = startKoin {
            modules(module {
                modules(listOf(apiModule, utilsModule, dbModule, viewModelModule))
            })
        }
    }

    @AfterEach
    fun after() {
        stopKoin()
    }

    companion object {

        private fun addMinusDayFromToday(value: Int): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val c = Calendar.getInstance()
            c.add(Calendar.DATE, value)
            val todayDate = c.time
            return sdf.format(todayDate)
        }

        @JvmStatic
        fun isTodayAfterRemindMeDate() = Stream.of(
            //Valid result, cutOffPublishDate
            Arguments.of(false, addMinusDayFromToday(0)), //Today
            Arguments.of(true, addMinusDayFromToday(-1)), //Yesterday
            Arguments.of(false, addMinusDayFromToday(1)), //Tomorrow
            Arguments.of(false, "")
        )

        @JvmStatic
        fun isPolicyAccepted() = Stream.of(
            //Valid result, publishDate, savedPublishDate
            Arguments.of(true, "2021-03-21", "2021-03-21"),
            Arguments.of(false, "2021-03-20", "2021-03-21"),
            Arguments.of(false, "2021-03-22", "2021-03-21"),
            Arguments.of(false, "2021-04-21", "2021-03-21"),
            Arguments.of(false, "2022-03-21", "2021-03-21"),
            Arguments.of(false, "2021-03-21", ""),
            Arguments.of(false, "2021-03-20", ""),
            Arguments.of(false, "2021-03-22", ""),
            Arguments.of(false, "2021-04-21", ""),
            Arguments.of(false, "2022-03-21", ""),
            Arguments.of(false, "", "2021-03-21"),
            Arguments.of(false, "", "")
        )
    }

    @ParameterizedTest
    @MethodSource("isTodayAfterRemindMeDate")
    fun isTodayAfterCutOffDateResultValid(valid: Boolean, date: String) {
        val result = vm.isTodayAfterRemindMeDate(date)
        assertEquals(valid, result)
    }

    @ParameterizedTest
    @MethodSource("isPolicyAccepted")
    fun isPolicyAcceptedResultValid(valid: Boolean, publishDate: String, savedPublishDate: String) {
        val result = vm.isPolicyAccepted(publishDate, savedPublishDate)
        assertEquals(valid, result)
    }
}