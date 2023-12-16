package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.DraftableOption
import de.tobiasgies.ootr.draftbot.drafts.Season7FriendlyNames.friendlyName

class Season7TournamentFullAutoDraftState(
    val majorOptions: Collection<DraftableOption>,
    val minorOptions: Collection<DraftableOption>,
) : DraftResult {
    override val isComplete = true
    override val selectedSettings =
        (majorOptions.map { it.draftableName to it } + minorOptions.map { it.draftableName to it }).toMap()

    override fun display() = buildString {
        appendLine("* **Major picks:** ${majorOptions.joinToString { it.friendlyName }}")
        appendLine("* **Minor picks:** ${minorOptions.joinToString { it.friendlyName }}")
    }

    override fun toString(): String {
        return "Season7TournamentFullAutoDraftState(isComplete=$isComplete, selectedSettings=$selectedSettings)"
    }

    companion object {
        fun randomize(draftPool: DraftPool): Season7TournamentFullAutoDraftState {
            var pool = draftPool
            val firstMajor = pool.randomMajorOption()
            pool = pool.without(firstMajor.draftableName)
            val secondMajor = pool.randomMajorOption()
            pool = pool.without(secondMajor.draftableName)
            val firstMinor = pool.randomMinorOption()
            pool = pool.without(firstMinor.draftableName)
            val secondMinor = pool.randomMinorOption()
            return Season7TournamentFullAutoDraftState(
                majorOptions = listOf(firstMajor, secondMajor),
                minorOptions = listOf(firstMinor, secondMinor),
            )
        }
    }
}