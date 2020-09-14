package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.LocalContract
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import net.dv8tion.jda.api.entities.ChannelType
import nl.pindab0ter.eggbot.EggBot.botCommandsChannel
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.simulation.SoloContractState
import nl.pindab0ter.eggbot.model.simulation.simulate
import nl.pindab0ter.eggbot.view.soloFinishedIfCheckedInResponse
import nl.pindab0ter.eggbot.view.soloInfoResponse
import org.jetbrains.exposed.sql.transactions.transaction

object SoloInfo : EggBotCommand() {

    init {
        category = ContractsCategory
        name = "solo"
        help = "Shows the progress of a contract you're not in a co-op for."
        parameters = listOf(
            contractIdOption,
            compactSwitch,
            forceReportedOnlySwitch
        )
        sendTyping = true
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val compact: Boolean = parameters.getBoolean(COMPACT, false)
        val catchUp: Boolean = parameters.getBoolean(FORCE_REPORTED_ONLY, false).not()

        val farmers: List<Farmer> = transaction {
            DiscordUser.findById(event.author.id)?.farmers
        }?.toList() ?: return event.replyAndLogError("Could not find any farmers. Please contact the bot maintainer.")

        // TODO: Replace with parallelMap
        for (farmer: Farmer in farmers) AuxBrain.getFarmerBackup(farmer.inGameId)?.let { backup ->
            val localContract: LocalContract = backup.contracts?.contracts?.find { localContract ->
                localContract.contract?.id == contractId
            } ?: backup.contracts?.archive?.find { archivedContract ->
                archivedContract.contract!!.id == contractId
            }?.let { archivedContract ->
                return when {
                    archivedContract.contract!!.coopAllowed && archivedContract.coopId.isNotBlank() -> event.replyAndLogWarning(
                        "The contract with ID `$contractId` was not a solo contract."
                    )
                    archivedContract.finished -> event.replyAndLogWarning(
                        """
                        `${backup.userName}` vs. _${archivedContract.contract.name}_:

                        You have successfully completed this contract in the past! ${Config.emojiSuccess}
                        """.trimIndent()
                    )
                    else -> event.replyAndLogWarning(
                        """
                        `${backup.userName}` vs. _${archivedContract.contract.name}_:
                        
                        You have attempted this contract in the past, but not finished it.
                        """.trimIndent()
                    )
                }
            } ?: return event.replyAndLogWarning(
                """
                No contract found with ID `$contractId` for `${farmer.inGameName}`.
                Try using `${event.client.textualPrefix}${ContractIDs.name}`
                """.trimIndent()
            )

            if (localContract.contract?.coopAllowed == true && localContract.coopId.isNotBlank()) return event.replyAndLogWarning(
                "The contract with ID `$contractId` is not a solo contract."
            )

            val initialState = SoloContractState(backup, localContract, catchUp) ?: return event.replyAndLogWarning(
                "Failed to collect all necessary information from the backup."
            )

            val state = simulate(initialState)

            if (catchUp && state.farmer.initialState == state.farmer.finalState)
                soloFinishedIfCheckedInResponse(state, compact).let { message ->
                    if (event.channel == botCommandsChannel) {
                        event.reply(message)
                    } else {
                        event.replyInDm(message)
                        if (event.isFromType(ChannelType.TEXT)) event.reactSuccess()
                    }
                } else soloInfoResponse(state, compact).let { message ->
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
