package sg.gov.tech.safeentry.selfcheck.model

data class SafeEntrySelfCheck(
        val count: Int,
        val data: List<SafeEntryMatch>
)
