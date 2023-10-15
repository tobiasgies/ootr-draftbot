package de.tobiasgies.ootr.draftbot.dto

import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.Draftable
import de.tobiasgies.ootr.draftbot.data.DraftableOption

// TODO this can probably be handled with a custom deserializer, but I wanna get it working first
data class DraftPoolDto(
    val major: Map<String, Map<String, Map<String, Any>>>,
    val minor: Map<String, Map<String, Map<String, Any>>>,
) {
    fun toDraftPool() = DraftPool(
        major = major.mapValues { (name, draftable) ->
            Draftable(name, draftable.mapValues { (optionName, settings) ->
                DraftableOption(name, optionName, settings)
            })
        },
        minor = minor.mapValues { (name, draftable) ->
            Draftable(name, draftable.mapValues { (optionName, settings) ->
                DraftableOption(name, optionName, settings)
            })
        },
    )
}