package sg.gov.tech.bluetrace.utils

sealed class VMState(val name: String) {
    object None : VMState("none")
    object Loading : VMState("loading")
    object Done : VMState("done")
    object Error : VMState("error")
    object NoNetwork : VMState("noNetwork")

    val isLoading: Boolean get() = this == Loading
    val isNotLoading: Boolean get() = this != Loading
}

data class State(
    val data: Any? = null,
    val error: Any? = null,
    val state: VMState = VMState.None
) {
    companion object {
        fun none() = State(state = VMState.None)
        fun loading() = State(state = VMState.Loading)
        fun noNetwork() = State(state = VMState.NoNetwork)
        fun <T : Any> done(result: T?) = State(data = result, state = VMState.Done)
        fun error(cause: Throwable) = State(error = cause, state = VMState.Error)
    }
}
