package nl.pindab0ter.eggbot.view

import com.auxbrain.ei.Contract
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.entity.Role
import nl.pindab0ter.eggbot.helpers.NumberFormatter.INTEGER
import nl.pindab0ter.eggbot.helpers.appendPaddingCharacters
import nl.pindab0ter.eggbot.helpers.formatIllions
import nl.pindab0ter.eggbot.helpers.mentionUser
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Farmer

suspend fun GuildBehavior.rollCallResponse(
    contract: Contract,
    coops: List<Coop>,
): List<String> {
    val header = listOf(buildString {
        appendLine("Co-ops generated for __${contract.name}__:")

        append("```")
        coops.forEach { coop ->
            append(coop.name)
            appendPaddingCharacters(coop.name, coops.map { it.name })
            append(" (")
            appendPaddingCharacters(coop.farmers.count(), coops.map { it.farmers.count() })
            append(coop.farmers.count())
            append("/${contract.maxCoopSize} members): ")
            appendPaddingCharacters(
                coop.activeEarningsBonus.formatIllions(INTEGER),
                coops.map { it.activeEarningsBonus.formatIllions(INTEGER) })
            append(coop.activeEarningsBonus.formatIllions(INTEGER) + " %")
            appendLine()
        }
        appendLine("```")
    })

    val coopContent = coops.map { coop ->
        val role = coop.roleId?.let { getRoleOrNull(it) }
        buildString {
            // Header
            append("**__Co-op ")
            when (val roleMention = role?.mention) {
                null -> append("`${coop.name}`")
                else -> append("$roleMention (`${coop.name}`)")
            }
            appendLine("__**")

            // Body
            coop.farmers
                .sortedByDescending { farmer -> farmer.earningsBonus }
                .forEach { farmer ->
                    when (val userMention = asGuildOrNull()?.mentionUser(farmer.discordUser.snowflake)) {
                        null -> append("`${farmer.inGameName}`")
                        else -> append("$userMention (`${farmer.inGameName}`)")
                    }
                    appendLine()
                }
            appendLine()
        }
    }

    return header + coopContent
}

suspend fun GuildBehavior.coopChannelMessage(
    coop: Coop,
    role: Role?,
): String = buildString {
    // Header
    appendLine("**__Co-op ${role?.mention ?: coop.name} (`${coop.name}`)__**")

    // Body
    coop.farmers
        .sortedBy(Farmer::earningsBonus)
        .forEach { farmer ->
            when (val userMention = asGuildOrNull()?.mentionUser(farmer.discordUser.snowflake)) {
                null -> append("`${farmer.inGameName}`")
                else -> append("$userMention (`${farmer.inGameName}`)")
            }
            append(" ${farmer.earningsBonus.formatIllions(INTEGER)}%")
            appendLine()
        }
    appendLine()
}
