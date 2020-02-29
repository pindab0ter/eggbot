package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain.getCoopStatus
import nl.pindab0ter.eggbot.utilities.PrerequisitesCheckResult
import nl.pindab0ter.eggbot.utilities.ProgressBar
import nl.pindab0ter.eggbot.utilities.ProgressBar.WhenDone
import nl.pindab0ter.eggbot.utilities.arguments
import nl.pindab0ter.eggbot.utilities.checkPrerequisites
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("FoldInitializerAndIfToElvis")
object CoopAdd : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "coop-add"
        arguments = "<contract-id> <co-op id> [norole]"
        help = "Registers an _already existing_ co-op so it shows up in the co-ops info listing and creates a new role which it assigns to all it's members unless `norole` is added."
        category = AdminCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 2,
            maxArguments = 3
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractId: String = event.arguments[0]
        val coopId: String = event.arguments[1]
        val noRole: Boolean = event.arguments.getOrNull(2) == "norole"
        val exists: Boolean = transaction {
            Coop.find { (Coops.name eq coopId) and (Coops.contract eq contractId) }.toList()
        }.isNotEmpty()

        if (exists) "Co-op is already registered.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        getCoopStatus(contractId, coopId).let getCoopStatus@{ status ->
            if (status == null || !status.isInitialized) "Could not find an active co-op with that `contract id` and `co-op id`.".let {
                event.replyWarning(it)
                log.debug { it }
                return@getCoopStatus
            }

            val role = if (noRole) null else EggBot.guild.createRole()
                .setName(coopId)
                .setMentionable(true)
                .complete()

            transaction {
                Coop.new {
                    this.name = status.coopId
                    this.contract = status.contractId
                    this.roleId = role?.id
                }
            }

            if (role != null) {
                val message = event.channel.sendMessage("Assigning roles…").complete()
                val progressBar = ProgressBar(status.contributorsList.count(), message, WhenDone.STOP_IMMEDIATELY)

                val successes = mutableListOf<DiscordUser>()
                val failures = mutableListOf<String>()

                status.contributorsList.mapNotNull { contributionInfo ->
                    contributionInfo to transaction { Farmer.findById(contributionInfo.userId)?.discordUser }
                }.forEachIndexed { i, (contributionInfo, discordUser) ->
                    log.debug { "${contributionInfo.userName}, ${discordUser?.discordTag}" }
                    if (discordUser != null) EggBot.guild.addRoleToMember(discordUser.discordId, role).submit()
                        .handle { _, exception ->
                            if (exception == null) {
                                successes.add(discordUser)
                                log.debug("Added ${discordUser.discordTag} to @${role.name}")
                            } else {
                                failures.add(contributionInfo.userName)
                                log.warn("Failed to add ${discordUser.discordTag} to ${role.name}. Cause: ${exception.localizedMessage}")
                            }
                        }.join()
                    else failures.add(contributionInfo.userName)
                    progressBar.update(i + 1)
                }

                StringBuilder().apply {
                    appendln("${Config.emojiSuccess} Successfully registered co-op `${status.coopId}` for contract `${status.contractId}`.")
                    if (successes.isNotEmpty()) {
                        appendln()
                        appendln("The following players have been assigned the role ${role.asMention}:")
                        successes.forEach { discordUser -> appendln(EggBot.guild.getMemberById(discordUser.discordId)?.asMention) }
                    }
                    if (failures.isNotEmpty()) {
                        appendln()
                        appendln("Unable to assign the following players their role:")
                        appendln("```")
                        failures.forEach { userName -> appendln(userName) }
                        appendln("```")
                    }
                }.toString().let { messageBody ->
                    message.delete().complete()
                    event.reply(messageBody)
                }
            } else event.replySuccess("Successfully registered co-op `${status.coopId}` for contract `${status.contractId}`.")
        }
    }
}
