package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.Contract
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.FlaggedOption
import com.martiansoftware.jsap.JSAP.*
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
import com.martiansoftware.jsap.UnflaggedOption
import kotlinx.coroutines.Dispatchers
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.controller.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.CONTRACT_ID
import nl.pindab0ter.eggbot.helpers.NO_ROLE
import nl.pindab0ter.eggbot.helpers.contractIdOption
import nl.pindab0ter.eggbot.helpers.getIntOrNull
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.*
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.rollCallResponse
import org.jetbrains.exposed.sql.transactions.transaction

object RollCall : EggBotCommand() {

    private val allowedCharacters = Regex("""^[a-zA-Z0-9\-]+$""")
    private const val BASE_NAME = "base name"
    private const val COOP_SIZE = "coop size"

    private val baseNameOption = UnflaggedOption(BASE_NAME)
        .setRequired(REQUIRED)
        .setHelp(
            "The base name for the co-ops. Can only consist of letters, digits and dashes, cannot start with a dash and cannot contain spaces."
        )

    private val noRoleFlag = Switch(NO_ROLE)
        .setShortFlag('n')
        .setLongFlag("no-roles")
        .setHelp("Don't create roles for these co-ops.")

    private val coopSizeOption = FlaggedOption(COOP_SIZE)
        .setRequired(NOT_REQUIRED)
        .setShortFlag('s')
        .setLongFlag("size")
        .setStringParser(INTEGER_PARSER)
        .setHelp("Create co-ops with the specified size.")

    init {
        category = AdminCategory
        name = "roll-call"
        help =
            "Create a co-op roll call for the specified contract, creating co-ops and server roles and assigns those roles. The names will start with a letter followed by a dash and `<base name>`, which can only consist of letters, digits and dashes, cannot start with a dash and cannot contain spaces (e.g.: `a-basename`)."
        adminRequired = true
        parameters = listOf(
            contractIdOption,
            baseNameOption,
            noRoleFlag,
            coopSizeOption,
        )
        botPermissions = arrayOf(MANAGE_ROLES)
        sendTyping = false
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val baseName = "-${parameters.getString(BASE_NAME)}"
        val noRoles: Boolean = parameters.getBoolean(NO_ROLE)
        val coopSize: Int? = parameters.getIntOrNull(COOP_SIZE)

        if (!allowedCharacters.matches(baseName)) return event.replyAndLogWarning(
            "Only letters, digits and dashes are allowed."
        )

        val contract: Contract = AuxBrain.getContract(contractId) ?: return event.replyAndLogWarning(
            "No active contract found with id `${contractId}`."
        )

        if (!contract.coopAllowed) return event.replyAndLogWarning(
            "Co-op is not allowed for this contract."
        )

        if (coopSize != null && ((coopSize > contract.maxCoopSize) || (coopSize <= 1))) return event.replyAndLogWarning(
            "Please enter a valid co-op size."
        )

        transaction {
            val existingCoops: List<Coop> = Coop.find { Coops.contractId eq contract.id }.toList()
            if (existingCoops.isNotEmpty()) return@transaction event.replyAndLogWarning(
                "Co-ops are already generated for contract `${contract.id}`."
            )

            val messageContents = "Generating co-ops${if (!noRoles) " and creating roles" else ""}…"
            val message = event.channel.sendMessage(messageContents).complete()

            val farmers = transaction { Farmer.all().sortedByDescending { it.earningsBonus }.toList() }
            val coops: List<Coop> = createRollCall(farmers, contract, baseName, noRoles, coopSize)

            if (!noRoles) {
                val progressBar = ProgressBar(
                    farmers.count(),
                    message,
                    statusText = messageContents,
                    unit = "roles assigned",
                    coroutineContext = Dispatchers.Default
                )

                coops.map { coop ->
                    coop.roleId?.let { guild.getRoleById(it) }?.let { role ->
                        assignRoles(
                            inGameNamesToDiscordIDs = coop.farmers.associate { farmer ->
                                farmer.inGameName to farmer.discordUser.discordId
                            },
                            role = role,
                            progressCallBack = progressBar::increment
                        )
                    }
                }

                progressBar.stop()
            }

            rollCallResponse(contract, coops).forEach { block ->
                event.reply(block)
            }
        }
    }
}
