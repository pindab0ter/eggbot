package nl.pindab0ter.eggbot.controller

import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP.INTEGER_PARSER
import com.martiansoftware.jsap.JSAP.NOT_REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.UnflaggedOption
import mu.KotlinLogging
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.controller.categories.FarmersCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.jda.EggBotCommand
import nl.pindab0ter.eggbot.helpers.asMonthAndDay
import nl.pindab0ter.eggbot.helpers.getIntOrNull
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object Inactive : EggBotCommand() {

    private val log = KotlinLogging.logger { }
    private const val DAYS = "days"

    init {
        category = FarmersCategory
        name = "inactive"
        help = "Set yourself as inactive for `[days]` amount of days or check whether you're inactive. " +
                "You can set yourself as active again before that moment by calling ${Config.prefix}${Active.name}" +
                "Being inactive means your EB will not be counted in the co-op roll call, " +
                "but you will still be assigned to a team. If you don't provide `[days]` the command will show you " +
                "if you're currently set as inactive and if you are for how long."
        parameters = listOf(
            UnflaggedOption(DAYS)
                .setRequired(NOT_REQUIRED)
                .setStringParser(INTEGER_PARSER)
                .setHelp("The amount of days you want to be set as inactive for.")
        )
        sendTyping = false
        init()
    }

    override fun execute(event: CommandEvent, parameters: JSAPResult) {
        val discordUser = transaction { DiscordUser.findById(event.author.id)!! }

        when (val days = parameters.getIntOrNull(DAYS)) {
            // If no arguments are given
            null -> when {
                discordUser.isActive -> {
                    event.reply("You are not inactive.")
                    return
                }
                else -> {
                    event.reply("You are set to be inactive until **${discordUser.inactiveUntil?.asMonthAndDay()}**.")
                    return
                }
            }
            in Int.MIN_VALUE..0 -> "The number of days must be positive.".let {
                event.replyWarning(it)
                log.debug { it }
                return
            }
            else -> DateTime.now().plusDays(days.coerceAtMost(356)).let { inactiveUntil ->
                log.info { "User ${discordUser.discordTag} will be inactive for $days days" }
                transaction { discordUser.inactiveUntil = inactiveUntil }
                event.replySuccess("You will be inactive until **${inactiveUntil.asMonthAndDay()}** or until you use `${event.client.textualPrefix}${Active.name}`.")
            }
        }
    }
}
