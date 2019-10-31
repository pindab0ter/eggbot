package nl.pindab0ter.eggbot.commands

import com.github.kittinunf.result.success
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.commands.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.ContractSimulation
import nl.pindab0ter.eggbot.utilities.PrerequisitesCheckResult
import nl.pindab0ter.eggbot.utilities.arguments
import nl.pindab0ter.eggbot.utilities.checkPrerequisites
import org.jetbrains.exposed.sql.transactions.transaction

object SoloInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "solo"
        aliases = arrayOf("soloinfo", "si", "solo-info")
        help = "Shows the progress of one of your own contracts."
        arguments = "<contract id> [compact]"
        category = ContractsCategory
        guildOnly = false
    }

    @Suppress("ReplaceSizeZeroCheckWithIsEmpty", "FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            minArguments = 1,
            maxArguments = 2
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val farmers = transaction { DiscordUser.findById(event.author.id)?.farmers?.toList()!! }
        val contractId = event.arguments.first()
        val compact: Boolean = event.arguments.getOrNull(1)?.startsWith("c") == true

        for (farmer: Farmer in farmers) AuxBrain.getFarmerBackup(farmer.inGameId).success { backup ->
            val contract = if (backup.hasGame()) backup.contracts.contractsList.find {
                it.contract.id == contractId
            } else "No data found for `${farmer.inGameName}`.".let {
                log.warn { it }
                event.reply(it)
                return@success
            }

            if (contract == null)
                "No contract found with ID `$contractId` for `${farmer.inGameName}`. Try using `${event.client.textualPrefix}${ContractIDs.name}`".let {
                    log.debug { it }
                    event.reply(it)
                    return@success
                }
            if (contract.contract.coopAllowed && contract.coopId.isNotBlank())
                "The contract with ID `$contractId` is not a solo contract.".let {
                    log.debug { it }
                    event.reply(it)
                    return@success
                }
            val simulation = ContractSimulation(backup, contractId)
                ?: "You haven't started this contract yet.".let {
                    log.debug { it }
                    event.reply(it)
                    return@success
                }

            simulation.run()

            Messages.soloStatus(simulation, compact).let { message ->
                if (event.channel.id == Config.botCommandsChannel) {
                    event.reply(message)
                } else {
                    event.replyInDm(message)
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
            }
        }
    }
}
