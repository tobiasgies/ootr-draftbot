package de.tobiasgies.ootr.draftbot.drafts

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

interface Draft {
    val identifier: String
    val friendlyName: String
    suspend fun executeDraft(slashCommand: GenericCommandInteractionEvent)
}