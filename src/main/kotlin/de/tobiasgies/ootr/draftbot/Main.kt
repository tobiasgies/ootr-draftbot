package de.tobiasgies.ootr.draftbot

import de.tobiasgies.ootr.draftbot.client.OotRandomizerClient
import de.tobiasgies.ootr.draftbot.drafts.Draft
import de.tobiasgies.ootr.draftbot.drafts.DraftFactory
import de.tobiasgies.ootr.draftbot.drafts.Season7QualifierDraft
import de.tobiasgies.ootr.draftbot.drafts.Season7TournamentDraft
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.choice
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.upsertCommand
import dev.minn.jda.ktx.jdabuilder.light
import io.github.cdimascio.dotenv.dotenv
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import okhttp3.OkHttpClient

fun main() {
    val dotenv = dotenv()
    setupMeterRegistry()

    val discordToken = dotenv["DISCORD_TOKEN"]!!
    val ootrToken = dotenv["OOTR_TOKEN"]!!

    val httpClient = buildHttpClient()
    val ootrClient = OotRandomizerClient(httpClient, ootrToken)

    val drafts = listOf(
        Season7QualifierDraft.Factory(ootrClient, ootrClient),
        Season7TournamentDraft.Factory(ootrClient, ootrClient),
    )

    val jda = light(token = discordToken, enableCoroutines = true)
    setupJda(jda, drafts)
}

private fun setupMeterRegistry() {
    val logger = KotlinLogging.logger {}
    if (Metrics.globalRegistry.registries.isEmpty()) {
        logger.warn {
            "No pre-configured meter registry found. The otel java agent is likely not attached. " +
                    "Attaching a SimpleMeterRegistry. No metrics will be exported by this process!"
        }
        Metrics.globalRegistry.add(SimpleMeterRegistry())
    }
}

private fun setupJda(
    jda: JDA,
    drafts: List<DraftFactory<out Draft>>
) {
    jda.upsertCommand("draft", "Start a tournament-style settings draft") {
        option<String>("type", "The draft type to simulate") {
            drafts.forEach { choice(it.friendlyName, it.identifier) }
        }
    }.queue()

    jda.onCommand(name = "draft") draftCommandHandler@{ event ->
        val type = event.getOption("type")!!.asString

        val draftFactory = drafts.find { it.identifier == type }
        if (draftFactory == null) {
            event.reply("Unknown draft type: $type").queue()
            return@draftCommandHandler
        }

        event.deferReply(true).queue()
        draftFactory.createDraft().start(slashCommand = event)
    }
}

private fun buildHttpClient() = OkHttpClient.Builder()
    .addInterceptor { chain ->
        chain.proceed(
            chain.request()
                .newBuilder()
                .header("User-Agent", "de.tobiasgies.ootr.draftbot/1.0.0")
                .build()
        )
    }.build()