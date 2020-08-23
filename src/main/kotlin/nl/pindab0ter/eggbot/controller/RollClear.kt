package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.controller.categories.AdminCategory
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.helpers.CONTRACT_ID
import nl.pindab0ter.eggbot.helpers.contractIdOption
import org.jetbrains.exposed.sql.transactions.transaction

object RollClear : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    init {
        category = AdminCategory
        name = "roll-clear"
        help = "Delete all the roles associated with the specified `<contract id>`."
        adminRequired = true
        parameters = listOf(contractIdOption)
        botPermissions = arrayOf(MANAGE_ROLES)
        sendTyping = false
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId = parameters.getString(CONTRACT_ID)

        val coops: List<Coop> = transaction {
            Coop.find { Coops.contract eq contractId }.toList()
        }

        if (coops.isEmpty()) "Didn't find any co-ops for $contractId".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val coopsToRoles = coops.zip(coops.map { coop ->
            coop.roleId?.let { guild.getRoleById(it) }
        })

        val message = event.channel.sendMessage("Deleting roles…").complete()

        val successes = mutableListOf<Pair<String, String?>>()
        val failures = mutableListOf<Pair<String, String?>>()

        // TODO: Move this responsibility to somewhere else
        coopsToRoles.forEach { (coop, role) ->
            val coopNameToRoleName = coop.name to role?.name
            if (role != null) role.delete().submit().handle { _, exception ->
                if (exception != null) {
                    failures.add(coopNameToRoleName)
                    log.warn { "Failed to remove Discord role (${exception.localizedMessage})" }
                    event.replyWarning("Failed to remove Discord role (${exception.localizedMessage})")
                } else {
                    transaction { coop.delete() }
                    successes.add(coopNameToRoleName)
                    log.info { "Co-op ${coopNameToRoleName.first} and role ${coopNameToRoleName.second} successfully removed." }
                }
            }.join() else {
                transaction { coop.delete() }
                successes.add(coopNameToRoleName)
                log.info { "Co-op ${coopNameToRoleName.first} successfully removed." }
            }
        }

        StringBuilder().apply {
            if (successes.isNotEmpty()) {
                appendLine("${Config.emojiSuccess} The following coops for `$contractId` have been deleted:")
                appendLine("```")
                successes.forEach { appendLine("${it.first}${it.second?.let { role -> " (@${role})" } ?: ""}") }
                appendLine("```")
            }
            if (failures.isNotEmpty()) {
                appendLine("${Config.emojiWarning} The following coops for `$contractId` could not be deleted:")
                appendLine("```")
                successes.forEach { appendLine("${it.first}${it.second?.let { role -> " (@${role})" } ?: ""}") }
                appendLine("```")
            }
        }.toString().let { messageBody ->
            message.delete().complete()
            event.reply(messageBody)
        }
    }
}
