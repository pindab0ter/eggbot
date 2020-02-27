package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.utilities.PrerequisitesCheckResult
import nl.pindab0ter.eggbot.utilities.arguments
import nl.pindab0ter.eggbot.utilities.checkPrerequisites
import org.jetbrains.exposed.sql.transactions.transaction

object RollClear : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "roll-clear"
        arguments = "<contract id>"
        aliases = arrayOf("role-clear")
        help = "Delete all the roles associated with the specified contract id."
        category = AdminCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 1,
            maxArguments = 1
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contract = event.arguments.first()

        val coops: List<Coop> = transaction {
            Coop.find { Coops.contract eq contract }.toList()
        }

        if (coops.isEmpty()) "Didn't find any co-ops for ${event.arguments.first()}".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val coopsToRoles = coops.zip(coops.map { coop ->
            coop.roleId?.let { EggBot.guild.getRoleById(it) }
        })

        val message = event.channel.sendMessage("Deleting roles…").complete()

        val successes = mutableListOf<Pair<String, String?>>()
        val failures = mutableListOf<Pair<String, String?>>()

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
                    log.debug { "Co-op ${coopNameToRoleName.first} and role ${coopNameToRoleName.second} successfully removed." }
                }
            }.join() else {
                transaction { coop.delete() }
                successes.add(coopNameToRoleName)
                log.debug { "Co-op ${coopNameToRoleName.first} successfully removed." }
            }
        }

        StringBuilder().apply {
            if (successes.isNotEmpty()) {
                appendln("${Config.emojiSuccess} The following coops for `$contract` have been deleted:")
                appendln("```")
                successes.forEach { appendln("${it.first}${it.second?.let { role -> " (@${role})" } ?: ""}") }
                appendln("```")
            }
            if (failures.isNotEmpty()) {
                appendln("${Config.emojiWarning} The following coops for `$contract` could not be deleted:")
                appendln("```")
                successes.forEach { appendln("${it.first}${it.second?.let { role -> " (@${role})" } ?: ""}") }
                appendln("```")
            }
        }.toString().let { message.editMessage(it).complete() }
    }
}
