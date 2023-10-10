package de.tobiasgies.ootr.draftbot.data

class DraftState(initialDraftPool: DraftPool) {
    var draftPool: DraftPool = initialDraftPool
        private set
    var currentStep: DraftStep = DraftStep.PICK_ORDER
        private set
    var userBansFirst: Boolean? = null
        private set
    var bans: List<String> = emptyList()
        private set
    var majorPicks: Map<String, DraftableOption> = mapOf()
        private set
    var minorPicks: Map<String, DraftableOption> = mapOf()
        private set

    fun userBansFirst() {
        if (currentStep != DraftStep.PICK_ORDER) {
            throw IllegalStateException("Cannot set ban order after the draft has started")
        }
        userBansFirst = true
        currentStep = DraftStep.BAN
    }

    fun botBansFirst() {
        if (currentStep != DraftStep.PICK_ORDER) {
            throw IllegalStateException("Cannot set ban order after the draft has started")
        }
        userBansFirst = false
        currentStep = DraftStep.BAN
    }

    fun banSetting(name: String) {
        if (currentStep != DraftStep.BAN) {
            throw IllegalStateException("Cannot ban a setting before the ban step")
        }
        if (draftPool.major.containsKey(name)) {
            draftPool = draftPool.without(name)
            bans += name
        } else if (draftPool.minor.containsKey(name)) {
            draftPool = draftPool.without(name)
            bans += name
        } else {
            throw IllegalArgumentException("Unknown draftable setting: $name")
        }
        if (bans.size == 2) {
            currentStep = DraftStep.PICK_MAJOR
        }
    }

    fun pickMajor(name: String, optionName: String) {
        if (currentStep != DraftStep.PICK_MAJOR) {
            throw IllegalStateException("Cannot pick a major setting before the major pick step")
        }
        if (!draftPool.major.containsKey(name)) {
            throw IllegalArgumentException("Unknown major setting: $name")
        }
        if (!draftPool.major[name]!!.options.containsKey(optionName)) {
            throw IllegalArgumentException("Unknown option for major setting $name: $optionName")
        }
        val option = draftPool.major[name]!!.options[optionName]!!
        draftPool = draftPool.without(name)
        majorPicks += name to option
        if (majorPicks.size == 2) {
            currentStep = DraftStep.PICK_MINOR
        }
    }

    fun pickMinor(name: String, optionName: String) {
        if (currentStep != DraftStep.PICK_MINOR) {
            throw IllegalStateException("Cannot pick a minor setting before the minor pick step")
        }
        if (!draftPool.minor.containsKey(name)) {
            throw IllegalArgumentException("Unknown minor setting: $name")
        }
        if (!draftPool.minor[name]!!.options.containsKey(optionName)) {
            throw IllegalArgumentException("Unknown option for minor setting $name: $optionName")
        }
        val option = draftPool.minor[name]!!.options[optionName]!!
        draftPool = draftPool.without(name)
        minorPicks += name to option
        if (minorPicks.size == 2) {
            currentStep = DraftStep.DONE
        }
    }

    fun display(): String {
        return buildString {
            if (userBansFirst != null) {
                val firstToBan = if (userBansFirst == true) "User" else "Bot"
                appendLine("* **First to ban:** $firstToBan")
            }
            if (bans.isNotEmpty()) {
                appendLine("* **Bans:** ${bans.map { it.capitalize() }.joinToString(", ")}")
            }
            if (majorPicks.isNotEmpty()) {
                append("* **Major picks:** ")
                appendLine(majorPicks.map {
                    "${it.key.capitalize()}: ${it.value.name.capitalize()}"
                }.joinToString(", "))

            }
            if (minorPicks.isNotEmpty()) {
                append("* **Minor picks:** ")
                appendLine(minorPicks.map {
                    "${it.key.capitalize()}: ${it.value.name.capitalize()}"
                }.joinToString(", "))
            }
        }
    }

    override fun toString(): String {
        return "DraftState(draftPool=$draftPool, currentStep=$currentStep, userBansFirst=$userBansFirst, bans=$bans, majorPicks=$majorPicks, minorPicks=$minorPicks)"
    }
}