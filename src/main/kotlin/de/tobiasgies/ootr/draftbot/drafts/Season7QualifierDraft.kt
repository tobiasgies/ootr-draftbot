package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.client.ConfigSource
import de.tobiasgies.ootr.draftbot.client.SeedGenerator
import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.Preset
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

class Season7QualifierDraft(
    draftPool: DraftPool,
    settingsPreset: Preset,
    seedGenerator: SeedGenerator
) : AbstractSeason7Draft(settingsPreset, seedGenerator) {
    override val draftState = Season7QualifierDraftState.randomize(draftPool)

    @WithSpan(kind = SpanKind.SERVER)
    override suspend fun start(slashCommand: GenericCommandInteractionEvent) {
        displayFinalDraft(slashCommand)
    }

    class Factory(
        private val configSource: ConfigSource,
        private val seedGenerator: SeedGenerator
    ) : DraftFactory<Season7QualifierDraft> {
        override val identifier = "s7_qual"
        override val friendlyName = "Season 7 Tournament, Qualifier Draft (1 major, 1 minor)"
        override fun createDraft(): Season7QualifierDraft {
            return Season7QualifierDraft(configSource.draftPool, configSource.presets["S7 Tournament"]!!, seedGenerator)
        }
    }
}