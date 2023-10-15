package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.data.Draftable
import de.tobiasgies.ootr.draftbot.data.DraftableOption

object Season7FriendlyNames {
    private val draftables = mapOf(
        "bridge" to FriendlyName("Rainbow Bridge", mapOf("open" to "Open bridge, 6 med GCBK")),
        "deku" to FriendlyName("Kokiri Forest", mapOf("open" to "Open Forest")),
        "interiors" to FriendlyName("Indoor Entrance Randomizer", mapOf("on" to "Enabled")),
        "dungeons" to FriendlyName("Dungeon Entrance Randomizer", mapOf("on" to "Simple (no Ganon's Castle)")),
        "grottos" to FriendlyName("Grotto Entrance Randomizer", mapOf("on" to "Enabled")),
        "shops" to FriendlyName("Shopsanity", mapOf("on" to "4 items, random prices")),
        "ow_tokens" to FriendlyName("Overworld Tokens", mapOf("on" to "Shuffled")),
        "dungeon_tokens" to FriendlyName("Dungeon Tokens", mapOf("on" to "Shuffled")),
        "scrubs" to FriendlyName("Scrub shuffle", mapOf("on" to "Enabled, affordable prices")),
        "keys" to FriendlyName("Small Keys", mapOf(
            "keysy" to "Keysy (dungeon small keys and boss keys removed)",
            "anywhere" to "Keyrings anywhere (include Boss Keys)",
        )),
        "required_only" to FriendlyName("Guarantee Reachable Locations", mapOf(
            "on" to "Required Only (aka Beatable Only)"
        )),
        "fountain" to FriendlyName("Zora's Fountain", mapOf("open" to "Open Fountain")),
        "cows" to FriendlyName("Cowsanity", mapOf("on" to "Enabled")),
        "gerudo_card" to FriendlyName("Shuffle Gerudo Card", mapOf("on" to "Enabled")),
        "trials" to FriendlyName("Ganon's Trials", mapOf("on" to "3 Trials")),
        "door_of_time" to FriendlyName("Door of Time", mapOf("closed" to "Closed")),
        "starting_age" to FriendlyName("Starting Age", mapOf("child" to "Child", "adult" to "Adult")),
        "random_spawns" to FriendlyName("Random Spawns", mapOf("on" to "Enabled")),
        "consumables" to FriendlyName("Start with Consumables", mapOf("none" to "Disabled")),
        "rupees" to FriendlyName("Start with max Rupees", mapOf("startwith" to "Enabled")),
        "cuccos" to FriendlyName("Anju's Chickens", mapOf("1" to "1 Cucco")),
        "free_scarecrow" to FriendlyName("Free Scarecrow", mapOf("on" to "Enabled")),
        "camc" to FriendlyName("Chest Appearance Matches Contents", mapOf("off" to "Disabled")),
        "mask_quest" to FriendlyName("Complete Mask Quest", mapOf("complete" to "Enabled, fast Bunny Hood disabled")),
        "blue_fire_arrows" to FriendlyName("Blue Fire Arrows", mapOf("on" to "Enabled")),
        "owl_warps" to FriendlyName("Random Owl Warps", mapOf("on" to "Enabled")),
        "song_warps" to FriendlyName("Random Warp Song Destinations", mapOf("on" to "Enabled")),
        "shuffle_beans" to FriendlyName("Shuffle Magic Beans", mapOf("on" to "Enabled")),
        "expensive_merchants" to FriendlyName("Shuffle Expensive Merchants", mapOf("on" to "Enabled")),
        "beans_planted" to FriendlyName("Pre-planted Magic Beans", mapOf("on" to "Enabled")),
        "bombchus_in_logic" to FriendlyName("Add Bombchu Bag and Drops", mapOf("on" to "Enabled (Bombchus in logic)")),
    )

    val Draftable.friendlyName: String
        get() = draftables[name]?.draftable ?: name.capitalize()

    val DraftableOption.friendlyName: String
        get() = if (draftables[draftableName]?.options?.containsKey(optionName) == true) {
            val friendlyName = draftables[draftableName]!!
            "${friendlyName.draftable}: ${friendlyName.options[optionName]}"
        } else {
            "${draftableName.capitalize()}: ${optionName.capitalize()}"
        }

    data class FriendlyName(
        val draftable: String,
        val options: Map<String, String>,
    )
}