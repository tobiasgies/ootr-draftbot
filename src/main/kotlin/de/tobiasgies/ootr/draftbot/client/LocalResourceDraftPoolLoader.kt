package de.tobiasgies.ootr.draftbot.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.dto.DraftPoolDto

class LocalResourceDraftPoolLoader : DraftPoolLoader {
    private val om = ObjectMapper().registerKotlinModule()

    override fun loadDraftPool(): DraftPool {
        val file = javaClass.classLoader.getResourceAsStream("draft_settings.json")
        val dto = om.readValue(file, DraftPoolDto::class.java)
        return dto.toDraftPool()
    }
}