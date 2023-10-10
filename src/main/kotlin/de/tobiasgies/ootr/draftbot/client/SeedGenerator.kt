package de.tobiasgies.ootr.draftbot.client

import de.tobiasgies.ootr.draftbot.data.Seed

interface SeedGenerator {
    suspend fun rollSeed(settings: Map<String, Any>): Seed
}