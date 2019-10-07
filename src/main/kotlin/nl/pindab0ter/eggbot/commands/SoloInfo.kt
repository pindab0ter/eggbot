package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.commands.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.simulation.ContractSimulation
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

        val farmers = transaction { DiscordUser.findById(event.author.id)?.farmers?.toList() }

        @Suppress("FoldInitializerAndIfToElvis")
        if (farmers.isNullOrEmpty()) "You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        when {
            event.arguments.size < 1 -> missingArguments.let {
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

        val contractId = event.arguments.first()
        val compact: Boolean = event.arguments.getOrNull(1)?.startsWith("c") == true

        farmers.forEach { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId) { (backup, _) ->
                val contract = if (backup == null || !backup.hasGame())
                    "No data found for `${farmer.inGameName}`.".let {
                        log.warn { it }
                        event.reply(it)
                        return@getFarmerBackup
                    } else backup.contracts.contractsList.find { it.contract.id == contractId }

                if (contract == null)
                    "No contract found with ID `$contractId`. Try using `${event.client.textualPrefix}${ContractIDs.name}`".let {
                        log.debug { it }
                        event.reply(it)
                        return@getFarmerBackup
                    }
                if (contract.contract.coopAllowed && contract.coopId.isNotBlank())
                    "The contract with ID `$contractId` is not a solo contract.".let {
                        log.debug { it }
                        event.reply(it)
                        return@getFarmerBackup
                    }
                val simulation = ContractSimulation(backup, contractId)
                    ?: "You haven't started this contract yet.".let {
                        log.debug { it }
                        event.reply(it)
                        return@getFarmerBackup
                    }

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
}
