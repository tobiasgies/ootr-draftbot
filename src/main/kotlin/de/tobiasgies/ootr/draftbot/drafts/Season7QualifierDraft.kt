package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.data.DraftPick
import de.tobiasgies.ootr.draftbot.data.DraftPool
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

class Season7QualifierDraft(private val draftPool: DraftPool) : Draft {
    override val identifier = "s7_qual"
    override val friendlyName = "Season 7 Tournament, Qualifier Draft (1 major, 1 minor)"

    override suspend fun executeDraft(slashCommand: GenericCommandInteractionEvent) {
        val majorOption = draftPool.major.entries.random().value
        val majorPick = DraftPick(majorOption, majorOption.options.entries.random().value)
        val minorOption = draftPool.minor.entries.random().value
        val minorPick = DraftPick(minorOption, minorOption.options.entries.random().value)

        slashCommand.hook
            .sendMessage("${slashCommand.user.asMention}, I picked the following settings for you:\n\n" +
                    "* ${majorPick.draftable.name.capitalize()}: ${majorPick.option.name.capitalize()}\n" +
                    "* ${minorPick.draftable.name.capitalize()}: ${minorPick.option.name.capitalize()}\n\n" +
                    "Go to the [OOTRandomizer website](https://www.ootrandomizer.com/generatorDev), select the " +
                    "`S7 Tournament` preset, and change the settings as indicated above to roll your seed.")
            .queue()
    }
}