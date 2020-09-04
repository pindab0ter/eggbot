package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.Contract
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
import com.martiansoftware.jsap.UnflaggedOption
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import nl.pindab0ter.eggbot.EggBot.botOwner
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.EggBot.jdaClient
import nl.pindab0ter.eggbot.controller.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.CONTRACT_ID
import nl.pindab0ter.eggbot.helpers.contractIdOption
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.ProgressBar
import nl.pindab0ter.eggbot.model.ProgressBar.WhenDone
import nl.pindab0ter.eggbot.model.createRollCall
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.rollCallResponse
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction

object RollCall : EggBotCommand() {

    private val log = KotlinLogging.logger { }
    private val allowedCharacters = Regex("""^[a-zA-Z0-9\-]+$""")
    private const val BASE_NAME = "base name"
    private const val OVERWRITE = "overwrite"

    private val overwriteFlag = Switch(OVERWRITE)
        .setShortFlag('o')
        .setLongFlag("overwrite")
        .setHelp(
            "Force overwrite existing co-ops registered with ${jdaClient.selfUser.name}. This will also delete any roles currently associated with that co-op and re-create those."
        )

    private val baseNameOption = UnflaggedOption(BASE_NAME)
        .setRequired(REQUIRED)
        .setHelp(
            "The base name for the co-ops. Can only consist of letters, digits and dashes, cannot start with a dash and cannot contain spaces."
        )

    init {
        category = AdminCategory
        name = "roll-call"
        help = "Create a co-op roll call for the specified contract, creating co-ops and server roles and assigns those roles. The names will start with a letter followed by a dash and `<base name>`, which can only consist of letters, digits and dashes, cannot start with a dash and cannot contain spaces (e.g.: `a-basename`)."
        adminRequired = true
        parameters = listOf(
            contractIdOption,
            baseNameOption,
            overwriteFlag
        )
        botPermissions = arrayOf(MANAGE_ROLES)
        sendTyping = false
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val force: Boolean = parameters.getBoolean(OVERWRITE)
        val baseName = "-${parameters.getString(BASE_NAME)}"

        if (!allowedCharacters.matches(baseName)) "Only letters, digits and dashes are allowed.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contract: Contract = AuxBrain.getContract(contractId)
            ?: "No active contract found with id `${contractId}`".let {
                event.replyWarning(it)
                log.debug { it }
                return
            }

        if (!contract.coopAllowed) "Co-op is not allowed for this contract".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        transaction {
            val existingCoops: List<Coop> = Coop.find { Coops.contractId eq contract.id }.toList()
            if (existingCoops.isNotEmpty()) {
                if (force) {
                    val roles = existingCoops.mapNotNull { coop ->
                        coop.roleId?.let { guild.getRoleById(it) }
                    }

                    if (roles.isEmpty()) "No roles found for `${contract.id}`".let {
                        event.replyWarning(it)
                        log.warn { it }
                    }

                    val roleDeletions = roles.map { role ->
                        role.delete().submit()
                    }

                    if (roleDeletions.any { it.isCompletedExceptionally }) "Something went wrong. Please contact ${botOwner?.asMention ?: "the bot maintainer"}".let {
                        event.replyWarning(it)
                        log.error { it }
                    }

                    Coops.deleteWhere { Coops.contractId eq contract.id }
                    log.info { "Deleted contracts for ${contract.id}" }
                } else "Co-ops are already generated for contract `${contract.id}`. Add `overwrite` to override.".let {
                    event.replyWarning(it)
                    log.debug { it }
                    return@transaction
                }
            }

            val message = event.channel.sendMessage("Generating co-ops and creating roles…").complete()

            val farmers = transaction { Farmer.all().sortedByDescending { it.earningsBonus }.toList() }
            val coops: List<Coop> = createRollCall(farmers, contract, baseName)

            val progressBar = ProgressBar(farmers.count(), message, WhenDone.STOP_IMMEDIATELY)

            assignRoles(coops) {
                progressBar.update()
                event.channel.sendTyping().queue()
            }

            message.delete().complete()

            rollCallResponse(contract, coops).forEach { block ->
                event.reply(block)
            }
        }
    }

    private fun assignRoles(coops: List<Coop>, progressCallback: () -> Unit) = transaction {
        coops.map { coop ->
            coop.roleId?.let { guild.getRoleById(it) } to coop.farmers
        }.forEach { (role, coopFarmers) ->
            coopFarmers.map { farmer ->
                val discordId = farmer.discordUser.discordId
                val discordTag = farmer.discordUser.discordTag
                guild.addRoleToMember(discordId, role!!).queue({}, { exception ->
                    if (exception == null) log.info("Added $discordTag to ${role.name}")
                    else log.warn("Failed to add $discordTag to ${role.name}. Cause: ${exception.localizedMessage}")
                })
                progressCallback()
            }
        }
    }
}
