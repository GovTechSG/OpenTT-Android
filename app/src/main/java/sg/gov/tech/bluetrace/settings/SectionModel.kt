package sg.gov.tech.bluetrace.settings

class SectionModel(
    val sectionLabel: String,
    val isTitle: Boolean = false,
    val isHeader: Boolean = false,
    val isFooter: Boolean = false,
    val titleList: Array<String>? = null
)