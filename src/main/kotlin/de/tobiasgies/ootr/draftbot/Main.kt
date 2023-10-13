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
import io.micrometer.core.instrument.*
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.opentelemetry.api.GlobalOpenTelemetry
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import okhttp3.OkHttpClient

fun main() {
    val dotenv = dotenv()
    val meterRegistry = setupMeterRegistry()

    val discordToken = dotenv["DISCORD_TOKEN"]!!
    val ootrToken = dotenv["OOTR_TOKEN"]!!

    val httpClient = buildHttpClient(meterRegistry)
    val ootrClient = OotRandomizerClient(httpClient, ootrToken)

    val drafts = listOf(
        Season7QualifierDraft.Factory(ootrClient, ootrClient, meterRegistry),
        Season7TournamentDraft.Factory(ootrClient, ootrClient, meterRegistry),
    )

    val jda = light(token = discordToken, enableCoroutines = true)
    setupJda(jda, drafts, meterRegistry)
}

private fun setupMeterRegistry(): MeterRegistry {
    val logger = KotlinLogging.logger {}
    if (Metrics.globalRegistry.registries.isEmpty()) {
        logger.warn {
            "No pre-configured meter registry found. The otel java agent is likely not attached. " +
                    "Attaching a SimpleMeterRegistry. No metrics will be exported by this process!"
        }
        Metrics.globalRegistry.add(SimpleMeterRegistry())
    }
    return Metrics.globalRegistry
}

private fun setupJda(
    jda: JDA,
    drafts: List<DraftFactory<out Draft>>,
    meterRegistry: MeterRegistry = Metrics.globalRegistry
) {
    jda.upsertCommand("draft", "Start a tournament-style settings draft") {
        option<String>("type", "The draft type to simulate") {
            drafts.forEach { choice(it.friendlyName, it.identifier) }
        }
    }.queue()

    jda.onCommand(name = "draft") onDraftCommand@{ event ->
        val type = event.getOption("type")!!.asString
        val observabilityTags = if (event.channelType.isGuild) {
            mapOf(
                "draft_type" to type,
                "location" to "guild",
                "guild" to event.guildChannel.guild.id,
                "guild_name" to event.guildChannel.guild.name,
                "channel" to event.guildChannel.id,
                "channel_name" to event.guildChannel.name,
            )
        } else {
            mapOf(
                "draft_type" to type,
                "location" to "private_message",
            )
        }
        val span = GlobalOpenTelemetry.getTracer("ootr-draftbot")
            .spanBuilder("onDraftCommand")
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.SERVER)
            .also { builder -> observabilityTags.forEach { (k, v) -> builder.setAttribute(k, v) } }
            .startSpan()
        try {
            meterRegistry.counter(
                "draftbot.drafts.started",
                observabilityTags.map { (k, v) -> Tag.of(k, v) }
            ).increment()

            val draftFactory = drafts.find { it.identifier == type }
            if (draftFactory == null) {
                meterRegistry.counter("draftbot.drafts.failed", "reason", "unknown_type").increment()
                event.reply("Unknown draft type: $type").queue()
                return@onDraftCommand
            }

            event.deferReply(true).queue()
            draftFactory.createDraft().start(slashCommand = event)
        } finally {
            span.end()
        }
    }
}

private fun buildHttpClient(meterRegistry: MeterRegistry = Metrics.globalRegistry) = OkHttpClient.Builder()
    .eventListener(
        OkHttpMetricsEventListener.builder(meterRegistry, "okhttp.requests")
            .uriMapper { req -> req.url.newBuilder().query(null).fragment(null).build().toString() }
            .tags(Tags.of("scope", "ootr_client"))
            .build())
    .addInterceptor { chain ->
        chain.proceed(
            chain.request()
                .newBuilder()
                .header("User-Agent", "de.tobiasgies.ootr.draftbot/1.0.0")
                .build()
        )
    }.build()