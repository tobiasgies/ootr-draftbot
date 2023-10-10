package de.tobiasgies.ootr.draftbot

import de.tobiasgies.ootr.draftbot.client.OotRandomizerClient
import de.tobiasgies.ootr.draftbot.drafts.Season7QualifierDraft
import de.tobiasgies.ootr.draftbot.drafts.Season7TournamentDraft
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.choice
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.upsertCommand
import dev.minn.jda.ktx.jdabuilder.light
import io.github.cdimascio.dotenv.dotenv
import okhttp3.OkHttpClient

fun main() {
    val dotenv = dotenv()

    val discordToken = dotenv["DISCORD_TOKEN"]!!
    val ootrToken = dotenv["OOTR_TOKEN"]!!

    val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request()
                    .newBuilder()
                    .header("User-Agent", "de.tobiasgies.ootr.draftbot/1.0.0")
                    .build()
            )
        }.build()
    val configSource = OotRandomizerClient(httpClient, ootrToken)

    val drafts = listOf(
        Season7QualifierDraft.Factory(configSource),
        Season7TournamentDraft.Factory(configSource),
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
        draft.createDraft().start(slashCommand = event)
    }
}