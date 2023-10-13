package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.client.SeedGenerator
import de.tobiasgies.ootr.draftbot.data.Preset
import de.tobiasgies.ootr.draftbot.util.withOtelContext
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageEdit
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KLogging
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import kotlin.reflect.KClass

abstract class AbstractSeason7Draft(
    private val settingsPreset: Preset,
    private val seedGenerator: SeedGenerator,
    private val draftType: KClass<out AbstractSeason7Draft>,
    protected val meterRegistry: MeterRegistry = Metrics.globalRegistry
) : Draft {
    protected abstract val draftState: DraftResult

    @WithSpan
    protected suspend fun displayFinalDraft(previous: IDeferrableCallback) {
        val otelContext = Context.current()
        if (!draftState.isComplete) {
            val e = IllegalStateException("Cannot display draft result before the draft is complete")
            meterRegistry.countDraftException(e)
            throw e
        }
        val rollSeedButton = previous.jda.button(
            label = "Yes, I want to play these settings!",
            style = ButtonStyle.SUCCESS,
            user = previous.user
        ) { button ->
            withOtelContext(otelContext) {
                button.deferEdit().queue()
                rollSeedAndDisplay(button)
            }
        }
        val cancelDraftButton = previous.jda.button(
            label = "No, these are trash, discard them.",
            style = ButtonStyle.DANGER,
            user = previous.user
        ) { button ->
            withOtelContext(otelContext) {
                button.deferEdit().queue()
                cancelDraftAndDisplay(button)
            }
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Draft completed__**\n\n" +
                    "The following settings were drafted:\n\n" +
                    "${draftState.display()}\n" +
                    "Would you like me to roll you a seed with these settings?"
            components += row(rollSeedButton, cancelDraftButton)
        }).queue()
    }

    @WithSpan
    private suspend fun rollSeedAndDisplay(previous: ButtonInteractionEvent) {
        meterRegistry.countSeedRequested()
        previous.hook.editOriginal(MessageEdit { content = "Rolling seed..." }).queue()
        previous.hook.editOriginalComponents(emptyList()).queue()

        val patchedSettings = settingsPreset.patchWithDraftResult(draftState, ::season7SpecialCaseHandler)
        try {
            val seed = seedGenerator.rollSeed(patchedSettings)
            meterRegistry.countSeedRolled()
            previous.hook.deleteOriginal().queue()
            previous.hook.sendMessage("**__Seed generated__**\n\n" +
                    "${previous.user.asMention}, the following settings were drafted:\n\n" +
                    "${draftState.display()}\n" +
                    "You can find your seed here: ${seed.url}").queue()
        } catch (e: Exception) {
            logger.error(e) { "There was a persistent error generating a seed. Falling back to sending user to OOTR.com." }
            meterRegistry.countDraftException(e)
            previous.hook.editOriginal(MessageEdit {
                content = "**__Seed generation failed__**\n\n" +
                        "The following settings were drafted:\n\n" +
                        "${draftState.display()}\n" +
                        "Unfortunately, there was an error generating your seed. Please visit " +
                        "[OoTRandomizer.com](https://ootrandomizer.com) to try and generate a seed manually."
            }).queue()
        }
    }

    @WithSpan
    private fun cancelDraftAndDisplay(previous: ButtonInteractionEvent) {
        meterRegistry.countAbortedDraft()
        previous.hook.editOriginal(MessageEdit {
            content = "**__Draft cancelled__**\n\n" +
                    "This draft was cancelled. You can start a new one by using the `/draft` command again."
        }).queue()
        previous.hook.editOriginalComponents(emptyList()).queue()
    }

    private fun MeterRegistry.countSeedRequested() {
        counter("draftbot.drafts.seed_requested", "draft_type", draftType.simpleName).increment()
    }

    private fun MeterRegistry.countSeedRolled() {
        counter("draftbot.drafts.seed_rolled", "draft_type", draftType.simpleName).increment()
    }

    private fun MeterRegistry.countAbortedDraft() {
        counter("draftbot.drafts.aborted", "draft_type", draftType.simpleName).increment()
    }

    private fun MeterRegistry.countDraftException(exception: Exception) {
        counter(
            "draftbot.drafts.failed",
            "draft_type",
            draftType.simpleName,
            "reason",
            "exception",
            "exception_type",
            exception::class.simpleName
        ).increment()
    }

    companion object : KLogging()
}