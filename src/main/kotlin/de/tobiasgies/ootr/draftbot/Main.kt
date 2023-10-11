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
import io.sentry.Sentry
import mu.KLogger
import mu.KotlinLogging
import okhttp3.OkHttpClient

fun main() {
    val dotenv = dotenv()

    // order is important: Do this as soon as the app is initialized, to ensure Sentry captures all errors.
    Sentry.init { options ->
        options.dsn = dotenv["SENTRY_DSN"]
        // Set tracesSampleRate to 1.0 to capture 100% of transactions for performance monitoring.
        // We recommend adjusting this value in production.
        options.tracesSampleRate = 1.0
    }

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
    val ootrClient = OotRandomizerClient(httpClient, ootrToken)

    val drafts = listOf(
        Season7QualifierDraft.Factory(ootrClient, ootrClient),
        Season7TournamentDraft.Factory(ootrClient, ootrClient),
    )

    val jda = light(token = discordToken, enableCoroutines = true)

    jda.upsertCommand("draft", "Start a tournament-style settings draft") {
        option<String>("type", "The draft type to simulate") {
            drafts.forEach { choice(it.friendlyName, it.identifier) }
        }
    }.queue()

    jda.onCommand(name = "draft") { event ->
        val type = event.getOption("type")!!.asString

        val draftFactory = drafts.find { it.identifier == type }
        if (draftFactory == null) {
            event.reply("Unknown draft type: $type").queue()
            return@onCommand
        }

        event.deferReply(true).queue()
        draftFactory.createDraft().start(slashCommand = event)
    }
}