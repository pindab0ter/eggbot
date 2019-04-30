package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Messages
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction

object ContractsInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "contracts"
        help = "Shows the progress of your own contracts."
        // category = ContractsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        val farmers = transaction { DiscordUser.findById(event.author.id)?.farmers?.toList() }

        @Suppress("FoldInitializerAndIfToElvis")
        if (farmers.isNullOrEmpty()) "You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        farmers.forEach { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId) { (backup, _) ->
                if (backup == null || !backup.hasData()) "No data found for `${farmer.inGameName}`.".let {
                    log.warn { it }
                    event.reply(it)
                    return@getFarmerBackup
                }

                if (backup.contracts.contractsList.isEmpty()) "No contracts found for ${farmer.inGameName}.".let {
                    log.trace { it }
                    event.reply(it)
                    return@getFarmerBackup
                }

                val (soloContracts, coopContracts) = backup.contracts.contractsList
                    .groupBy { it.contract.coopAllowed }
                    .let { it[0].orEmpty() to it[1].orEmpty() }

                soloContracts
                    .map { it to backup.farmsList.find { farm -> farm.contractId == it.contract.identifier }!! }
                    .forEach { (localContract, farm) ->
                        event.replyInDm(Messages.contractStatus(localContract, farm))
                    }

                coopContracts
                    .map { it to AuxBrain.getCoopStatus(it.contract.identifier, it.coopIdentifier).get() }
                    .forEach { (localContract, coopStatus) ->
                        event.replyInDm(Messages.coopStatus(localContract, coopStatus))
                    }
            }
        }
    }
}

