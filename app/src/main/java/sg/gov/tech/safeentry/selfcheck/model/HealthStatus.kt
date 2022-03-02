package sg.gov.tech.safeentry.selfcheck.model

data class HealthStatus(
        val status: String,
        val selfCheck: SafeEntrySelfCheck,
        val vaccination: VaccinationInfo
)