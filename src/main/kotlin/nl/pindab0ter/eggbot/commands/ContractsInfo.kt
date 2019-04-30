package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.*
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.network.AuxBrain
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Duration

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
                        event.replyInDm(StringBuilder("**${localContract.contract.name}**:\n").apply {
                            try {
                                val eggs = localContract.myEggs
                                val rate = 0.0
                                val hourlyRate = rate.times(60)

                                val elapsedTime = Duration(localContract.timeAccepted.toDateTime(), DateTime.now())
                                val timeRemaining = localContract.contract.lengthSeconds.toDuration().minus(elapsedTime)
                                val requiredEggs = localContract.contract
                                    .goalsList[localContract.contract.goalsList.size - 1]
                                    .targetAmount
                                val projectedEggs = rate.times(localContract.coopGracePeriodEndTime / 60)

                                appendln("Eggs: ${eggs.formatIllions()}")
                                appendln("Rate: ${rate.formatIllions(true)} (${hourlyRate.formatIllions(true)}/hr)")
                                appendln("Time remaining: ${timeRemaining.asDayHoursAndMinutes()}")
                                append("Projected eggs: ${projectedEggs.formatIllions(true)}")
                                append("/")
                                append("${requiredEggs.formatIllions(true)}\n")
                            } catch (e: Exception) {
                                log.error(e) { "" }
                            }
                        }.toString())
                    }

                coopContracts
                    .map { it to AuxBrain.getCoopStatus(it.contract.identifier, it.coopIdentifier).get() }
                    .forEach { (localContract, coopStatus) ->
                        event.replyInDm(StringBuilder("**${localContract.contract.name}** (`${localContract.coopIdentifier}`):\n").apply {
                            val eggs = coopStatus.contributorsList.sumByDouble { it.contributionAmount }
                            val rate = coopStatus.contributorsList.sumByDouble { it.contributionRate }
                            val hourlyRate = rate.times(60)
                            val timeRemaining = coopStatus.secondsRemaining.toPeriod()
                            val requiredEggs = localContract.contract
                                .goalsList[localContract.contract.goalsList.size - 1]
                                .targetAmount
                            val projectedEggs = coopStatus.contributorsList
                                .sumByDouble { it.contributionRate }
                                .times(coopStatus.secondsRemaining / 60)

                            appendln("Eggs: ${eggs.formatIllions()}")
                            appendln("Rate: ${rate.formatIllions(true)} (${hourlyRate.formatIllions(true)}/hr)")
                            appendln("Time remaining: ${timeRemaining.asDayHoursAndMinutes()}")
                            append("Projected eggs: ${projectedEggs.formatIllions(true)}")
                            append("/")
                            append("${requiredEggs.formatIllions(true)}\n")
                            appendln("```")
                            val coopInfo = coopStatus.contributorsList.map {
                                Triple(
                                    it.userName,
                                    it.contributionAmount.formatIllions(true),
                                    it.contributionRate.formatIllions(true) + "/s"
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

