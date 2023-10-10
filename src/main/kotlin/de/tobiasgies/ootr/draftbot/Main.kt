package de.tobiasgies.ootr.draftbot

import de.tobiasgies.ootr.draftbot.client.LocalResourceDraftPoolLoader
import de.tobiasgies.ootr.draftbot.client.LocalResourcePresetsLoader
import de.tobiasgies.ootr.draftbot.drafts.Season7QualifierDraft
import de.tobiasgies.ootr.draftbot.drafts.Season7TournamentDraft
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.choice
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.upsertCommand
import dev.minn.jda.ktx.jdabuilder.light
import io.github.cdimascio.dotenv.dotenv

fun main() {
    val dotenv = dotenv()

    val discordToken = dotenv["DISCORD_TOKEN"]!!

    val presets = LocalResourcePresetsLoader().loadPresets()
    val draftPool = LocalResourceDraftPoolLoader().loadDraftPool()

    val drafts = listOf(
        Season7QualifierDraft(draftPool),
        Season7TournamentDraft(draftPool),
    )

    val jda = light(token = discordToken, enableCoroutines = true)

    jda.upsertCommand("draft", "Start a tournament-style settings draft") {
        option<String>("type", "The draft type to simulate") {
            drafts.forEach { choice(it.friendlyName, it.identifier) }
        }
    }.queue()

    jda.onCommand(name = "draft") { event ->
        val type = event.getOption("type")!!.asString

        val draft = drafts.find { it.identifier == type }
        if (draft == null) {
            event.reply("Unknown draft type: $type").queue()
            return@onCommand
        }

        event.deferReply(true).queue()
        draft.executeDraft(event)
    }
}