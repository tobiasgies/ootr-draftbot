package de.tobiasgies.ootr.draftbot.data

data class DraftableOption(
    val draftableName: String,
    val optionName: String,
    val settings: Map<String, Any>
)
