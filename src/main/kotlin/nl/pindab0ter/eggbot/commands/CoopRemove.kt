package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.utilities.PrerequisitesCheckResult
import nl.pindab0ter.eggbot.utilities.arguments
import nl.pindab0ter.eggbot.utilities.checkPrerequisites
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("FoldInitializerAndIfToElvis")
object CoopRemove : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop-remove"
        aliases = arrayOf("co-op-remove", "cr")
        arguments = "<contract id> <co-op id>"
        help = "Unregisters a co-op so it no longer shows up in the co-ops listing."
        category = AdminCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 2,
            maxArguments = 2
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractId: String = event.arguments[0]
        val coopId: String = event.arguments[1]
        val coop = transaction { Coop.find { (Coops.name eq coopId) and (Coops.contract eq contractId) }.firstOrNull() }

        if (coop == null) "No co-op registered with that `contract id` and `co-op id`.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val role = transaction { coop.roleId?.let { EggBot.guild.getRoleById(it) } }

        if (role != null) role.delete().queue({
            transaction { coop.delete() }
            "Co-op and role successfully removed.".let {
                event.replySuccess(it)
                log.debug { it }
            }
        }, { exception ->
            log.warn { exception.localizedMessage }
            event.replyWarning("Failed to remove Discord role (${exception.localizedMessage})")
        }) else {
            transaction { coop.delete() }
            "Co-op successfully removed.".let {
                event.replySuccess(it)
                log.debug { it }
            }
        }
    }
}
