package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.Contract
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.api.entities.Role
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.helpers.mapCartesianProducts
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor


private val DEFAULT_ROLE_COLOR = Color(15, 212, 57) // #0FD439

fun assignRoles(
    inGameNamesToDiscordIDs: Map<String, String>,
    role: Role,
    progressCallBack: () -> Unit,
): Pair<List<DiscordUser>, List<String>> {
    val logger = logger("assignRoles")
    val successes = mutableListOf<DiscordUser>()
    val failures = mutableListOf<String>()

    inGameNamesToDiscordIDs.map { (inGameName, discordID) ->
        inGameName to transaction { DiscordUser.findById(discordID) }
    }.forEach { (inGameName, discordUser) ->
        if (discordUser != null) EggBot.guild.addRoleToMember(discordUser.discordId, role).submit()
            .handle { _, exception ->
                if (exception == null) {
                    successes.add(discordUser)
                    logger.debug("Assigned @${role.name} to ${discordUser.discordTag}.")
                } else {
                    failures.add(inGameName)
                    logger.warn("Failed to assign @${role.name} to ${discordUser.discordTag}. Cause: ${exception.localizedMessage}")
                }
            }.join()
        else failures.add(inGameName)
        progressCallBack()
    }
    return successes to failures
}


fun createCoopsAndRoles(
    contract: Contract,
    amount: Int,
    baseName: String,
    noRole: Boolean,
): List<Coop> = transaction {
    val coopNames = coopNames(amount, baseName)
    List(amount) { index ->
        val roleId = if (noRole) null else EggBot.guild.createRole().run {
            setName(coopNames[index])
            setMentionable(true)
            setColor(DEFAULT_ROLE_COLOR)
            complete()
        }.id
        Coop.new {
            this.contractId = contract.id
            this.name = coopNames[index]
            this.roleId = roleId
        }
    }
}

private fun coopNames(amount: Int, baseName: String): List<String> = when {
    amount <= 26 -> ('a' until 'a' + amount).map { char -> "$char$baseName" }
    else -> {
        val chunks = ceil(amount.div(26.0)).toInt()
        val chunkSize = floor(amount.toDouble().div(chunks)).toInt()
        val remainder = amount - chunks * chunkSize
        ('a' until 'a' + chunkSize).mapCartesianProducts(1..chunks) { char: Char, digit: Int ->
            "$char$digit$baseName"
        }.plus(('a' until 'a' + remainder).map { c -> "${c + chunkSize}$baseName" })
    }
}

fun deleteCoopsAndRoles(
    coopsToRoles: List<Pair<Coop, Role?>>,
    event: CommandEvent,
): Pair<Map<String, String?>, Map<String, String?>> {
    val logger = logger("deleteCoopsAndRoles")
    val successes = mutableListOf<Pair<String, String?>>()
    val failures = mutableListOf<Pair<String, String?>>()

    coopsToRoles.forEach { (coop, role) ->
        val coopNameToRoleName = coop.name to role?.name
        if (role != null) role.delete().submit().handle { _, exception ->
            if (exception != null) "Failed to remove Discord role (${exception.localizedMessage})".let {
                failures.add(coopNameToRoleName)
                logger.warn { it }
                event.replyWarning(it)
            } else {
                transaction { coop.delete() }
                successes.add(coopNameToRoleName)
                logger.info { "Co-op ${coopNameToRoleName.first} and role ${coopNameToRoleName.second} successfully removed." }
            }
        }.join() else {
            transaction { coop.delete() }
            successes.add(coopNameToRoleName)
            logger.info { "Co-op ${coopNameToRoleName.first} successfully removed." }
        }
    }
    return successes.toMap() to failures.toMap()
}
