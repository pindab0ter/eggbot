package nl.pindab0ter.eggbot.controller

import com.auxbrain.ei.FarmType.HOME
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.helpers.COMPACT
import nl.pindab0ter.eggbot.helpers.compactSwitch
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.view.artifactCheckResponse
import org.jetbrains.exposed.sql.transactions.transaction

object ArtifactCheck : EggBotCommand() {

    init {
        category = FarmersCategory
        name = "artifact-check"
        aliases = arrayOf(
            "artifacts-check",
            "ac",
        )
        parameters = listOf(compactSwitch)
        help = "Show the artifact bonuses of all your active farms."
        sendTyping = true
        init()
    }

    override fun execute(
        event: CommandEvent,
        parameters: JSAPResult,
    ): Unit = transaction {
        DiscordUser.findById(event.author.id)?.farmers?.toList()?.sortedBy { farmer ->
            farmer.inGameName
        }?.mapNotNull { farmer ->
            AuxBrain.getFarmerBackup(farmer.inGameId)
        }?.forEach { backup ->
            val farm = backup.farms.firstOrNull { farm ->
                farm.farmType == HOME
            } ?: return@forEach
            artifactCheckResponse(farm, backup, parameters.getBoolean(COMPACT, false)).forEach(event::reply)
        }
    }
}
