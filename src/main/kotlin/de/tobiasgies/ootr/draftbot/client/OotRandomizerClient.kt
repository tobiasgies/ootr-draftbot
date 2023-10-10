package de.tobiasgies.ootr.draftbot.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.Preset
import de.tobiasgies.ootr.draftbot.dto.DraftPoolDto
import de.tobiasgies.ootr.draftbot.util.cached
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request

class OotRandomizerClient(private val httpClient: OkHttpClient) : ConfigSource {
    private val om = ObjectMapper().registerKotlinModule()
    private fun <T> fetch(url: String, parser: (String) -> T): T {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Failed to load data: $response")
        }

        val body = response.body?.string()
            ?: throw RuntimeException("Retrieved response body is empty")

        return parser(body)
    }

    override val presets by cached {
        val parser: (String) -> Map<String, Preset> = { responseBody ->
            val mapOfMaps = om.readValue(responseBody, object : TypeReference<Map<String, Map<String, Any>>>() {})
            mapOfMaps.mapValues { Preset(it.key, it.value) }
        }
        fetch(PRESETS_ENDPOINT, parser)
    }

    override val draftPool by cached {
        val parser: (String) -> DraftPool = { responseBody ->
            val dto = om.readValue(responseBody, DraftPoolDto::class.java)
            dto.toDraftPool()
        }
        fetch(DRAFT_POOL_ENDPOINT, parser)
    }

    companion object : KLogging() {
        private val PRESETS_ENDPOINT = "https://raw.githubusercontent.com/TestRunnerSRL/OoT-Randomizer/Dev/data/presets_default.json"
        private val DRAFT_POOL_ENDPOINT = "https://ootrandomizer.com/rtgg/draft_settings.json"
    }
}