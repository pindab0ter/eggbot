package nl.pindab0ter.eggbot.extensions

import com.auxbrain.ei.Backup
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.NO_ALIAS
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.AttemptStatus
import nl.pindab0ter.eggbot.helpers.AttemptStatus.*
import nl.pindab0ter.eggbot.helpers.attemptStatusFor
import nl.pindab0ter.eggbot.helpers.contract
import nl.pindab0ter.eggbot.helpers.mapAsync
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.withProgressBar
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class ContractAttemptsCommand : Extension() {
    override val name: String = javaClass.simpleName

    override suspend fun setup() = config.servers.forEach { server ->
        class ContractAttemptsArguments : Arguments() {
            val contract by contract()
        }

        publicSlashCommand(::ContractAttemptsArguments) {
            name = "contract-attempts"
            description = "Show who has or hasn’t attempted the specified contract"
            guild(server.snowflake)

            action {
                val farmerCount = transaction(databases[server.name]) { Farmer.count() }
                val farmers: Map<Backup, AttemptStatus> = withProgressBar(
                    goal = farmerCount.toInt(),
                    statusText = "Checking the attempt status of all farmers for __${arguments.contract.name}__…",
                    unit = "farmers",
                ) {
                    val backups = newSuspendedTransaction(null, databases[server.name]) {
                        Farmer.all().mapAsync { AuxBrain.getFarmerBackup(it.eggIncId, databases[server.name]).also { increment() } }
                    }

                    backups
                        .filterNotNull()
                        .associateWith { backup -> backup.attemptStatusFor(arguments.contract.id) }
                        .filter { (_, attemptStatus) -> attemptStatus != COMPLETED }
                }

                edit { content = "Finished checking all farmers." }

                if (farmers.isEmpty()) respond {
                    content = "All farmers have completed __${arguments.contract.name}__."
                } else respond {
                    content = buildString {
                        val failedToGetProphecyEggFarmers = farmers.filter { (_, attemptStatus) -> attemptStatus == FAILED_TO_GET_PROPHECY_EGG }
                        val failedToCompleteAllGoalsFarmers = farmers.filter { (_, attemptStatus) -> attemptStatus == FAILED_TO_COMPLETE_ALL_GOALS }
                        val neverAttemptedFarmers = farmers.filter { (_, attemptStatus) -> attemptStatus == NEVER_ATTEMPTED }
                        appendLine("The following farmers have not completed __${arguments.contract.name}__:")
                        if (failedToGetProphecyEggFarmers.isNotEmpty()) {
                            appendLine("- ${failedToGetProphecyEggFarmers.size} farmers have not earned the Prophecy Egg:")
                            append("  ")
                            appendLine(failedToGetProphecyEggFarmers.keys.joinToString { farmer ->
                                val userName = farmer.userName
                                "`${if (!userName.isNullOrBlank()) userName else NO_ALIAS}`"
                            })
                        }
                        if (failedToCompleteAllGoalsFarmers.isNotEmpty()) {
                            appendLine("- ${failedToCompleteAllGoalsFarmers.size} farmers have not completed all goals:")
                            append("  ")
                            appendLine(failedToCompleteAllGoalsFarmers.keys.joinToString { farmer ->
                                val userName = farmer.userName
                                "`${if (!userName.isNullOrBlank()) userName else NO_ALIAS}`"
                            })
                        }
                        if (neverAttemptedFarmers.isNotEmpty()) {
                            appendLine("- ${neverAttemptedFarmers.size} farmers have not attempted this contract:")
                            append("  ")
                            appendLine(neverAttemptedFarmers.keys.joinToString { farmer ->
                                val userName = farmer.userName
                                "`${if (!userName.isNullOrBlank()) userName else NO_ALIAS}`"
                            })
                        }
                    }
                }
            }
        }
    }
}
