package de.tobiasgies.ootr.draftbot.data

data class DraftPool(
    val major: Map<String, Draftable>,
    val minor: Map<String, Draftable>,
) {
    fun without(name: String) = DraftPool(
        major = major - name,
        minor = minor - name,
    )
}