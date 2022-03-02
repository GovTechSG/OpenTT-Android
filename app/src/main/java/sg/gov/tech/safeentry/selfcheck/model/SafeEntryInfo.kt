package sg.gov.tech.safeentry.selfcheck.model

class SafeEntryInfo(
    val checkin: CheckInInfo,
    val location: MatchLocation,
    var checkout: CheckoutInfo?
) {
    fun isCheckout() = checkout != null
}

class CheckInInfo(
    val id: String,
    val time: Long,
    val type: String
)

class CheckoutInfo(
    val id: String,
    val time: Long,
    val type: String
)
