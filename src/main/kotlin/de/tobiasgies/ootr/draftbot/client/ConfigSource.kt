package de.tobiasgies.ootr.draftbot.client

import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.Preset

interface ConfigSource {
    val presets: Map<String, Preset>
    val draftPool: DraftPool
}