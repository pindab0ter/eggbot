package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.network.AuxBrain.getCoopStatus
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("FoldInitializerAndIfToElvis")
object CoopAdd : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop-add"
        aliases = arrayOf("co-op-add", "ca")
        arguments = "<contract-id> <co-op id>"
        help = "Registers a co-op so it shows up in the co-ops info listing."
        category = AdminCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 1,
            maxArguments = 2
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractId: String = event.arguments[0]
        val coopId: String = event.arguments[1]
        val exists: Boolean = transaction {
            Coop.find { (Coops.name eq coopId) and (Coops.contract eq contractId) }.toList()
        }.isNotEmpty()

        if (exists) "Co-op is already registered.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        getCoopStatus(contractId, coopId).let getCoopStatus@{ (status, _) ->
            if (status == null || !status.isInitialized) "Could not find an active co-op with that `contract id` and `co-op id`.".let {
                event.replyWarning(it)
                log.debug { it }
                return@getCoopStatus
            }

            transaction {
                Coop.new {
                    this.name = status.coopId
                    this.contract = status.contractId
                }
            }

            "Successfully registered co-op `${status.coopId}` for contract `${status.contractId}`.".let {
                event.replySuccess(it)
                log.debug { it }
                return
            }
        }
    }
}
