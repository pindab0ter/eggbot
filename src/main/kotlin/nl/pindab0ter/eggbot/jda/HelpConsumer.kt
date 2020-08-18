package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot.guild
import nl.pindab0ter.eggbot.EggBot.jdaClient
import nl.pindab0ter.eggbot.commands.CoopInfo
import nl.pindab0ter.eggbot.utilities.splitMessage
import java.util.function.Consumer


object HelpConsumer : Consumer<CommandEvent> {

    override fun accept(event: CommandEvent) = StringBuilder().apply {

        fun append(commands: List<Command>) = commands.forEach { command ->
            if (!command.isHidden && (!command.isOwnerCommand || event.isOwner)) {
                append("\n`")
                append(Config.prefix)
                append(command.name)
                append(if (command.arguments == null) "`" else " ${command.arguments}`")
                appendLine()
                append("    ${command.help}")
            }
        }

        appendLine(
            """
            ℹ️ **Help:**
            __**Description**__
            ${jdaClient.selfUser.name} helps the members of ${guild.name} tackle Egg, Inc.'s multiplayer contracts by offering a range of commands that help make people's lives a little bit easier. It won't do your dishes, but it will tell you if you're going to make it to that final goal.

            __**Instructions**__
            Type the command name, followed by a space and then it's arguments if there are any.
            Arguments in angled brackets (`<>`) are required, arguments in square brackets (`[]`) are optional.
            Type the arguments without the brackets. For example: `${Config.prefix}${CoopInfo.name} --compact contract coopname`
            Arguments starting with one or two dashes (`[-h|--help]`) are called "flags" that allow you to specify options.
            A flag with one dash is the short version of a flag with two dashes and using either `-h` or `--help` will work.
            It doesn't matter if you place flags in front or behind unflagged arguments.
            You can use short and long commands and arguments interchangeably.
            When an argument contains spaces, surround it with quotation marks (e.g.: `!whois "name with spaces"`) or it will assume each word is an argument.
            For more information on a specific command, type `${Config.prefix}command -h` or `${Config.prefix}command --help`""".trimIndent()
        )

        commandClient.commands.let { commands ->
            if (commands.any { it.category != null }) {
                commands.groupBy { it.category }.toList()
                    .filterNot { (_, commands) -> commands.all { command -> command.isHidden } }
                    .forEachIndexed { i, (category, commands) ->
                        append(if (i == 0) "\n" else "\n\n")
                        append("\u200B__${if (category != null) category.name else "No category"}:__")
                        append(commands)
                    }
            } else append(commands)
        }
    }.toString().splitMessage(prefix = "Continued…\n", separator = '\u200B').forEach { section ->
        event.reply(section)
    }
}
