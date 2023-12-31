package de.tobiasgies.ootr.draftbot.data

import com.fasterxml.jackson.annotation.JsonKey
import com.fasterxml.jackson.annotation.JsonValue
import de.tobiasgies.ootr.draftbot.drafts.DraftResult
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan

data class Preset(
    @JsonKey val fullName: String,
    @JsonValue val settings: Map<String, Any>,
) {
    @WithSpan
    fun patchWithDraftResult(
        @SpanAttribute draftResult: DraftResult,
        specialCaseModifier: (DraftResult, Map<String, Any>) -> Map<String, Any> = { _, settings -> settings },
    ): Map<String, Any> {
        if (!draftResult.isComplete) {
            throw IllegalArgumentException("Cannot patch preset with incomplete draft result")
        }
        val patchedSettings = settings + draftResult.selectedSettings.entries.fold(mapOf()) { acc, entry ->
            acc + entry.value.settings
        }
        return specialCaseModifier(draftResult, patchedSettings)
    }
}
