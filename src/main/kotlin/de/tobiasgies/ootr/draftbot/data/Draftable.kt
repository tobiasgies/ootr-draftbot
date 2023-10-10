package de.tobiasgies.ootr.draftbot.data

data class Draftable(
    val name: String,
    val options: Map<String, DraftableOption>,
)
