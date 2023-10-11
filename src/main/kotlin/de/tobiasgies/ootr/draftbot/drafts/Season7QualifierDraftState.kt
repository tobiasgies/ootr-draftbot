package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.DraftableOption
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan

class Season7QualifierDraftState @WithSpan constructor(
    @SpanAttribute val majorPick: String,
    @SpanAttribute val majorOption: DraftableOption,
    @SpanAttribute val minorPick: String,
    @SpanAttribute val minorOption: DraftableOption,
) : DraftResult {
    override val isComplete = true
    override val selectedSettings = mapOf(
        majorPick to majorOption,
        minorPick to minorOption,
    )

    override fun display() = buildString {
        appendLine("* **Major pick:** ${majorPick.capitalize()}: ${majorOption.name.capitalize()}")
        appendLine("* **Minor pick:** ${minorPick.capitalize()}: ${minorOption.name.capitalize()}")
    }

    companion object {
        fun randomize(draftPool: DraftPool): Season7QualifierDraftState {
            val majorPick = draftPool.major.entries.random().value
            val majorOption = majorPick.options.entries.random().value
            val minorPick = draftPool.minor.entries.random().value
            val minorOption = minorPick.options.entries.random().value
            return Season7QualifierDraftState(
                majorPick = majorPick.name,
                majorOption = majorOption,
                minorPick = minorPick.name,
                minorOption = minorOption,
            )
        }
    }
}