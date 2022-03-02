package sg.gov.tech.safeentry.selfcheck.model

data class HealthStatusApiData (
    val data: HealthStatus? = null,
    val error: Any? = null,
    val state: ConnectionState = ConnectionState.None
) {
    companion object {
        fun none() = HealthStatusApiData(state = ConnectionState.None)
        fun loading() = HealthStatusApiData(state = ConnectionState.Loading)
        fun noNetwork() = HealthStatusApiData(state = ConnectionState.NoNetwork)
        fun done(result: HealthStatus?) = HealthStatusApiData(data = result, state = ConnectionState.Done)
        fun error(cause: Throwable) = HealthStatusApiData(error = cause, state = ConnectionState.Error)
    }
}