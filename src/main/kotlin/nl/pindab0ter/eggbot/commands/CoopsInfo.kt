package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.menu.OrderedMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.asyncMap
import nl.pindab0ter.eggbot.auxbrain.CoopContractSimulation
import nl.pindab0ter.eggbot.auxbrain.CoopContractSimulationResult.*
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.TimeUnit.MINUTES

object CoopsInfo : Command() {

    private val log = KotlinLogging.logger { }

    // TODO: Allow coop-id argument
    init {
        name = "coops"
        help = "Shows info on all co-ops for the chosen contract."
        // category = ContractsCategory
        guildOnly = false
        botPermissions = arrayOf(MESSAGE_EMBED_LINKS)
    }

    val builder = OrderedMenu.Builder()
        .allowTextInput(true)
        .useNumbers()
        .setEventWaiter(EggBot.eventWaiter)
        .useCancelButton(true)
        .setTimeout(1, MINUTES)

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        val availableContracts: List<String> = transaction { Coop.all().map { it.contract } }.distinct()
        val contracts = AuxBrain.getContracts().contractsList.filter { availableContracts.contains(it.identifier) }

        builder
            .setText("Currently active contracts:")
            .clearChoices()
            .setSelection { m, i ->
                // TODO: Add progress indication and extend sendTyping when not done yet.
                m.channel.sendTyping().queue()

                val coops = runBlocking(Dispatchers.IO) {
                    transaction {
                        Coop.find { Coops.contract eq contracts[i - 1].identifier }.toList()
                    }.asyncMap { coop ->
                        CoopContractSimulation.Factory(coop.contract, coop.name)
                    }
                }

                m.channel.sendMessage(coops.joinToString("\n") { coop ->
                    when (coop) {
                        is Finished ->
                            "`${coop.coopId}`: ✓ Finished"
                        is InProgress -> when {
                            coop.simulation.projectedToFinish() -> "`${coop.simulation.coopId}`: ✓ Will finish"
                            else -> "`${coop.simulation.coopId}`: ✗ Won't finish"
                        }
                        is Empty -> "`${coop.coopId}`: ✗ Empty co-op, remove using `!co-op-remove ${coop.coopId}`?"
                        is NotFound -> "`${coop.coopId}`: ✗ No active co-op found, remove using `!co-op-remove ${coop.coopId}`?"
                    }
                }).queue()
            }
            .setCancel {}
            .setUsers(event.author)

        contracts.forEach { builder.addChoice("**`${it.identifier}`**: ${it.name}\n_${it.description}_") }

        builder.build().display(event.channel)
    }
}
