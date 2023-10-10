package de.tobiasgies.ootr.draftbot.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.Preset
import de.tobiasgies.ootr.draftbot.dto.DraftPoolDto

class LocalResourceLoader : DraftPoolLoader, PresetsLoader {
    private val om = ObjectMapper().registerKotlinModule()

    override fun loadDraftPool(): DraftPool {
        val file = javaClass.classLoader.getResourceAsStream("draft_settings.json")
        val dto = om.readValue(file, DraftPoolDto::class.java)
        return dto.toDraftPool()
    }

    override fun loadPresets(): Map<String, Preset> {
        val file = javaClass.classLoader.getResourceAsStream("presets_default.json")
        val mapOfMaps = om.readValue(file, object : TypeReference<Map<String, Map<String, Any>>>() {})
        return mapOfMaps.mapValues { Preset(it.key, it.value) }
    }
}