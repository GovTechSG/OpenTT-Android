package sg.gov.tech.safeentry.selfcheck.model

data class SEApiData(
        val data: SafeEntrySelfCheck? = null,
        val error: Any? = null,
        val state: ConnectionState = ConnectionState.None
) {
    companion object {
        fun none() = SEApiData(state = ConnectionState.None)
        fun loading() = SEApiData(state = ConnectionState.Loading)
        fun noNetwork() = SEApiData(state = ConnectionState.NoNetwork)
        fun <T : SafeEntrySelfCheck> done(result: T?) = SEApiData(data = result, state = ConnectionState.Done)
        fun error(cause: Throwable) = SEApiData(error = cause, state = ConnectionState.Error)
    }
}
