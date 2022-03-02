package sg.gov.tech.bluetrace.revamp.requestModel

open class CheckInRequestModel(
    var id: String? = "",
    var venueId: String? = "",
    var tenantId: String? = "",
    var tenantName: String? = "",

    var postalCode: String? = ""
) : BaseRequestModel(){
    var actionType: String = "checkin"
}

class CheckInOutGroupIds(var id: String)

class CheckInRequestWithGroupMember(
    idCheckInGroup: String?,
    venueIdCheckInGroup: String?,
    tenantIdCheckInGroup: String?,
    tenantNameCheckInGroup: String?,
    postalCodeCheckInGroup: String?,
    var groupIds: List<CheckInOutGroupIds>
) : CheckInRequestModel(
    idCheckInGroup,
    venueIdCheckInGroup,
    tenantIdCheckInGroup,
    tenantNameCheckInGroup,
    postalCodeCheckInGroup
)
