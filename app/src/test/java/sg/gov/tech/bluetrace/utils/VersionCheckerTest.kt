package sg.gov.tech.bluetrace.utils


import org.junit.Assert.assertEquals
import org.junit.Test
import sg.gov.tech.bluetrace.utils.VersionChecker.isAGreaterVersionThan
import sg.gov.tech.bluetrace.utils.VersionChecker.isSameVersionThan
import sg.gov.tech.bluetrace.utils.VersionChecker.isVersionGreaterOrEqual

class VersionCheckerTest {
    @Test
    fun isVersionBiggerOrEqual_versionLengthEqual_BiggerTrue() {
        val biggerVersion = "2.3.9"
        val smallerVersion = "2.3.8"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),true)
    }

    @Test
    fun isVersionBiggerOrEqual_versionLengthEqual_BiggerFalse() {
        val biggerVersion = "2.3.7"
        val smallerVersion = "2.3.8"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),false)
    }

    @Test
    fun isVersionBiggerOrEqual_versionLengthEqual_VersionsEqual() {
        val biggerVersion = "2.3.8"
        val smallerVersion = "2.3.8"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),true)
    }

    @Test
    fun isVersionBiggerOrEqual_biggerVersionLengthSmall_BiggerTrue() {
        val biggerVersion = "2.4"
        val smallerVersion = "2.3.8"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),true)
    }

    @Test
    fun isVersionBiggerOrEqual_biggerVersionLengthSmall_BiggerFalse() {
        val biggerVersion = "2.3"
        val smallerVersion = "2.3.8"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),false)
    }


    @Test
    fun isVersionBiggerOrEqual_biggerVersionLengthSmall_VersionEqual() {
        val biggerVersion = "2.3.0"
        val smallerVersion = "2.3.0"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),true)
    }

    @Test
    fun isVersionBiggerOrEqual_SmallerVersionLengthSmall_BiggerTrue() {
        val biggerVersion = "2.4.9"
        val smallerVersion = "2.4"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),true)
    }

    @Test
    fun isVersionBiggerOrEqual_SmallerVersionLengthSmallBiggerFalse() {
        val biggerVersion = "2.2.8"
        val smallerVersion = "2.3"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),false)
    }
    @Test
    fun isVersionBiggerOrEqual_SmallerVersionLengthSmall_VersionEqual() {
        val biggerVersion = "2.3.0"
        val smallerVersion = "2.3"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),true)
    }

    @Test
    fun isVersionBiggerOrEqual_VersionNameHasACharacter(){
        val biggerVersion = "2.3a.0"
        val smallerVersion = "2.3"
        assertEquals(biggerVersion.isVersionGreaterOrEqual(smallerVersion),false)
    }

    @Test
    fun isSameVersion_VersionEqual(){
        val version1 = "2.3.0"
        val version2 = "2.3.0"
        assertEquals(version1.isSameVersionThan(version2),true)
    }

    @Test
    fun isSameVersion_DifferentVersionLength_VersionEqual(){
        val version1 = "2.3"
        val version2 = "2.3.0"
        assertEquals(version1.isSameVersionThan(version2),true)
    }

    @Test
    fun isSameVersion_DifferentVersionLength_VersionNotEqual(){
        val version1 = "2"
        val version2 = "2.3.0"
        assertEquals(version1.isSameVersionThan(version2),false)
    }
    @Test
    fun isSameVersion_VersionNotEqual(){
        val version1 = "2.3.5"
        val version2 = "2.3.0"
        assertEquals(version1.isSameVersionThan(version2),false)
    }
    @Test
    fun isAGreaterVersionThan_VersionGreaterTrue(){
        val version1 = "2.3.5"
        val version2 = "2.3.0"
        assertEquals(version1.isAGreaterVersionThan(version2),true)
    }
    @Test
    fun isAGreaterVersionThan_VersionLengthNotEqual_VersionGreaterTrue(){
        val version1 = "2.3.5"
        val version2 = "2.3"
        assertEquals(version1.isAGreaterVersionThan(version2),true)
    }
    @Test
    fun isAGreaterVersionThan_VersionGreaterFalse(){
        val version1 = "2.3.0"
        val version2 = "2.3.1"
        assertEquals(version1.isAGreaterVersionThan(version2),false)
    }
    @Test
    fun isAGreaterVersionThan_VersionLengthNotEqual_VersionGreaterFalse(){
        val version1 = "2"
        val version2 = "2.3.7"
        assertEquals(version1.isAGreaterVersionThan(version2),false)
    }

    @Test
    fun getCompleteVersionName_VersionLength1(){
        val version = "2"
        assertEquals(VersionChecker.getCompleteVersionName(version),"2.0.0")
    }

    @Test
    fun getCompleteVersionName_VersionLength2(){
        val version = "2.3"
        assertEquals(VersionChecker.getCompleteVersionName(version),"2.3.0")
    }

    @Test
    fun getCompleteVersionName_VersionLength3(){
        val version = "2.3.0"
        assertEquals(VersionChecker.getCompleteVersionName(version),"2.3.0")
    }

    @Test
    fun getCompleteVersionName_VersionLength3NoZero(){
        val version = "2.3.3"
        assertEquals(VersionChecker.getCompleteVersionName(version),"2.3.3")
    }
}