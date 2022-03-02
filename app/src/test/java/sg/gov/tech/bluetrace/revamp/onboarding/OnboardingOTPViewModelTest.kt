package sg.gov.tech.bluetrace.revamp.onboarding

import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import sg.gov.tech.bluetrace.R
import sg.gov.tech.bluetrace.revamp.settings.BarCodeViewModel
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

class OnboardingOTPViewModelTest {
    private val vm = OnboardingOTPViewModel()
    var result: Boolean = false

    companion object {
        @JvmStatic
        fun otpInputs() = Stream.of(
            Arguments.of(true, "111111"),
            Arguments.of(true, "123456"),
            Arguments.of(false, ""),
            Arguments.of(false, "1"),
            Arguments.of(false, "12"),
            Arguments.of(false, "123"),
            Arguments.of(false, "1234"),
            Arguments.of(false, "12345")
        )

        @JvmStatic
        fun calculateNumOfSecLeftInputs() = Stream.of(
            //Parameter: Valid, Expected result, input
            Arguments.of("59", convertSecToMs(59L)),
            Arguments.of("30", convertSecToMs(30L)),
            Arguments.of("10", convertSecToMs(10L)),
            Arguments.of("09", convertSecToMs(9L)),
            Arguments.of("05", convertSecToMs(5L)),
            Arguments.of("01", convertSecToMs(1L)),
            Arguments.of("00", convertSecToMs(0L))
        )

        @JvmStatic
        fun getResendCountDownTextInputs() = Stream.of(
            //Parameter: Valid, Expected result, input
            Arguments.of("Resend 59s", "59"),
            Arguments.of("Resend 30s", "30"),
            Arguments.of("Resend 10s", "10"),
            Arguments.of("Resend 09s", "09"),
            Arguments.of("Resend 05s", "05"),
            Arguments.of("Resend 01s", "01"),
            Arguments.of("Resend 00s", "00")

        )

        private fun convertSecToMs(seconds: Long): Long {
            return TimeUnit.SECONDS.toMillis(seconds)
        }

    }

    @ParameterizedTest
    @MethodSource("otpInputs")
    fun isOTPCheckValid (valid: Boolean, input: String) {
        vm.validateOTP(input) {
            assertEquals(valid, it)
        }
    }

    @ParameterizedTest
    @MethodSource("calculateNumOfSecLeftInputs")
    fun isNumOfSecValid (expectedValue: String, timeInMs: Long) {
        val result = vm.calculateNumOfSecLeft(timeInMs)
        assertEquals(expectedValue, result)
    }

    @ParameterizedTest
    @MethodSource("getResendCountDownTextInputs")
    fun isCountDownTextValid (expectedValue: String, seconds: String) {
        val result = vm.getResendCountDownText("Resend", seconds)
        assertEquals(expectedValue, result)
    }
}