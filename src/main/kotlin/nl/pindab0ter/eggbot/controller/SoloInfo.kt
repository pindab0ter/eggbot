package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.helpers.COMPACT
import nl.pindab0ter.eggbot.helpers.CONTRACT_ID
import nl.pindab0ter.eggbot.helpers.compactSwitch
import nl.pindab0ter.eggbot.helpers.contractIdOption
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.simulation.new.simulateSoloContract
import nl.pindab0ter.eggbot.view.soloInfoResponse
import org.jetbrains.exposed.sql.transactions.transaction

object SoloInfo : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    // TODO: Add option to opt out of catching up (simulating from reported data)

    init {
        category = ContractsCategory
        name = "solo"
        help = "Shows the progress of a contract you're not in a co-op for."
        parameters = listOf(
            contractIdOption,
            compactSwitch
        )
        sendTyping = true
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val farmers = transaction { DiscordUser.findById(event.author.id)?.farmers?.toList()!! }
        val contractId: String = parameters.getString(CONTRACT_ID)
        val compact: Boolean = parameters.getBoolean(COMPACT, false)

        for (farmer: Farmer in farmers) AuxBrain.getFarmerBackup(farmer.inGameId)?.let { backup ->
            val localContract = if (backup.game != null) backup.contracts?.contracts?.find {
                it.contract?.id == contractId
            } else "No data found for `${farmer.inGameName}`.".let {
                log.warn { it }
                event.reply(it)
                return
            }
            if (localContract == null)
                "No contract found with ID `$contractId` for `${farmer.inGameName}`. Try using `${event.client.textualPrefix}${ContractIDs.name}`".let {
                    log.debug { it }
                    event.reply(it)
                    return
                }
            if (localContract.contract?.coopAllowed == true && localContract.coopId.isNotBlank())
                "The contract with ID `$contractId` is not a solo contract.".let {
                    log.debug { it }
                    event.reply(it)
                    return
                }

            val soloContractState = simulateSoloContract(backup, contractId)

            soloInfoResponse(soloContractState, compact).let { message ->
                if (event.channel == botCommandsChannel) {
                    event.reply(message)
                } else {
                    event.replyInDm(message)
                    if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                }
            }
        }
    }
}
