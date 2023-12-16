package de.tobiasgies.ootr.draftbot.data

data class DraftPool(
    val major: Map<String, Draftable>,
    val minor: Map<String, Draftable>,
) {
    val combined by lazy { major + minor }

    fun randomMajorOption(): DraftableOption {
        val pick = major.entries.random().value
        return pick.options.entries.random().value
    }

    fun randomMinorOption(): DraftableOption {
        val pick = minor.entries.random().value
        return pick.options.entries.random().value
    }

    fun without(name: String) = DraftPool(
        major = major - name,
        minor = minor - name,
    )
}