package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.DraftState
import de.tobiasgies.ootr.draftbot.data.DraftStep
import dev.minn.jda.ktx.events.onStringSelect
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import java.lang.Math.random
import java.util.*

// TODO: This is not multi-user capable yet and will break. Needs another layer of indirection (e.g. DraftFactory).
class Season7TournamentDraft(initialDraftPool: DraftPool) : Draft {
    override val identifier = "s7_1v1"
    override val friendlyName = "Season 7 Tournament, 1 vs 1 draft (2 bans, 2 major, 2 minor)"
    private val draftState = DraftState(initialDraftPool)

    override suspend fun executeDraft(slashCommand: GenericCommandInteractionEvent) {
        val banFirstButton = slashCommand.jda.button(label = "I ban first", user = slashCommand.user) { button ->
            button.deferEdit().queue()
            draftState.userBansFirst()
            displayBanMajorMinor(button)
        }
        val banSecondButton = slashCommand.jda.button(label = "Bot bans first", user = slashCommand.user) { button ->
            button.deferEdit().queue()
            draftState.botBansFirst()
            executeBotBan()
            displayBanMajorMinor(button)
        }
        slashCommand.hook.editOriginal(MessageEdit {
            content = "**__Step 1: Picking order__**\n\n" +
                    "Please decide who gets to ban a setting first."
            components += row(banFirstButton, banSecondButton)
        }).queue()
    }

    private fun executeBotBan() {
        if (random() < BAN_MINOR_CHANCE) {
            draftState.banSetting(draftState.draftPool.minor.keys.random())
        } else {
            draftState.banSetting(draftState.draftPool.major.keys.random())
        }
    }

    private fun displayBanMajorMinor(previous: ButtonInteractionEvent) {
        val banMajorButton = previous.jda.button(label = "Major setting", user = previous.user) { button ->
            button.deferEdit().queue()
            displayBanSelection(button, "major", draftState.draftPool.major.keys)
        }
        val banMinorButton = previous.jda.button(label = "Minor setting", user = previous.user) { button ->
            button.deferEdit().queue()
            displayBanSelection(button, "minor", draftState.draftPool.minor.keys)
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Step 2: Bans__**\n\n" +
                    "**Current draft state:**\n${draftState.display()}\n" +
                    "**Bannable settings:**\n" +
                    "* **Major settings:** ${draftState.draftPool.major.keys.joinToString(", ")}\n" +
                    "* **Minor settings:** ${draftState.draftPool.minor.keys.joinToString(", ")}\n\n" +
                    "Would you like to ban a major or minor setting?"
            components += row(banMajorButton, banMinorButton)
        }).queue()
    }

    private fun displayBanSelection(previous: ButtonInteractionEvent, type: String, bannableSettings: Set<String>) {
        val selectUuid = UUID.randomUUID()
        previous.jda.onStringSelect("ban_setting_$selectUuid") { select ->
            select.deferEdit().queue()
            draftState.banSetting(select.values.first())
            if (draftState.currentStep == DraftStep.BAN) {
                // The bot hasn't banned yet.
                executeBotBan()
                executeBotPickMajor()
            }
            displayMajorPickSelection(select)
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Step 2: Bans__**\n\n" +
                    "**Current draft state:**\n${draftState.display()}\n" +
                    "Banning a $type setting."
            components += row(StringSelectMenu("ban_setting_$selectUuid", "Select a setting to ban") {
                bannableSettings.forEach { option(it.capitalize(), it) }
            })
        }).queue()
    }

    private fun executeBotPickMajor() {
        val draftable = draftState.draftPool.major.entries.random()
        val draftableOption = draftable.value.options.keys.random()

        draftState.pickMajor(draftable.key, draftableOption)
    }

    private fun executeBotPickMinor() {
        val draftable = draftState.draftPool.minor.entries.random()
        val draftableOption = draftable.value.options.keys.random()

        draftState.pickMinor(draftable.key, draftableOption)
    }

    private fun displayMajorPickSelection(previous: StringSelectInteractionEvent) {
        val selectUuid = UUID.randomUUID()
        previous.jda.onStringSelect("pick_major_setting_$selectUuid") { select ->
            select.deferEdit().queue()
            val (draftable, optionName) = select.values.first().split('=')
            draftState.pickMajor(draftable, optionName)
            if (draftState.currentStep == DraftStep.PICK_MAJOR) {
                // The bot hasn't picked yet.
                executeBotPickMajor()
                executeBotPickMinor()
            }
            displayMinorPickSelection(select)
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Step 3: Major pick__**\n\n" +
                    "**Current draft state:**\n${draftState.display()}\n" +
                    "You must pick a major setting to modify."
            components += row(StringSelectMenu("pick_major_setting_$selectUuid", "Pick a setting") {
                draftState.draftPool.major.forEach { draftable ->
                    draftable.value.options.forEach {
                        option("${draftable.key.capitalize()}: ${it.key.capitalize()}", "${draftable.key}=${it.key}")
                    }
                }
            })
        }).queue()
    }

    private fun displayMinorPickSelection(previous: StringSelectInteractionEvent) {
        val selectUuid = UUID.randomUUID()
        previous.jda.onStringSelect("pick_minor_setting_$selectUuid") { select ->
            select.deferEdit().queue()
            val (draftable, optionName) = select.values.first().split('=')
            draftState.pickMinor(draftable, optionName)
            if (draftState.currentStep == DraftStep.PICK_MINOR) {
                // The bot hasn't picked yet.
                executeBotPickMinor()
            }
            displayFinalDraft(select)
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Step 4: Minor pick__**\n\n" +
                    "**Current draft state:**\n${draftState.display()}\n" +
                    "You must pick a minor setting to modify."
            components += row(StringSelectMenu("pick_minor_setting_$selectUuid", "Pick a setting") {
                draftState.draftPool.minor.forEach { draftable ->
                    draftable.value.options.forEach {
                        option("${draftable.key.capitalize()}: ${it.key.capitalize()}", "${draftable.key}=${it.key}")
                    }
                }
            })
        }).queue()
    }

    private fun displayFinalDraft(previous: StringSelectInteractionEvent) {
        if (draftState.currentStep != DraftStep.DONE) {
            throw IllegalStateException("Cannot display final draft before the draft is done")
        }
        previous.hook.deleteOriginal().queue()
        previous.hook.sendMessage("**__Draft completed!__**\n\n" +
                "${previous.user.asMention}, we drafted the following settings together:\n\n" +
                "${draftState.display()}\n" +
                "Go to the [OOTRandomizer website](https://www.ootrandomizer.com/generatorDev), select the " +
                "`S7 Tournament` preset, and change the settings as indicated above to roll your seed.").queue()
    }

    companion object {
        private val BAN_MINOR_CHANCE = 0.2
    }
}