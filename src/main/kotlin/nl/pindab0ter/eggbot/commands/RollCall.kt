package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.arguments
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.format
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.contracts.ExperimentalContracts

object RollCall : Command() {
    init {
        name = "rc"
        arguments = "<contract id>"
        aliases = arrayOf("rollcall", "roll-call")
        help = "Create a co-op roll call for the given contract id"
        // TODO: Make guild only
        guildOnly = false
    }

    @ExperimentalContracts
    override fun execute(event: CommandEvent) {
        if (event.arguments.count() < 1) {
            event.replyWarning("Missing argument(s). See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }
        if (event.arguments.count() > 1) {
            event.replyWarning("Too many arguments. See `${event.client.textualPrefix}${event.client.helpWord}` for more information")
            return
        }

        transaction {
            val contractInfo = AuxBrain.getContracts().contractsList.find { it.identifier == event.arguments.first() }

            if (contractInfo == null) {
                event.replyWarning("No active contract found with id `${event.arguments.first()}`")
                return@transaction
            }

            if (contractInfo.coopAllowed != 1) {
                event.replyWarning("Co-op is not allowed for this contract")
                return@transaction
            }

            val coops = listOf(Coop.new {
                name = "acluckerz10"
                contractId = contractInfo.identifier
//                hasStarted = false
            }, Coop.new {
                name = "bcluckerz10"
                contractId = contractInfo.identifier
//                hasStarted = false
            })

            commit()

            // distribute players

            val farmers = Farmer.all()

            farmers.forEachIndexed { index, f ->
                val coop = coops[index % coops.size]
                coop.farmers = SizedCollection(coop.farmers.plus(f))
            }

            commit()

            event.reply(
                "Coops: ```" + coops.joinToString("\n") { coop ->
                    "${coop.name} ${format(coop.farmers.map { it.earningsBonus }.sum())}"
                } + "\nTotal EB: ${format(farmers.map { it.earningsBonus }.sum())}```"
            )
        }
    }
}
