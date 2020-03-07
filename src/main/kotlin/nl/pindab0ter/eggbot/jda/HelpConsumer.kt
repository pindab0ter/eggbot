package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
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
                append(if (command.arguments.isNullOrBlank()) "`" else " ${command.arguments}`")
                appendln()
                append("    ${command.help}")
            }
        }

        appendln(
            """
            ℹ️ **Help:**
            __**Description:**__
            ${EggBot.jdaClient.selfUser.name} helps the members of ${EggBot.guild.name} tackle Egg, Inc.'s multiplayer contracts by offering a range of commands that help make people's lives a little bit easier. It won't do your dishes, but it will tell you if you're going to make it to that final goal.

            __**Instructions:**__
            Arguments in angled brackets (`<>`) are required, arguments in square brackets (`[]`) are optional.
            When it says `[-h|--help]` that means either `-h` or `--help` will work.
            Type the arguments without the brackets. For example: `${Config.prefix}${CoopInfo.name} --compact contract coopname`
            For more information on a specific command, type `${Config.prefix}command -h` or `${Config.prefix}command --help`""".trimIndent()
        )

        commandClient.commands.let { commands ->
            if (commands.any { it.category != null }) {
                commands.groupBy { it.category }.toList()
                    .filterNot { (_, commands) -> commands.all { command -> command.isHidden } }
                    .forEachIndexed { i, (category, commands) ->
                        append(if (i == 0) "\n" else "\n\n")
                        append("\u200D__${if (category != null) category.name else "No category"}:__")
                        append(commands)
                    }
            } else append(commands)
        }
    }.toString().splitMessage(prefix = "Continued…\n", separator = '\u200D').forEach { section ->
        event.reply(section)
    }
}
