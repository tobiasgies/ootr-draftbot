package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.data.DraftableOption

interface DraftResult {
    val isComplete: Boolean
    val selectedSettings: Map<String, DraftableOption>
}