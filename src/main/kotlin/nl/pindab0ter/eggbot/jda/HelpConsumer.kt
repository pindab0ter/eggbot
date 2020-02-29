package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import java.util.function.Consumer


object HelpConsumer : Consumer<CommandEvent> {

    override fun accept(event: CommandEvent) = StringBuilder().apply {

        fun append(commands: List<Command>) = commands.forEach { command ->
            if (!command.isHidden && (!command.isOwnerCommand || event.isOwner)) {
                append("\n`")
                append(commandClient.textualPrefix)
                append(if (commandClient.prefix == null) " " else "")
                append(command.name)
                append(if (command.arguments == null) "`" else " ${command.arguments}`")
                append(" - ${command.help}")
            }
        }

        appendln("`<>` = required argument, `[]` = optional argument\nType the arguments without the brackets.")

        commandClient.commands.let { commands ->
            if (commands.any { it.category != null }) {
                commands.groupBy { it.category }.toList()
                    .filterNot { (_, commands) -> commands.all { command -> command.isHidden } }
                    .forEachIndexed { i, (category, commands) ->
                        append(if (i == 0) "\n" else "\n\n")
                        append("  __${if (category != null) category.name else "No category"}__:")
                        append(commands)
                    }
            } else append(commands)
        }
    }.toString().let { helpMessage ->
        event.reply(helpMessage)
    }
}
