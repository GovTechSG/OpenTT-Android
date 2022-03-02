package sg.gov.tech.bluetrace.fragment

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.mock
import sg.gov.tech.bluetrace.revamp.home.HomeViewModel
import sg.gov.tech.bluetrace.revamp.di.modules.apiModule
import sg.gov.tech.bluetrace.revamp.di.modules.dbModule
import sg.gov.tech.bluetrace.revamp.di.modules.utilsModule
import sg.gov.tech.bluetrace.revamp.di.modules.viewModelModule
import java.util.stream.Stream

class HomeViewModelTest: KoinTest {

    private val homeVM by inject<HomeViewModel>()

    /*
    Need to use
    - @BeforeEach and @AfterEach instead of @Before and @After
    - Include androidContext in startKoin
    is because HomeViewModel require DAO in the parameter which needs the context
     */

    @BeforeEach
    fun before() {
        startKoin {
            androidContext(mock(Context::class.java))
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
        @JvmStatic
        fun getConnectedDeviceRangeInputs(): Stream<Arguments> =
            Stream.of(
                //Parameter: Correct text output, Number of actual nearby device
                Arguments.of("0", 0), //0 Device, Range Correct
                Arguments.of("1-5", 1), //Boundary Value Upper
                Arguments.of("1-5", 3), //Value in range
                Arguments.of("1-5", 5), //Boundary Value Bottom
                Arguments.of("6-10", 7), //Value greater than 5 in range
                Arguments.of("11-15", 11), //Upper Boundary Value Greater Than 5
                Arguments.of("21-25", 25) //Bottom Boundary Value Greater Than 5
            )

        @JvmStatic
        fun getTimeInMilliInputs(): Stream<Arguments> =
            Stream.of(
                //Parameter: Correct time output in Milli, input
                Arguments.of(1800000, 0.5),
                Arguments.of(7200000, 2.0),
                Arguments.of(28800000, 8.0),
                //Some other random value
                Arguments.of(5400000, 1.5),
                Arguments.of(36000000, 10)
            )

        @JvmStatic
        fun displayAnnouncementAppVersionCheckInputs(): Stream<Arguments> =
            Stream.of(
                //Parameter: expected boolean result, current app version, min app version, max app version
                //Test in range and exact cases
                Arguments.of(true, "2.6.0", "2.3.8", "2.7"),
                Arguments.of(true, "2.3.8", "2.3.8", "2.7"),
                Arguments.of(true, "2.7", "2.3.8", "2.7"),
                Arguments.of(true, "2.7.0", "2.3.8", "2.7"),
                //Test lesser than min and bigger than max
                Arguments.of(false, "2.1.0", "2.3.8", "2.7"),
                Arguments.of(false, "2.3.7", "2.3.8", "2.7"),
                Arguments.of(false, "2.7.1", "2.3.8", "2.7"),
                Arguments.of(false, "2.8.0", "2.3.8", "2.7"),
                //Test when any of the value is blank
                Arguments.of(false, "", "2.3.8", "2.7"),
                Arguments.of(true, "2.6.0", "", "2.7"),
                Arguments.of(false, "2.9.0", "", "2.7"),
                Arguments.of(true, "2.6.0", "2.3.8", ""),
                Arguments.of(false, "2.2.0", "2.3.8", ""),
                Arguments.of(false, "", "", "2.7"),
                Arguments.of(false, "2.6.0", "", ""),
                Arguments.of(false, "", "2.3.8", ""),
                Arguments.of(false, "", "", ""),
                //Same as the test case for blank but change to null (Result should be the same)
                Arguments.of(false, null, "2.3.8", "2.7"),
                Arguments.of(true, "2.6.0", null, "2.7"),
                Arguments.of(false, "2.9.0", null, "2.7"),
                Arguments.of(true, "2.6.0", "2.3.8", null),
                Arguments.of(false, "2.2.0", "2.3.8", null),
                Arguments.of(false, null, null, "2.7"),
                Arguments.of(false, "2.6.0", null, null),
                Arguments.of(false, null, "2.3.8", null),
                Arguments.of(false, null, null, null)
            )

        @JvmStatic
        fun getTimeLapsedInTextInputs(): Stream<Arguments> =
            Stream.of(
                Arguments.of("", 0),
                Arguments.of("", 7000),
                Arguments.of("", 56000),
                Arguments.of("1 min ago", 63000),
                Arguments.of("1 min ago", 119000),
                Arguments.of("2 mins ago", 126000),
                Arguments.of("2 mins ago", 175000),
                Arguments.of("3 mins ago", 182000),
                Arguments.of("3 mins ago", 238000),
                Arguments.of("4 mins ago", 245000),
                Arguments.of("4 mins ago", 294000)
            )
    }

    @ParameterizedTest
    @MethodSource("getConnectedDeviceRangeInputs")
    fun validConnectedDeviceRange(expectedStringValue: String, nearbyDevice: Int) {
        assertEquals(homeVM.getConnectedDeviceRange(nearbyDevice), expectedStringValue)
    }

    @ParameterizedTest
    @MethodSource("getTimeInMilliInputs")
    fun validTimeInMilli(expectedTime: Long, inputTime: Double) {
        assertEquals(homeVM.getTimeInMilli(inputTime), expectedTime)
    }

    @ParameterizedTest
    @MethodSource("displayAnnouncementAppVersionCheckInputs")
    fun displayAnnouncementVersionCheck(expectedResult: Boolean, currentAppVersion: String?, minAppVersion: String?, maxAppVersion: String?) {
        assertEquals(homeVM.displayAnnouncementAppVersionCheck(currentAppVersion, minAppVersion, maxAppVersion), expectedResult)
    }

    @ParameterizedTest
    @MethodSource("getTimeLapsedInTextInputs")
    fun validTimeLapsedInTexts(expectedString: String, inputTimeInMilli: Long) {
        assertEquals(homeVM.getTimeLapsedInText(inputTimeInMilli), expectedString)
    }
}