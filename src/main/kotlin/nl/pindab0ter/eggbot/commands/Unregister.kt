package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.utilities.*
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction

object Unregister : Command() {
    private val log = KotlinLogging.logger { }

    init {
        name = "unregister"
        arguments = "<discord id>"
        help = "Unregister a user. This cannot be undone!"
        category = AdminCategory
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 1,
            maxArguments = 2
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val id = event.arguments.first()
        val member = try {
            EggBot.guild.getMemberById(id)
        } catch (exception: NumberFormatException) {
            null
        } ?: "No user with Discord ID `$id` found.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        transaction {
            val discordUser = DiscordUser.findById(member.user.id)
                ?: "`${member.user.name}` ${if (member.nickname != null) "(a.k.a. `${member.nickname}`) " else ""} is not registered.".let {
                    event.replyWarning(it)
                    log.debug { it }
                    return@transaction
                }

            StringBuilder()
                .append("`${member.user.name}` ")
                .append(if (member.nickname != null) "(a.k.a. `${member.nickname}`) " else "")
                .append("has been unregistered, along with the ")
                .append(
                    when {
                        discordUser.farmers.count() > 1 ->
                            "farmers `${discordUser.farmers.joinToString(", ") { it.inGameName }}`"
                        else ->
                            "farmer `${discordUser.farmers.first().inGameName}`"
                    }
                )
                .toString().let {
                    event.replyWarning(it)
                    log.debug { it }
                }

            discordUser.delete()
        }
    }
}
