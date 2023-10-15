package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.client.ConfigSource
import de.tobiasgies.ootr.draftbot.client.SeedGenerator
import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.Draftable
import de.tobiasgies.ootr.draftbot.data.Preset
import de.tobiasgies.ootr.draftbot.drafts.Season7FriendlyNames.friendlyName
import de.tobiasgies.ootr.draftbot.drafts.Season7TournamentDraftState.Step
import de.tobiasgies.ootr.draftbot.util.withOtelContext
import dev.minn.jda.ktx.events.onStringSelect
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.jdabuilder.scope
import dev.minn.jda.ktx.messages.MessageEdit
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import java.lang.Math.random
import java.util.*
import kotlin.time.Duration

class Season7TournamentDraft(
    initialDraftPool: DraftPool,
    settingsPreset: Preset,
    seedGenerator: SeedGenerator,
    meterRegistry: MeterRegistry = Metrics.globalRegistry
) : AbstractSeason7Draft(settingsPreset, seedGenerator, Season7TournamentDraft::class, meterRegistry) {
    override val draftState = Season7TournamentDraftState(initialDraftPool)

    @WithSpan
    override suspend fun start(slashCommand: GenericCommandInteractionEvent) {
        val otelContext = Context.current()
        val banFirstButton = slashCommand.jda.button(label = "I ban first", user = slashCommand.user) { button ->
            withOtelContext(otelContext) {
                button.deferEdit().queue()
                draftState.userBansFirst()
                meterRegistry.countPickingOrder("user")
                displayBanMajorMinor(button)
            }
        }
        val banSecondButton = slashCommand.jda.button(label = "Bot bans first", user = slashCommand.user) { button ->
            withOtelContext(otelContext) {
                button.deferEdit().queue()
                draftState.botBansFirst()
                meterRegistry.countPickingOrder("bot")
                executeBotBan()
                displayBanMajorMinor(button)
            }
        }
        slashCommand.hook.editOriginal(MessageEdit {
            content = "**__Step 1: Picking order__**\n\n" +
                    "Please decide who gets to ban a setting first."
            components += row(banFirstButton, banSecondButton)
        }).queue()
    }

    @WithSpan
    private suspend fun displayBanMajorMinor(previous: ButtonInteractionEvent) {
        val otelContext = Context.current()
        val banMajorButton = previous.jda.button(label = "Major setting", user = previous.user) { button ->
            withOtelContext(otelContext) {
                button.deferEdit().queue()
                displayBanSelection(button, "major", draftState.draftPool.major.keys)
            }
        }
        val banMinorButton = previous.jda.button(label = "Minor setting", user = previous.user) { button ->
            withOtelContext(otelContext) {
                button.deferEdit().queue()
                displayBanSelection(button, "minor", draftState.draftPool.minor.keys)
            }
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Step 2: Bans__**\n\n" +
                    "**Current draft state:**\n${draftState.display()}\n" +
                    "**Bannable settings:**\n" +
                    "* **Major settings:** ${draftState.draftPool.major.values.joinToString(", ") { it.friendlyName }}\n" +
                    "* **Minor settings:** ${draftState.draftPool.minor.values.joinToString(", ") { it.friendlyName }}\n\n" +
                    "Would you like to ban a major or minor setting?"
            components += row(banMajorButton, banMinorButton)
        }).queue()
    }

    @WithSpan
    private suspend fun displayBanSelection(previous: ButtonInteractionEvent, type: String, bannableSettings: Set<String>) {
        val otelContext = Context.current()
        val selectUuid = UUID.randomUUID()
        val listener = previous.jda.onStringSelect("ban_setting_$selectUuid") { select ->
            withOtelContext(otelContext) {
                select.deferEdit().queue()
                draftState.banSetting(select.values.first())
                meterRegistry.countBan(select.values.first(), type, "user")
                if (draftState.currentStep == Step.BAN) {
                    // User banned first, the bot hasn't banned yet.
                    executeBotBan()
                } else if (draftState.currentStep == Step.PICK_MAJOR) {
                    // User banned second, so the bot needs to pick first.
                    executeBotPickMajor()
                }
                displayMajorPickSelection(select)
            }
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Step 2: Bans__**\n\n" +
                    "**Current draft state:**\n${draftState.display()}\n" +
                    "Banning a $type setting."
            components += row(StringSelectMenu("ban_setting_$selectUuid", "Select a setting to ban") {
                bannableSettings.forEach { option(it.capitalize(), it) }
            })
        }).queue()
        previous.jda.removeEventListenerLater(listener)
    }

    @WithSpan
    private suspend fun displayMajorPickSelection(previous: StringSelectInteractionEvent) {
        val otelContext = Context.current()
        val selectUuid = UUID.randomUUID()
        val listener = previous.jda.onStringSelect("pick_major_setting_$selectUuid") { select ->
            withOtelContext(otelContext) {
                select.deferEdit().queue()
                select.jda.removeEventListener(this)
                val (draftable, optionName) = select.values.first().split('=')
                draftState.pickMajor(draftable, optionName)
                meterRegistry.countPick(select.values.first(), "major", "user")
                if (draftState.currentStep == Step.PICK_MAJOR) {
                    // The bot hasn't picked yet.
                    executeBotPickMajor()
                    executeBotPickMinor()
                }
                displayMinorPickSelection(select)
            }
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Step 3: Major pick__**\n\n" +
                    "**Current draft state:**\n${draftState.display()}\n" +
                    "You must pick a major setting to modify."
            components += row(StringSelectMenu("pick_major_setting_$selectUuid", "Pick a setting") {
                draftState.draftPool.major.forEach { draftable ->
                    draftable.value.options.forEach {
                        option(
                            it.value.friendlyName,
                            draftChoiceValue(draftable.value, it.key)
                        )
                    }
                }
            })
        }).queue()
        previous.jda.removeEventListenerLater(listener)
    }

    @WithSpan
    private suspend fun displayMinorPickSelection(previous: StringSelectInteractionEvent) {
        val otelContext = Context.current()
        val selectUuid = UUID.randomUUID()
        val listener = previous.jda.onStringSelect("pick_minor_setting_$selectUuid") { select ->
            withOtelContext(otelContext) {
                select.deferEdit().queue()
                select.jda.removeEventListener(this)
                val (draftable, optionName) = select.values.first().split('=')
                draftState.pickMinor(draftable, optionName)
                meterRegistry.countPick(select.values.first(), "minor", "user")
                if (draftState.currentStep == Step.PICK_MINOR) {
                    // The bot hasn't picked yet.
                    executeBotPickMinor()
                }
                displayFinalDraft(select)
            }
        }
        previous.hook.editOriginal(MessageEdit {
            content = "**__Step 4: Minor pick__**\n\n" +
                    "**Current draft state:**\n${draftState.display()}\n" +
                    "You must pick a minor setting to modify."
            components += row(StringSelectMenu("pick_minor_setting_$selectUuid", "Pick a setting") {
                draftState.draftPool.minor.forEach { draftable ->
                    draftable.value.options.forEach {
                        option(
                            it.value.friendlyName,
                            draftChoiceValue(draftable.value, it.key)
                        )
                    }
                }
            })
        }).queue()
        previous.jda.removeEventListenerLater(listener)
    }

    @WithSpan
    private fun executeBotBan() {
        if (random() < BAN_MINOR_CHANCE) {
            val setting = draftState.draftPool.minor.keys.random()
            draftState.banSetting(setting)
            meterRegistry.countBan(setting, "minor", "bot")
        } else {
            val setting = draftState.draftPool.major.keys.random()
            draftState.banSetting(setting)
            meterRegistry.countBan(setting, "major", "bot")
        }
    }

    @WithSpan
    private fun executeBotPickMajor() {
        val draftable = draftState.draftPool.major.entries.random()
        val draftableOption = draftable.value.options.keys.random()

        draftState.pickMajor(draftable.key, draftableOption)
        meterRegistry.countPick(draftChoiceValue(draftable.value, draftableOption), "major","bot")
    }

    @WithSpan
    private fun executeBotPickMinor() {
        val draftable = draftState.draftPool.minor.entries.random()
        val draftableOption = draftable.value.options.keys.random()

        draftState.pickMinor(draftable.key, draftableOption)
        meterRegistry.countPick(draftChoiceValue(draftable.value, draftableOption), "minor","bot")
    }

    class Factory(
        private val configSource: ConfigSource,
        private val seedGenerator: SeedGenerator,
        private val meterRegistry: MeterRegistry = Metrics.globalRegistry
    ) : DraftFactory<Season7TournamentDraft> {
        override val identifier = Season7TournamentDraft::class.simpleName!!
        override val friendlyName = "Season 7 Tournament, 1 vs 1 draft (2 bans, 2 major, 2 minor)"
        override fun createDraft(): Season7TournamentDraft {
            return Season7TournamentDraft(
                configSource.draftPool,
                configSource.presets["S7 Tournament"]!!,
                seedGenerator,
                meterRegistry
            )
        }
    }

    companion object : KLogging() {
        private const val BAN_MINOR_CHANCE = 0.2

        private fun draftChoiceValue(draftable: Draftable, optionName: String) = "${draftable.name}=$optionName"

        private fun JDA.removeEventListenerLater(eventListener: Any, timeout: Duration = ButtonDefaults.EXPIRATION) {
            if (timeout.isPositive() && timeout.isFinite()) {
                scope.launch {
                    delay(timeout)
                    removeEventListener(eventListener)
                }
            }
        }

        private fun MeterRegistry.countPickingOrder(firstPick: String) {
            counter(
                "draftbot.drafts.picking_order",
                "draft_type",
                Season7TournamentDraft::class.simpleName,
                "first_pick",
                firstPick
            ).increment()
        }
        private fun MeterRegistry.countPick(setting: String, settingType: String, pickedBy: String) {
            counter(
                "draftbot.drafts.picked_setting",
                "type",
                Season7TournamentDraft::class.simpleName,
                "setting",
                setting,
                "setting_type",
                settingType,
                "picked_by",
                pickedBy,
            ).increment()
        }

        private fun MeterRegistry.countBan(setting: String, settingType: String, bannedBy: String) {
            counter(
                "draftbot.drafts.banned_setting",
                "type",
                Season7TournamentDraft::class.simpleName,
                "setting",
                setting,
                "setting_type",
                settingType,
                "banned_by",
                bannedBy,
            ).increment()
        }
    }
}