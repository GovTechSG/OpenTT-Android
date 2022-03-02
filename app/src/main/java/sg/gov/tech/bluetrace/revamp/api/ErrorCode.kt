package sg.gov.tech.bluetrace.revamp.api

object ErrorCode {

    /**   */
    /**
     * The status codes that can be returned from a Callable HTTPS. These are the
     * canonical error codes for Google APIs, as documented here:
     * https://firebase.google.com/docs/reference/functions/providers_https_
     * and firebase converts Http codes to own codes, plz do refer FirebaseFunctionsException for mapping
     */

    val INVALID_PARAMETERS = 3
    val RESOURCE_EXHAUSTED = 8
}