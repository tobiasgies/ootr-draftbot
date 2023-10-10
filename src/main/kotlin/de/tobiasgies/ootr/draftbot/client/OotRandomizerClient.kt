package de.tobiasgies.ootr.draftbot.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.Preset
import de.tobiasgies.ootr.draftbot.data.Seed
import de.tobiasgies.ootr.draftbot.dto.CreateSeedResponse
import de.tobiasgies.ootr.draftbot.dto.DraftPoolDto
import de.tobiasgies.ootr.draftbot.dto.SeedStatusResponse
import de.tobiasgies.ootr.draftbot.dto.SeedStatusResponse.Status
import de.tobiasgies.ootr.draftbot.dto.VersionResponse
import de.tobiasgies.ootr.draftbot.util.cached
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.executeAsync
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

class OotRandomizerClient(
    private val httpClient: OkHttpClient,
    private val apiKey: String
) : ConfigSource, SeedGenerator {
    private val om = ObjectMapper().registerKotlinModule()

    private fun <T> fetch(url: String, parser: (String) -> T): T {
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Failed to load data: $response")
        }

        val body = response.body.string()
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

    private val latestDevVersion by cached {
        val parser: (String) -> VersionResponse = { responseBody ->
            om.readValue(responseBody, VersionResponse::class.java)
        }
        fetch(VERSION_ENDPOINT, parser).currentlyActiveVersion
    }

    override suspend fun rollSeed(settings: Map<String, Any>) = doRollSeed(settings)

    private suspend fun doRollSeed(settings: Map<String, Any>, retryCount: Int = 0): Seed {
        val url = SEED_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("version", "dev_$latestDevVersion")
            .build()
        val request = Request.Builder()
            .url(url)
            .post(om.writeValueAsString(settings).toRequestBody(contentType = "application/json".toMediaType()))
            .build()
        try {
            val response = withContext(coroutineContext + Dispatchers.IO) {
                httpClient.newCall(request).executeAsync()
            }
            if (!response.isSuccessful) {
                throw RuntimeException("Request for a seed was not successful: $response")
            }
            val body = om.readValue(response.body.string(), CreateSeedResponse::class.java)
            val seed = Seed(body.id)
            awaitSeedReady(seed)
            return seed
        } catch (e: Exception) {
            if (retryCount >= 5) {
                logger.error(e) { "There is a persistent issue requesting a seed. Giving up." }
                throw e
            }
            val retryInterval = 1.seconds * (retryCount + 1)
            logger.warn(e) { "There was an issue requesting a seed. Retrying in $retryInterval." }
            delay(retryInterval)
            return doRollSeed(settings, retryCount + 1)
        }
    }

    private suspend fun awaitSeedReady(seed: Seed) {
        val url = STATUS_ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("key", apiKey)
            .addQueryParameter("id", seed.id)
            .build()
        val request = Request.Builder().url(url).build()

        // This method is called from inside a try block, hence no need for separate error handling
        val response = withContext(coroutineContext + Dispatchers.IO) {
            httpClient.newCall(request).executeAsync()
        }
        if (!response.isSuccessful) {
            throw RuntimeException("Request for seed ${seed.id} status was not successful: $response")
        }
        val body = om.readValue(response.body.string(), SeedStatusResponse::class.java)
        if (body.status == Status.FAILED) {
            throw RuntimeException("Seed ${seed.id} failed to generate: $response")
        }
        if (body.status == Status.PENDING) {
            delay(2.seconds)
            awaitSeedReady(seed)
        }
    }

    companion object : KLogging() {
        private val PRESETS_ENDPOINT = "https://raw.githubusercontent.com/TestRunnerSRL/OoT-Randomizer/Dev/data/presets_default.json"
        private val DRAFT_POOL_ENDPOINT = "https://ootrandomizer.com/rtgg/draft_settings.json"
        private val SEED_ENDPOINT = "https://ootrandomizer.com/api/v2/seed/create"
        private val STATUS_ENDPOINT = "https://ootrandomizer.com/api/v2/seed/status"
        private val VERSION_ENDPOINT = "https://ootrandomizer.com/api/version?branch=dev"
    }
}