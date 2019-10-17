package nl.pindab0ter.eggbot.utilities

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.ChannelType.TEXT
import net.dv8tion.jda.core.entities.User
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.Register
import nl.pindab0ter.eggbot.database.DiscordUser
import nl.pindab0ter.eggbot.jda.commandClient
import org.jetbrains.exposed.sql.transactions.transaction


sealed class PrerequisitesCheckResult {
    class Success : PrerequisitesCheckResult()
    class Failure(val message: String) : PrerequisitesCheckResult()
}

fun Command.checkPrerequisites(
    commandEvent: CommandEvent,
    registrationRequired: Boolean = true,
    adminRequired: Boolean = false,
    channelType: ChannelType? = null,
    minArguments: Int = 0,
    maxArguments: Int = Int.MAX_VALUE
): PrerequisitesCheckResult = when {
    commandEvent.author.isRegistered < registrationRequired ->
        PrerequisitesCheckResult.Failure("You are not yet registered. Please register using `${commandClient.textualPrefix}${Register.name}`.")
    commandEvent.author.isAdmin < adminRequired ->
        PrerequisitesCheckResult.Failure("You must have at least a role called `${Config.adminRole}` to use that!")
    channelType == TEXT && channelType != commandEvent.channelType ->
        PrerequisitesCheckResult.Failure("This command cannot be used in DMs. Please try again in a public channel.")
    channelType == PRIVATE && channelType != commandEvent.channelType ->
        PrerequisitesCheckResult.Failure("This command can only be used in DMs. Please try again by DMing ${EggBot.jdaClient.selfUser.asTag}.")
    commandEvent.arguments.size < minArguments ->
        PrerequisitesCheckResult.Failure("Missing argument(s). Use `${commandClient.textualPrefix}$name $arguments` without the brackets.")
    commandEvent.arguments.size > maxArguments ->
        PrerequisitesCheckResult.Failure("Too many arguments. Use `${commandClient.textualPrefix}$name $arguments` without the brackets.")
    else -> PrerequisitesCheckResult.Success()
}

val User.isRegistered: Boolean
    get() = transaction {
        DiscordUser.findById(id)?.farmers?.toList()?.sortedBy { it.inGameName }?.isNotEmpty() == true
    }

val User.isAdmin: Boolean
    get() = mutualGuilds.any { guild ->
        guild.getMember(this).let { author ->
            author.isOwner || author.user.id == Config.ownerId || author.roles.any { memberRole ->
                guild.getRolesByName(Config.adminRole, true).any { guildRole ->
                    memberRole.position >= guildRole.position
                }
            }
        }
    }

val CommandEvent.arguments: List<String> get() = if (args.isBlank()) emptyList() else args.split(Regex("""\s"""))