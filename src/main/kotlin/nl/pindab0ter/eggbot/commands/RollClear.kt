package nl.pindab0ter.eggbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.utilities.PrerequisitesCheckResult
import nl.pindab0ter.eggbot.utilities.arguments
import nl.pindab0ter.eggbot.utilities.checkPrerequisites
import org.jetbrains.exposed.sql.transactions.transaction

object RollClear : Command() {

    private val log = KotlinLogging.logger { }

    init {
        name = "roll-clear"
        arguments = "<contract id>"
        aliases = arrayOf("role-clear")
        help = "Delete all the roles associated with the specified contract id."
        category = AdminCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 1,
            maxArguments = 1
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contract = event.arguments.first()

        val coops: List<Coop> = transaction {
            Coop.find { Coops.contract eq contract }.toList()
        }

        if (coops.all { it.roleId == null }) "No roles found for `${contract}`".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val roles = coops.mapNotNull { coop ->
            coop.roleId?.let { EggBot.guild.getRoleById(it) }
        }

        val roleNames = roles.map { role -> role.name }

        if (roles.isEmpty()) "No roles found for `${contract}`".let {
            event.replyWarning(it)
            log.warn { it }
            return
        }

        val message = event.channel.sendMessage("Deleting roles…").complete()

        val roleDeletions = roles.map { role ->
            event.channel.sendTyping().queue()
            role.delete().submit().also { it.join() }
        }

        if (roleDeletions.any { it.isCompletedExceptionally }) "Something went wrong. Please contact ${EggBot.jdaClient.getUserById(
            Config.ownerId
        )?.asMention ?: "the bot maintainer"}".let {
            event.replyWarning(it)
            log.error { it }
            return
        }

        StringBuilder("The following roles associated with `$contract` have been deleted:").apply {
            appendln("```")
            roleNames.forEach { append("$it\n") }
            appendln("```")
        }.toString().let {
            message.editMessage(it).complete()
        }
    }
}
