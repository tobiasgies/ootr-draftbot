package de.tobiasgies.ootr.draftbot.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.tobiasgies.ootr.draftbot.data.Preset
import de.tobiasgies.ootr.draftbot.dto.DraftPoolDto

class LocalResourceConfigSource : ConfigSource {
    private val om = ObjectMapper().registerKotlinModule()

    override val draftPool by lazy {
        val file = javaClass.classLoader.getResourceAsStream("draft_settings.json")
        val dto = om.readValue(file, DraftPoolDto::class.java)
        dto.toDraftPool()
    }

    override val presets by lazy {
        val file = javaClass.classLoader.getResourceAsStream("presets_default.json")
        val mapOfMaps = om.readValue(file, object : TypeReference<Map<String, Map<String, Any>>>() {})
        mapOfMaps.mapValues { Preset(it.key, it.value) }
    }
}