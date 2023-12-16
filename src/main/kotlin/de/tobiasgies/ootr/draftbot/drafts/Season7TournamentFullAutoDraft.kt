package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.client.ConfigSource
import de.tobiasgies.ootr.draftbot.client.SeedGenerator
import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.Preset
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.opentelemetry.instrumentation.annotations.WithSpan
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

class Season7TournamentFullAutoDraft(
    draftPool: DraftPool,
    settingsPreset: Preset,
    seedGenerator: SeedGenerator,
    meterRegistry: MeterRegistry = Metrics.globalRegistry
) : AbstractSeason7Draft(settingsPreset, seedGenerator, Season7TournamentFullAutoDraft::class, meterRegistry) {
    override val draftState = Season7TournamentFullAutoDraftState.randomize(draftPool)

    @WithSpan
    override suspend fun start(slashCommand: GenericCommandInteractionEvent) {
        displayFinalDraft(slashCommand)
    }

    class Factory(
        private val configSource: ConfigSource,
        private val seedGenerator: SeedGenerator,
        private val meterRegistry: MeterRegistry = Metrics.globalRegistry
    ) : DraftFactory<Season7TournamentFullAutoDraft> {
        override val identifier = Season7TournamentFullAutoDraft::class.simpleName!!
        override val friendlyName = "Season 7 Tournament, 1v1 full-auto (bot picks all settings - 2 major, 2 minor)"
        override fun createDraft(): Season7TournamentFullAutoDraft {
            return Season7TournamentFullAutoDraft(
                configSource.draftPool,
                configSource.presets["S7 Tournament"]!!,
                seedGenerator,
                meterRegistry
            )
        }
    }
}