package nl.pindab0ter.eggbot

import com.auxbrain.ei.EggInc
import nl.pindab0ter.eggbot.auxbrain.Simulation
import nl.pindab0ter.eggbot.database.Contract
import nl.pindab0ter.eggbot.database.Farmer
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

object Messages {
    private data class NameToValue(val name: String, val value: String)

    private fun leaderBoard(title: String, farmers: List<NameToValue>): List<String> =
        StringBuilder("$title leader board:\n").apply {
            append("```")
            farmers.forEachIndexed { index, (name, value) ->
                appendPaddingSpaces(index + 1, farmers.count())
                append("${index + 1}:")
                append(" ")
                append(name)
                appendPaddingSpaces(name, farmers.map { it.name })
                append("  ")
                appendPaddingSpaces(
                    value.split(Regex("[^,.\\d]"), 2).first(),
                    farmers.map { it.value.split(Regex("[^,.\\d]"), 2).first() })
                append(value)
                if (index < farmers.size - 1) appendln()
            }
        }.toString().splitMessage(prefix = "```", postfix = "```")

    fun earningsBonusLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Earnings Bonus",
        farmers.map { NameToValue(it.inGameName, it.earningsBonus.formatInteger() + "\u00A0%") }
    )

    fun earningsBonusLeaderBoardCompact(farmers: List<Farmer>): List<String> = leaderBoard(
        "Earnings Bonus",
        farmers.map { NameToValue(it.inGameName, it.earningsBonus.formatIllions() + "\u00A0%") }
    )

    fun soulEggsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Soul Eggs",
        farmers.map { NameToValue(it.inGameName, it.soulEggs.formatInteger()) }
    )

    fun prestigesLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Prestiges",
        farmers.map { NameToValue(it.inGameName, it.prestiges.formatInteger()) }
    )

    fun droneTakedownsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.droneTakedowns.formatInteger()) }
    )

    fun eliteDroneTakedownsLeaderBoard(farmers: List<Farmer>): List<String> = leaderBoard(
        "Elite Drone Takedowns",
        farmers.map { NameToValue(it.inGameName, it.eliteDroneTakedowns.formatInteger()) }
    )


    fun earningsBonus(farmer: Farmer, compact: Boolean = false): String = StringBuilder().apply {
        val eb = farmer.earningsBonus.let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val se = BigDecimal(farmer.soulEggs).let { (if (compact) it.formatIllions() else it.formatInteger()) }
        val pe = farmer.prophecyEggs.formatInteger()
        val sb = "${farmer.soulBonus.formatInteger()}/140"
        val pb = "${farmer.prophecyBonus.formatInteger()}/5"
        val seToNext =
            farmer.nextRole
                ?.lowerBound
                ?.minus(farmer.earningsBonus)
                ?.divide(farmer.bonusPerSoulEgg, HALF_UP)
                ?.let { (if (compact) it.formatIllions() else it.formatInteger()) }
                ?.let { "+ $it" } ?: "Unknown"
        val role = farmer.role?.name ?: "Unknown"
        val strings = listOf(
            eb, se, seToNext, role
        )

        append("Earnings bonus for **${farmer.inGameName}**:```\n")
        append("Role:            ")
        append(" ".repeat(strings.maxBy { it.length }?.length?.minus(role.length) ?: 0))
        appendln(farmer.role?.name ?: "Unknown")
        append("Earnings bonus:  ")
        appendPaddingSpaces(eb, strings)
        appendln("$eb %")
        append("Soul Eggs:       ")
        appendPaddingSpaces(se, strings)
        appendln("$se SE")

        if (farmer.soulBonus < 140) {
            append("Soul Food:       ")
            appendPaddingSpaces(sb, strings)
            appendln(sb)
        }

        append("Prophecy Eggs:   ")
        appendPaddingSpaces(pe, strings)
        appendln("$pe PE")

        if (farmer.prophecyBonus < 5) {
            append("Prophecy Bonus:  ")
            appendPaddingSpaces(pb, strings)
            appendln(pb)
        }

        append("SE to next rank: ")
        appendPaddingSpaces(seToNext, strings)
        append("$seToNext SE")
        append("```")
    }.toString()

    fun soloStatus(
        simulation: Simulation
    ): String = StringBuilder("`${simulation.contractId}` (${simulation.contractName}):\n").apply {
        val eggEmote = Config.eggEmojiIds[simulation.egg]?.let { id ->
            EggBot.jdaClient.getEmoteById(id)?.asMention
        } ?: ""

        appendln("**Farmer**: `${simulation.backup.name}`")
        appendln("**Eggs**: ${simulation.eggsLaid.formatIllions()}$eggEmote")
        appendln("**Rate**: ${simulation.effectiveEggLayingRateHour.formatIllions(true)}/hr")
        appendln("**Time remaining**: ${simulation.timeRemaining.asDayHoursAndMinutes()}")
        appendln("**Required eggs**: ${simulation.requiredEggs.formatIllions(true)}")
        appendln("**Projected eggs with int. hatchery calm**: ${simulation.finalTargetWithCalm.formatIllions()}")
    }.toString()

    fun coopStatus(
        contract: Contract,
        coopStatus: EggInc.CoopStatusResponse
    ): String = StringBuilder().apply {
        val eggsLaid = coopStatus.contributorsList.sumByDouble { it.contributionAmount }
        val eggsPerSecond = coopStatus.contributorsList.sumByDouble { it.contributionRate }
        val eggsPerHour = eggsPerSecond.times(3600)
        val timeRemaining = coopStatus.secondsRemaining.toDuration()
        val requiredEggs = contract.finalAmount
        val projectedEggs = eggsLaid + eggsPerSecond * coopStatus.secondsRemaining
        val eggEmote = Config.eggEmojiIds[contract.egg]?.let { id ->
            EggBot.jdaClient.getEmoteById(id)?.asMention
        } ?: ""

        appendln("`${contract.identifier}` (${contract.name}):${if (eggEmote.isBlank()) "" else " $eggEmote"}")
        appendln("**Co-op**: `${coopStatus.coopIdentifier}`")
        appendln("**Time remaining**: ${timeRemaining.asDayHoursAndMinutes()}")

        if (coopStatus.contributorsCount == 0) {
            appendln("\nThis co-op has no members.")
            return@apply
        }

        appendln("**Current**: ${eggsLaid.formatIllions()} (${eggsPerHour.formatIllions()}/hr)")
        appendln("**Required**: ${requiredEggs.formatIllions(true)}")
        appendln("**Expected**: ${projectedEggs.formatIllions()}")
        appendln()

        if (contract.goals.all { goal -> eggsLaid >= goal.targetAmount }) {
            appendln("**Contract finished! ${Config.emojiSuccess}**")
            appendln()
        } else {
            append("Goals (${contract.goals.count { eggsLaid >= it.targetAmount }}/${contract.goals.count()}):\n```")
            contract.goals
                .mapIndexed { index, goal -> index to goal }
                .filter { (_, goal) -> eggsLaid < goal.targetAmount }
                .forEach { (index, goal) ->
                    val finishedIn = (goal.targetAmount - eggsLaid).div(eggsPerSecond).toDuration()
                    val success = finishedIn < timeRemaining
                    val oneYear = Duration(DateTime.now(), DateTime.now().plusYears(1))

                    appendPaddingSpaces(index + 1, coopStatus.contributorsCount)
                    append("${index + 1}: ")
                    appendPaddingSpaces(
                        goal.targetAmount.formatIllions(true),
                        contract.goals
                            .filter { eggsLaid < it.targetAmount }
                            .map { it.targetAmount.formatIllions(true) }
                    )
                    append(goal.targetAmount.formatIllions(true))
                    append(if (success) " ✓ " else " ✗ ")
                    if (finishedIn > oneYear) append("More than a year")
                    else append(finishedIn.asDayHoursAndMinutes())
                    if (index + 1 < contract.goals.count()) appendln()
                }
            appendln("```")
        }

        appendln("Members (${coopStatus.contributorsCount}/${contract.maxCoopSize}):")
        appendln("```")

        data class Contributor(
            val userName: String,
            val active: Boolean,
            val contributionAmount: String,
            val contributionRate: String
        )

        val coopInfo = coopStatus.contributorsList.mapIndexed { i, it ->
            Contributor(
                it.userName,
                it.active == 1,
                it.contributionAmount.formatIllions(),
                it.contributionRate.times(3600).formatIllions() + "/hr"
            )
        }
        coopInfo.forEachIndexed { index, (userName, active, amount, rate) ->
            appendPaddingSpaces(index + 1, coopStatus.contributorsCount)
            append("${index + 1}: ")
            append(userName)
            appendPaddingSpaces(userName + if (!active) "  zZ" else " ",
                coopInfo.map { it.userName + if (!it.active) "  zZ" else " " })
            if (!active) append("  zZ ")
            else append("  ")
            appendPaddingSpaces(amount, coopInfo.map { it.contributionAmount })
            append(amount)
            append("|")
            append(rate)
            appendln()
        }
        appendln("```")
    }.toString()
}
