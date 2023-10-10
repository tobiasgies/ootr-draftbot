package de.tobiasgies.ootr.draftbot.client

import de.tobiasgies.ootr.draftbot.data.Preset

interface PresetsLoader {
    fun loadPresets(): Map<String, Preset>
}