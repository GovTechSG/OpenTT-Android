package sg.gov.tech.safeentry.selfcheck.model

data class VaccinationInfo(
        val isVaccinated: Boolean,
        val iconText: String,
        val header: String,
        val subtext: String,
        val urlText: String,
        val urlLink: String
)