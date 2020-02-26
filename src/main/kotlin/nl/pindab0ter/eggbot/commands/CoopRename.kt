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
object CoopRename : Command() {

    private val log = KotlinLogging.logger { }
    private val allowedCharacters = Regex("""^[a-zA-Z0-9\-]+$""")

    init {
        name = "coop-rename"
        arguments = "<contract id> <co-op id> <new name>"
        help = "Renames a co-op and it's associated role if available. Only letters, digits and dashes allowed."
        category = AdminCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 3,
            maxArguments = 3
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractId: String = event.arguments[0]
        val coopId: String = event.arguments[1]
        val newName: String = event.arguments[2]
        val coop = transaction { Coop.find { (Coops.name eq coopId) and (Coops.contract eq contractId) }.firstOrNull() }

        if (coop == null) "No co-op registered with that `contract id` and `co-op id`.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        if (!allowedCharacters.matches(newName)) "Only letters, digits and dashes are allowed.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val role = transaction { coop.roleId?.let { EggBot.guild.getRoleById(it) } }

        if (role != null) role.manager.setName(newName).queue({
            transaction { coop.name = newName }
            "Co-op and role successfully renamed from $coopId to $newName.".let {
                event.replySuccess(it)
                log.debug { it }
            }
        }, { exception ->
            log.warn { exception.localizedMessage }
            event.replyWarning("Failed to rename Discord role (${exception.localizedMessage})")
        }) else {
            transaction { coop.name = newName }
            "Co-op successfully renamed.".let {
                event.replySuccess(it)
                log.debug { it }
            }
        }
    }
}
