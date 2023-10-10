package de.tobiasgies.ootr.draftbot.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class VersionResponse(
    val branch: String,
    val currentlyActiveVersion: String
)
