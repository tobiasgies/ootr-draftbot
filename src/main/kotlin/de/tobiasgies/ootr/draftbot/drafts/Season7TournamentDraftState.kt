package de.tobiasgies.ootr.draftbot.drafts

import de.tobiasgies.ootr.draftbot.data.DraftPool
import de.tobiasgies.ootr.draftbot.data.DraftableOption

class Season7TournamentDraftState(initialDraftPool: DraftPool) : DraftResult {
    var draftPool: DraftPool = initialDraftPool
        private set

    var currentStep: Step = Step.PICK_ORDER
        private set

    override val isComplete: Boolean
        get() = currentStep == Step.DONE

    override val selectedSettings: Map<String, DraftableOption>
        get() = majorPicks + minorPicks

    private var userBansFirst: Boolean? = null
    private var bans: List<String> = emptyList()
    private var majorPicks: Map<String, DraftableOption> = mapOf()
    private var minorPicks: Map<String, DraftableOption> = mapOf()

    fun userBansFirst() {
        if (currentStep != Step.PICK_ORDER) {
            throw IllegalStateException("Cannot set ban order after the draft has started")
        }
        userBansFirst = true
        currentStep = Step.BAN
    }

    fun botBansFirst() {
        if (currentStep != Step.PICK_ORDER) {
            throw IllegalStateException("Cannot set ban order after the draft has started")
        }
        userBansFirst = false
        currentStep = Step.BAN
    }

    fun banSetting(name: String) {
        if (currentStep != Step.BAN) {
            throw IllegalStateException("Cannot ban a setting before the ban step")
        }
        if (draftPool.major.containsKey(name) || draftPool.minor.containsKey(name)) {
            draftPool = draftPool.without(name)
            bans += name
        } else {
            throw IllegalArgumentException("Unknown draftable setting: $name")
        }
        if (bans.size == 2) {
            currentStep = Step.PICK_MAJOR
        }
    }

    fun pickMajor(name: String, optionName: String) {
        if (currentStep != Step.PICK_MAJOR) {
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
            currentStep = Step.PICK_MINOR
        }
    }

    fun pickMinor(name: String, optionName: String) {
        if (currentStep != Step.PICK_MINOR) {
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
            currentStep = Step.DONE
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

    enum class Step {
        PICK_ORDER,
        BAN,
        PICK_MAJOR,
        PICK_MINOR,
        DONE,
    }
}