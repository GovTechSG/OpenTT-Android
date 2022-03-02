package sg.gov.tech.bluetrace.onboarding.newOnboard.register

interface LocalDataProvider<T> {
    fun provide(): T
}
