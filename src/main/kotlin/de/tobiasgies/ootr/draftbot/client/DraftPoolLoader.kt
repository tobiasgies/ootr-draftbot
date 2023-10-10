package de.tobiasgies.ootr.draftbot.client

import de.tobiasgies.ootr.draftbot.data.DraftPool

interface DraftPoolLoader {
    fun loadDraftPool(): DraftPool
}