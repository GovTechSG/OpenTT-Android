package sg.gov.tech.bluetrace.revamp.setting

import org.junit.Assert.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import sg.gov.tech.bluetrace.revamp.settings.BarCodeViewModel
import java.util.stream.Stream

class BarCodeViewModelTest {
    private val vm = BarCodeViewModel()
    var result: Boolean = false

    companion object {
        @JvmStatic
        fun passportInputs() = Stream.of(
            //Parameter: Valid, Expected result, input
            Arguments.of(true, "PP-A11111111I", "A11111111"),
            Arguments.of(true, "PP-B22222222R", "B22222222"),
            Arguments.of(true, "PP-C33333333-", "C33333333"),
            Arguments.of(false, "PP-A11111111A", "A11111111")
        )
    }

    @ParameterizedTest
    @MethodSource("passportInputs")
    fun isPassportWithCheckSumValid (valid: Boolean, expectedValue: String, input: String) {
        val ppNumWithCheckSum = vm.getPassportNumberWithCheckSum(input)
        result = ppNumWithCheckSum == expectedValue
        assertEquals(valid, result)
    }
}