package de.tobiasgies.ootr.draftbot.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.tobiasgies.ootr.draftbot.data.Preset

class LocalResourcePresetsLoader : PresetsLoader {
    private val om = ObjectMapper().registerKotlinModule()

    override fun loadPresets(): Map<String, Preset> {
        val file = javaClass.classLoader.getResourceAsStream("presets_default.json")
        val mapOfMaps = om.readValue(file, object : TypeReference<Map<String, Map<String, Any>>>() {})
        return mapOfMaps.mapValues { Preset(it.key, it.value) }
    }

}