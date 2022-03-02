package sg.gov.tech.revamp.responseModel


data class TempIdModel(
    val shortTempIDs: List<TempID>,
    val tempIDs: List<TempID>,
    var modelMappingId: Long,
    var refreshTime: Long = 0,
    val status: String
) {
    data class TempID(val expiryTime: Int?, val startTime: Int?, val tempID: String?)

}

