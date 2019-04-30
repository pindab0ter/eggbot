package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction

object ContractInfo : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "contracts"
        aliases = arrayOf("contract", "co-op", "coop")
        arguments = "[co-op ID]"
        help = "Shows the progress of your own contracts or of a specific co-op."
        // category = ContractsCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        if (event.arguments.size > 1) tooManyArguments.let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        val farmers = transaction { DiscordUser.findById(event.author.id)?.farmers?.toList() }

        @Suppress("FoldInitializerAndIfToElvis")
        if (farmers.isNullOrEmpty()) "You are not yet registered. Please register using `${event.client.textualPrefix}${Register.name}`.".let {
            event.replyWarning(it)
            log.trace { it }
            return
        }

        farmers.forEach { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId) { (backup, _) ->
                if (backup == null || !backup.hasData()) "No data found for ${farmer.inGameName}.".let {
                    log.trace { it }
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

                coopContracts.map { it to AuxBrain.getCoopStatus(it.contract.identifier, it.coopIdentifier).get() }
                    .forEach { (localContract, coopStatus) ->
                        event.replyInDm(StringBuilder("Active Contracts:\n").apply {

                            val eggs = coopStatus.contributorsList
                                .sumByDouble { it.contributionAmount }
                                .formatShortScaleAbbreviated()

                            val rate = coopStatus.contributorsList.sumByDouble { it.contributionRate }
                                .formatShortScaleAbbreviated(true)

                            val hourlyRate = coopStatus.contributorsList.sumByDouble { it.contributionRate }
                                .times(60)
                                .formatShortScaleAbbreviated(true)

                            val timeRemaining = coopStatus.secondsRemaining.toPeriod()

                            val projectedEggs = "???"

                            appendln("**`${localContract.coopIdentifier}`** (${localContract.contract.name}):")
                            appendln("Eggs: $eggs")
                            appendln("Rate: $rate ($hourlyRate/hr)")
                            appendln("Time remaining: ${timeRemaining.asDayHoursAndMinutes()}")
                            appendln("Projected Eggs: $projectedEggs")
                            appendln("```")
                            val coopInfo = coopStatus.contributorsList.map {
                                Triple(
                                    it.userName,
                                    it.contributionAmount.formatShortScaleAbbreviated(true),
                                    it.contributionRate.formatShortScaleAbbreviated(true) + "/s"
                                )
                            }
                            coopInfo.forEach { (userName, amount, rate) ->
                                append(userName)
                                appendPaddingSpaces(userName, coopInfo.map { it.first })
                                append(" ")
                                appendPaddingSpaces(amount, coopInfo.map { it.second })
                                append(amount)
                                append("|")
                                append(rate)
                                appendln()
                            }
                            appendln("```")
                        }.toString())
                    }
            }
        }
    }
}

