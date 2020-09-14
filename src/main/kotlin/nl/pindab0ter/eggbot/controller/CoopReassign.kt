package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.controller.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.helpers.CONTRACT_ID
import nl.pindab0ter.eggbot.helpers.COOP_ID
import nl.pindab0ter.eggbot.helpers.contractIdOption
import nl.pindab0ter.eggbot.helpers.coopIdOption
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.database.Coop
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("FoldInitializerAndIfToElvis")
object CoopReassign : EggBotCommand() {

    private val allowedCharacters = Regex("""^[a-zA-Z0-9\-]+$""")
    private const val NEW_NAME = "new name"

    init {
        name = "coop-reassign"
        help = "Reassigns a co-op and renames it's associated role if available, does NOT rename the co-op in-game. Only letters, digits and dashes allowed."
        category = AdminCategory
        parameters = listOf(
            contractIdOption,
            coopIdOption,
            UnflaggedOption(NEW_NAME)
                .setRequired(REQUIRED)
                .setHelp("The name of the co-op. No checks are performed, this co-op might not (yet) exist in-game, so make sure it's spelled correctly.")
        )
        adminRequired = true
        botPermissions = arrayOf(MANAGE_ROLES)
        sendTyping = true
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val coopId: String = parameters.getString(COOP_ID)
        val newName: String = parameters.getString(NEW_NAME)
        val coop = transaction {
            Coop.find { (Coops.name eq coopId) and (Coops.contractId eq contractId) }.firstOrNull()
        } ?: return event.replyAndLogWarning("No co-op registered with that `contract id` and `co-op id`.")

        if (!allowedCharacters.matches(newName)) return event.replyAndLogWarning("Only letters, digits and dashes are allowed.")

        val role = transaction { coop.roleId?.let { guild.getRoleById(it) } }

        if (role != null) role.manager.setName(newName).queue({
            transaction { coop.name = newName }
            event.replyAndLogSuccess("Co-op successfully reassigned from `$coopId` to `$newName` and role renamed accordingly.")
        }, { exception ->
            event.replyAndLogWarning("Failed to rename Discord role (${exception.localizedMessage})", LogType.Warning)
        }) else {
            transaction { coop.name = newName }
            event.replyAndLogSuccess("Co-op successfully reassigned from `$coopId` to `$newName`.")
        }
    }
}
