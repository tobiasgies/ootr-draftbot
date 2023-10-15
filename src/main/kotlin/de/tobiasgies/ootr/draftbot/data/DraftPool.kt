package de.tobiasgies.ootr.draftbot.data

data class DraftPool(
    val major: Map<String, Draftable>,
    val minor: Map<String, Draftable>,
) {
    val combined by lazy { major + minor }

    fun without(name: String) = DraftPool(
        major = major - name,
        minor = minor - name,
    )
}