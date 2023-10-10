package de.tobiasgies.ootr.draftbot.data

data class Seed(
    val id: String
) {
    val uri = "https://ootrandomizer.com/seed/get?id=$id"
}
