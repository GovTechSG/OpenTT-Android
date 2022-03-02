package sg.gov.tech.safeentry.selfcheck.model

sealed class ConnectionState(val name: String) {
    object None : ConnectionState("none")
    object Loading : ConnectionState("loading")
    object Done : ConnectionState("done")
    object Error : ConnectionState("error")
    object NoNetwork : ConnectionState("noNetwork")

    val isLoading: Boolean get() = this == Loading
    val isNotLoading: Boolean get() = this != Loading
}