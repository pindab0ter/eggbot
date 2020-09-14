package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.controller.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.CONTRACT_ID
import nl.pindab0ter.eggbot.helpers.contractIdOption
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.deleteCoopsAndRoles
import org.jetbrains.exposed.sql.transactions.transaction

object RollClear : EggBotCommand() {

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
            Coop.find { Coops.contractId eq contractId }.toList()
        }

        if (coops.isEmpty()) return event.replyAndLogWarning("Didn't find any co-ops for $contractId")

        val coopsToRoles = coops.zip(coops.map { coop ->
            coop.roleId?.let { guild.getRoleById(it) }
        })

        val message = event.channel.sendMessage("Deleting roles…").complete()

        val (successes, failures) = deleteCoopsAndRoles(coopsToRoles, event)

        buildString {
            if (successes.isNotEmpty()) {
                appendLine("${Config.emojiSuccess} The following coops for `$contractId` have been deleted:")
                appendLine("```")
                successes.forEach { (coopName, roleName) ->
                    appendLine("${coopName}${roleName?.let { " (@${it})" } ?: ""}")
                }
                appendLine("```")
            }
            if (failures.isNotEmpty()) {
                appendLine("${Config.emojiWarning} The following coops for `$contractId` could not be deleted:")
                appendLine("```")
                successes.forEach { (coopName, roleName) ->
                    appendLine("${coopName}${roleName?.let { " (@${it})" } ?: ""}")
                }
                appendLine("```")
            }
        }.let { messageBody ->
            message.delete().complete()
            event.reply(messageBody)
        }
    }

}
