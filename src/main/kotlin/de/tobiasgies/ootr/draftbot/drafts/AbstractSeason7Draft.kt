package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.client.SeedGenerator
import de.tobiasgies.ootr.draftbot.data.Preset
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

abstract class AbstractSeason7Draft(
    private val settingsPreset: Preset,
    private val seedGenerator: SeedGenerator,
) : Draft {
    protected abstract val draftState: DraftResult

    protected fun displayFinalDraft(previous: IDeferrableCallback) {
        if (!draftState.isComplete) {
            throw IllegalStateException("Cannot display draft result before the draft is complete")
        }
        val rollSeedButton = previous.jda.button(
            label = "Yes, I want to play these settings!",
            style = ButtonStyle.SUCCESS,
            user = previous.user
        ) { button ->
            button.deferEdit().queue()
            rollSeedAndDisplay(button)
        }
        val cancelDraftButton = previous.jda.button(
            label = "No, these are trash, discard them.",
            style = ButtonStyle.DANGER,
            user = previous.user
        ) { button ->
            button.deferEdit().queue()
            cancelDraftAndDisplay(button)
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Draft completed__**\n\n" +
                    "The following settings were drafted:\n\n" +
                    "${draftState.display()}\n" +
                    "Would you like me to roll you a seed with these settings?"
            components += row(rollSeedButton, cancelDraftButton)
        }).queue()
    }

    private suspend fun rollSeedAndDisplay(previous: ButtonInteractionEvent) {
        previous.hook.editOriginal(MessageEdit { content = "Rolling seed..." }).queue()
        previous.hook.editOriginalComponents(emptyList()).queue()

        val patchedSettings = settingsPreset.patchWithDraftResult(draftState, ::season7SpecialCaseHandler)
        try {
            val seed = seedGenerator.rollSeed(patchedSettings)
            previous.hook.deleteOriginal().queue()
            previous.hook.sendMessage("**__Seed generated__**\n\n" +
                    "${previous.user.asMention}, The following settings were drafted:\n\n" +
                    "${draftState.display()}\n" +
                    "You can find your seed here: ${seed.url}").queue()
        } catch (e: Exception) {
            Season7TournamentDraft.logger.error(e) { "There was a persistent error generating a seed. Falling back to sending user to OOTR.com." }
            previous.hook.editOriginal(MessageEdit {
                content = "**__Seed generation failed__**\n\n" +
                        "The following settings were drafted:\n\n" +
                        "${draftState.display()}\n" +
                        "Unfortunately, there was an error generating your seed. Please visit " +
                        "[OoTRandomizer.com](https://ootrandomizer.com) to try and generate a seed manually."
            }).queue()
        }
    }

    private fun cancelDraftAndDisplay(previous: ButtonInteractionEvent) {
        previous.hook.editOriginal(MessageEdit {
            content = "**__Draft cancelled__**\n\n" +
                    "This draft was cancelled. You can start a new one by using the `/draft` command again."
        }).queue()
        previous.hook.editOriginalComponents(emptyList()).queue()
    }
}