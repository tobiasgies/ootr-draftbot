package de.tobiasgies.ootr.draftbot.drafts

interface DraftFactory<T : Draft> {
    val identifier: String
    val friendlyName: String
    fun createDraft(): T
}