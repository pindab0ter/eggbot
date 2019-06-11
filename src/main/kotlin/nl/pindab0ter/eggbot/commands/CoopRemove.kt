package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.missingArguments
import nl.pindab0ter.eggbot.tooManyArguments
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("FoldInitializerAndIfToElvis")
object CoopRemove : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop-remove"
        aliases = arrayOf("co-op-remove", "cr")
        arguments = "<contract-id> <co-op id>"
        help = "Shows the progress of a specific co-op."
        hidden = true
        // category = ContractsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        // If the admin role is defined check whether the author has at least that role or is the guild owner
        if (Config.rollCallRole != null && event.author.mutualGuilds.none { guild ->
                guild.getMember(event.author).let { author ->
                    author.isOwner || author.roles.any { memberRole ->
                        guild.getRolesByName(Config.rollCallRole, true)
                            .any { adminRole -> memberRole.position >= adminRole.position }
                    }
                }
            } || event.author.id == Config.ownerId) "You must have at least a role called `${Config.rollCallRole}` to use that!".let {
            event.replyError(it)
            log.debug { it }
            return
        }

        when {
            event.arguments.size < 2 -> missingArguments.let {
                event.replyWarning(it)
                log.debug { it }
                return
            }
            event.arguments.size > 2 -> tooManyArguments.let {
                event.replyWarning(it)
                log.debug { it }
                return
            }
        }

        val contractId: String = event.arguments[0]
        val coopId: String = event.arguments[1]
        transaction {
            val results = Coop.find { (Coops.name eq coopId) and (Coops.contract eq contractId) }.toList()

            if (results.isEmpty()) "No co-op registered with that `contract id` and `co-op id`.".let {
                event.replyWarning(it)
                log.debug { it }
                return@transaction
            }

            results.first().delete()

            "Co-op successfully removed.".let {
                event.replySuccess(it)
                log.debug { it }
                return@transaction
            }
        }
    }
}

