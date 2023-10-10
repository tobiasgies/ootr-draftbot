package de.tobiasgies.ootr.draftbot.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateSeedResponse(
    val id: String,
    val version: String,
    val spoilers: Boolean
)
