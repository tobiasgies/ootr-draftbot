package de.tobiasgies.ootr.draftbot.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class SeedStatusResponse(
    val status: Status,
    val progress: Int,
    val version: String?,
    val positionQueue: Int?,
    val maxWaitTime: Int?,
    val isMultiWorld: Boolean?,
) {
    enum class Status(@get:JsonValue val numeric: Int) {
        PENDING(0),
        GENERATED_SUCCESS(1),
        GENERATED_WITH_LINK(2),
        FAILED(3);

        companion object {
            @JsonCreator
            fun fromNumeric(numeric: Int): Status {
                return when (numeric) {
                    PENDING.numeric -> PENDING
                    GENERATED_SUCCESS.numeric -> GENERATED_SUCCESS
                    GENERATED_WITH_LINK.numeric -> GENERATED_WITH_LINK
                    FAILED.numeric -> FAILED
                    else -> throw IllegalArgumentException("Unknown seed status: $numeric")
                }
            }
        }
    }
}
