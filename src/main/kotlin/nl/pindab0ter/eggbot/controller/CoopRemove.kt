package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
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

object CoopRemove : EggBotCommand() {

    private val log = KotlinLogging.logger { }

    init {
        category = AdminCategory
        name = "coop-remove"
        help = "Unregisters a co-op so it no longer shows up in the co-ops listing and delete it's associated role."
        parameters = listOf(
            contractIdOption,
            coopIdOption
        )
        adminRequired = true
        botPermissions = arrayOf(Permission.MANAGE_ROLES)
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val contractId: String = parameters.getString(CONTRACT_ID)
        val coopId: String = parameters.getString(COOP_ID)
        val coop = transaction { Coop.find { (Coops.name eq coopId) and (Coops.contract eq contractId) }.firstOrNull() }
            ?: "No co-op registered with that `contract id` and `co-op id`.".let {
                event.replyWarning(it)
                log.debug { it }
                return
            }

        val role = transaction { coop.roleId?.let { guild.getRoleById(it) } }

        if (role != null) role.delete().queue({
            transaction { coop.delete() }
            "Co-op and role successfully removed.".let {
                event.replySuccess(it)
                log.debug { it }
            }
        }, { exception ->
            log.warn { exception.localizedMessage }
            event.replyWarning("Failed to remove Discord role (${exception.localizedMessage})")
        }) else {
            transaction { coop.delete() }
            "Co-op successfully removed.".let {
                event.replySuccess(it)
                log.debug { it }
            }
        }
    }
}
