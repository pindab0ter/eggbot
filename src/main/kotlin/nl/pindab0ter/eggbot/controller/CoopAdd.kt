package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.controller.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.AuxBrain.getCoopStatus
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.ProgressBar
import nl.pindab0ter.eggbot.model.assignRoles
import nl.pindab0ter.eggbot.model.database.Coop
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("FoldInitializerAndIfToElvis")
object CoopAdd : EggBotCommand() {

    init {
        category = AdminCategory
        name = "coop-add"
        help = "Registers an _already existing_ co-op so it shows up in the co-ops info listing and creates a new role which it assigns to all it's members unless the `no-role` flag is set."
        parameters = listOf(
            contractIdOption,
            coopIdOption,
            Switch(NO_ROLE)
                .setShortFlag('n')
                .setLongFlag("no-role")
                .setHelp("Don't create a role for this co-op. Use this when tracking co-ops not part of ${guild.name}"),
            Switch(FORCE)
                .setShortFlag('f')
                .setLongFlag("force")
                .setHelp("Add the co-op even if it doesn't exist. Does not create a role.")
        )
        sendTyping = false
        adminRequired = true
        botPermissions = arrayOf(MANAGE_ROLES)
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val coopId: String = parameters.getString(COOP_ID)
        val noRole: Boolean = parameters.getBoolean(NO_ROLE)
        val force: Boolean = parameters.getBoolean(FORCE)
        val exists: Boolean = transaction {
            Coop.find { (Coops.name eq coopId) and (Coops.contractId eq contractId) }.toList().isNotEmpty()
        }

        if (exists) return event.replyAndLogWarning(
            "Co-op is already registered."
        )

        AuxBrain.getContract(contractId) ?: return event.replyAndLogWarning(
            "Could not find an active contract with that `contract id`."
        )

        getCoopStatus(contractId, coopId).let getCoopStatus@{ status ->
            if (status == null && !force) return@getCoopStatus event.replyAndLogWarning(
                "Could not find an active co-op with that `contract id` and `co-op id`."
            )

            val role = if (noRole) null else guild.createRole()
                .setName(coopId)
                .setMentionable(true)
                .complete()

            transaction {
                Coop.new {
                    this.name = coopId
                    this.contractId = contractId
                    this.roleId = role?.id
                }
            }

            if (role != null && status != null) {
                val message = event.channel.sendMessage("Assigning roles…").complete()
                val progressBar = ProgressBar(status.contributors.count(), message)

                val (successes, failures) = assignRoles(
                    inGameNamesToDiscordIDs = status.contributors.map { contributor ->
                        contributor.userName to contributor.userId
                    }.toMap(),
                    role = role,
                    progressCallBack = progressBar::update
                )

                buildString {
                    appendLine("${Config.emojiSuccess} Successfully registered co-op `${status.coopId}` for contract `${status.contractId}`.")
                    if (successes.isNotEmpty()) {
                        appendLine()
                        appendLine("The following players have been assigned the role ${role.asMention}:")
                        successes.forEach { discordUser -> appendLine(guild.getMemberById(discordUser.discordId)?.asMention) }
                    }
                    if (failures.isNotEmpty()) {
                        appendLine()
                        appendLine("Unable to assign the following players their role:")
                        appendLine("```")
                        failures.forEach { userName -> appendLine(userName) }
                        appendLine("```")
                    }
                }.let { messageBody ->
                    message.delete().complete()
                    event.reply(messageBody)
                }
            } else event.replySuccess("Successfully registered co-op `${coopId}` for contract `${contractId}`.")
        }
    }
}
