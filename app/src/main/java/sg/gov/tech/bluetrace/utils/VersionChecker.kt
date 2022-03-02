package sg.gov.tech.bluetrace.utils

import java.lang.NumberFormatException

object VersionChecker {

    enum class Version {
        MAJOR,
        MINOR,
        PATCH
    }

    fun String.isSameVersionThan(comparedVersion: String): Boolean {
        if (comparedVersion.isEmpty())
            return false
        return try{
            val thisVersionParts = getCompleteVersionName(this).split(".").map { it.toInt() }
            val comparedVersionParts = getCompleteVersionName(comparedVersion).split(".").map { it.toInt() }
            when {
                thisVersionParts[Version.MAJOR.ordinal] == comparedVersionParts[Version.MAJOR.ordinal] &&
                        thisVersionParts[Version.MINOR.ordinal] == comparedVersionParts[Version.MINOR.ordinal] &&
                        thisVersionParts[Version.PATCH.ordinal] == comparedVersionParts[Version.PATCH.ordinal] -> true
                else -> false
            }
        } catch(e: NumberFormatException){
            false
        }
    }

    fun String.isAGreaterVersionThan(comparedVersion: String): Boolean {
        try{
            val thisVersionParts = getCompleteVersionName(this).split(".").map { it.toInt() }
            val comparedVersionParts = getCompleteVersionName(comparedVersion).split(".").map { it.toInt() }
            return when {

                thisVersionParts[Version.MAJOR.ordinal] > comparedVersionParts[Version.MAJOR.ordinal] -> true

                thisVersionParts[Version.MAJOR.ordinal] == comparedVersionParts[Version.MAJOR.ordinal] &&
                        thisVersionParts[Version.MINOR.ordinal] > comparedVersionParts[Version.MINOR.ordinal] -> true

                thisVersionParts[Version.MAJOR.ordinal] == comparedVersionParts[Version.MAJOR.ordinal] &&
                        thisVersionParts[Version.MINOR.ordinal] == comparedVersionParts[Version.MINOR.ordinal] &&
                        thisVersionParts[Version.PATCH.ordinal] > comparedVersionParts[Version.PATCH.ordinal] -> true
                else -> false
            }
        }
        catch (e: NumberFormatException){
            return false
        }
    }

    fun getCompleteVersionName(version: String): String {
        return when (version.length) {
            1 -> return "$version.0.0"
            3 -> return "$version.0"
            else -> version
        }

    }
    fun String.isVersionGreaterOrEqual(comparedVersion: String) : Boolean{
        return (this.isAGreaterVersionThan(comparedVersion) || this.isSameVersionThan(comparedVersion))
    }
}