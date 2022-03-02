package sg.gov.tech.bluetrace.revamp.requestModel

open class CheckOutRequestModel(
    var venueId: String? = "",
    var tenantId: String? = "",
    var tenantName: String? = "",
    var id: String? = ""
) : BaseRequestModel() {
    var actionType: String = "checkout"
    class CheckInOutGroupIds(var id: String)

    class CheckOutRequestWithGroupMember(
        venueIdCheckOutGroup: String?,
        tenantIdCheckOutGroup: String?,
        tenantNameCheckOutGroup: String?,
        idCheckOutGroup: String?,
        var groupIds: List<CheckInOutGroupIds>
    ) : CheckOutRequestModel(
        venueIdCheckOutGroup,
        tenantIdCheckOutGroup,
        tenantNameCheckOutGroup,
        idCheckOutGroup
    )
}
