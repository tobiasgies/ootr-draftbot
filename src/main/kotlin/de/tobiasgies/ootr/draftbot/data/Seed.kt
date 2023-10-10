package de.tobiasgies.ootr.draftbot.data

data class Seed(
    val id: String
) {
    val url = "https://ootrandomizer.com/seed/get?id=$id"
}
