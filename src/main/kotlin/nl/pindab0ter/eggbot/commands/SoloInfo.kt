package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import net.dv8tion.jda.core.entities.ChannelType
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.auxbrain.Simulation
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction

object SoloInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "solo"
        aliases = arrayOf("soloinfo", "si", "solo-info")
        help = "Shows the progress of one of your own contracts."
        arguments = "<contract id>"
        // category = ContractsCategory
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
            event.arguments.size > 1 -> tooManyArguments.let {
                event.replyWarning(it)
                log.debug { it }
                return
            }
        }

        val contractId = event.arguments.first()

        farmers.forEach { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId) { (backup, _) ->
                val contract = if (backup == null || !backup.hasData())
                    "No data found for `${farmer.inGameName}`.".let {
                        log.warn { it }
                        event.reply(it)
                        return@getFarmerBackup
                    } else backup.contracts.contractsList.find { it.contract.identifier == contractId }

                if (contract == null)
                    "No contract found with ID `$contractId`. Try using `${event.client.textualPrefix}${ContractIDs.name}`".let {
                        log.debug { it }
                        event.reply(it)
                        return@getFarmerBackup
                    }
                if (contract.contract.coopAllowed == 1)
                    "The contract with ID `$contractId` is not a solo contract.".let {
                        log.debug { it }
                        event.reply(it)
                        return@getFarmerBackup
                    }

                Messages.soloStatus(Simulation(backup, contractId)).let { message ->
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
