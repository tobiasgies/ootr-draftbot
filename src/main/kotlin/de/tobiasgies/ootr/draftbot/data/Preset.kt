package de.tobiasgies.ootr.draftbot.data

import com.fasterxml.jackson.annotation.JsonKey
import com.fasterxml.jackson.annotation.JsonValue

data class Preset(
    @JsonKey val fullName: String,
    @JsonValue val settings: Map<String, Any>,
)
