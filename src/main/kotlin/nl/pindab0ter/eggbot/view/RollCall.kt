package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.helpers.NumberFormatter
import nl.pindab0ter.eggbot.helpers.appendPaddingCharacters
import nl.pindab0ter.eggbot.helpers.formatIllions
import nl.pindab0ter.eggbot.model.database.Coop

fun rollCallResponse(
    contract: Contract,
    coops: List<Coop>,
): List<String> = listOf(buildString {
    appendLine("Co-ops generated for `${contract.id}`:")

    append("```")
    coops.forEach { coop ->
        append(coop.name)
        appendPaddingCharacters(coop.name, coops.map { it.name })
        append(" (")
        appendPaddingCharacters(coop.farmers.count(), coops.map { it.farmers.count() })
        append(coop.farmers.count())
        append("/${contract.maxCoopSize} members): ")
        appendPaddingCharacters(
            coop.activeEarningsBonus.formatIllions(NumberFormatter.INTEGER),
            coops.map { it.activeEarningsBonus.formatIllions(NumberFormatter.INTEGER) })
        append(coop.activeEarningsBonus.formatIllions(NumberFormatter.INTEGER) + " %")
        appendLine()
    }
    appendLine("```")
}).plus(coops.map { coop ->
    val role = coop.roleId?.let { EggBot.guild.getRoleById(it) }
    buildString {
        // Header
        append("**__Co-op ${role?.asMention ?: coop.name} (`${coop.name}`)")
        if (coop.hasLeader) appendLine(" led by:__ ${coop.leader!!.discordUser.asMention}**")
        else appendLine("__**")

        // Body
        coop.farmers
            .sortedBy { farmer -> farmer.inGameName }
            .forEach { farmer ->
                if (farmer.isActive.not()) append("_")
                append(farmer.discordUser.asMention)
                if (farmer.inGameName.isNotEmpty()) append(" (${farmer.inGameName})")
                if (farmer.isActive.not()) append(" (Inactive)_")
                appendLine()
            }
        appendLine()
    }
})
