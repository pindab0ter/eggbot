package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import nl.pindab0ter.eggbot.controller.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Farmers
import nl.pindab0ter.eggbot.helpers.table
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

object Unmigrated : EggBotCommand() {

    private const val EGG_INC_ID = "egg inc id"

    init {
        category = AdminCategory
        name = "unmigrated"
        help = "List all players that have not yet migrated to the new Egg, Inc. ID."
        hidden = true
        registrationRequired = true
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) = transaction {
        val unmigratedFarmers = Farmer.find { Farmers.id notLike "EI%" }.sortedBy { it.discordUser.discordName }

        table {
            title = "Unmigrated farmers"

            column {
                header = "Discord user"
                rightPadding = 1
                cells = unmigratedFarmers.map(Farmer::discordUser).map(DiscordUser::discordTag)
            }

            column {
                header = "Farmer"
                alignment = RIGHT
                cells = unmigratedFarmers.map(Farmer::inGameName)
            }

        }.forEach(event::reply)
    }
}
