package de.tobiasgies.ootr.draftbot.drafts

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

interface Draft {
    suspend fun start(slashCommand: GenericCommandInteractionEvent)
}