package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
import nl.pindab0ter.eggbot.controller.categories.ContractsCategory
import nl.pindab0ter.eggbot.helpers.OPT_IN
import nl.pindab0ter.eggbot.helpers.OPT_OUT
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction

object Leader : EggBotCommand() {

    init {
        category = ContractsCategory
        name = "leader"
        help = "Check your current co-op leader eligibility, opt out or opt back in again."
        parameters = listOf(
            Switch(OPT_IN)
                .setLongFlag("opt-in")
                .setHelp("Opt back in to being eligible to be chosen as co-op leader."),
            Switch(OPT_OUT)
                .setLongFlag("opt-out")
                .setHelp("Opt out of being eligible to be chosen as co-op leader."),
        )
        sendTyping = false
        init()
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(
        event: CommandEvent,
        parameters: JSAPResult,
    ) {
        val discordUser = transaction { DiscordUser.findById(event.author.id)!! }
        val optIn: Boolean = parameters.getBoolean(OPT_IN)
        val optOut: Boolean = parameters.getBoolean(OPT_OUT)

        if (optIn && optOut) event.replyAndLogWarning("You cannot opt in and out at the same time.")

        when {
            optIn && !discordUser.optedOutOfCoopLead -> event.reply("You are already eligible to be chosen as a co-op leader.")
            optOut && discordUser.optedOutOfCoopLead -> event.reply("You are already opted out of being eligible to be chosen as a co-op leader.")
            optIn && discordUser.optedOutOfCoopLead -> {
                transaction { discordUser.optInToCoopLead() }
                event.replySuccess("You have successfully opted back in to being eligible to be chosen as co-op leader.")
            }
            optOut && !discordUser.optedOutOfCoopLead -> {
                transaction { discordUser.optOutOfCoopLead() }
                event.replySuccess("You have successfully opted out of being eligible to be chosen as co-op leader.")
            }
            discordUser.optedOutOfCoopLead -> event.reply("You have opted out of being eligible to be chosen as co-op leader.")
            !discordUser.optedOutOfCoopLead -> event.reply("You are eligible to be chosen as co-op leader.")
        }
    }
}
