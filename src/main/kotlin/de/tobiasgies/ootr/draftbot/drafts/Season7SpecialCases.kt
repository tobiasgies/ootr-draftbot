package de.tobiasgies.ootr.draftbot.drafts

fun season7SpecialCaseHandler(draftResult: DraftResult, settings: Map<String, Any>): Map<String, Any> {
    val patchedSettings = settings.toMutableMap()

    if (draftResult.selectedSettings.containsKey("ow_tokens")
        && draftResult.selectedSettings.containsKey("dungeon_tokens")) {
        patchedSettings["tokensanity"] = "all"
    }

    if (settings["shuffle_dungeon_entrances"] == "simple") {
        val allowedTricks = settings["allowed_tricks"] as? List<*> ?: listOf<String>()
        patchedSettings["allowed_tricks"] = allowedTricks + "logic_dc_scarecrow_gs"
    }

    return patchedSettings.toMap()
}