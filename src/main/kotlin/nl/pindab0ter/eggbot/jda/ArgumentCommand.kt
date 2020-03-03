package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Parameter
import com.martiansoftware.jsap.Switch
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config

abstract class ArgumentCommand : Command() {

    val log = KotlinLogging.logger {}
    abstract val parameters: List<Parameter>
    private val parser: JSAP by lazy {
        JSAP().apply {
            parameters.forEach { registerParameter(it) }
        }
    }
    private val helpMessage: String by lazy {
        """```
            |Usage: ${Config.prefix}$name ${parser.usage}
            |
            |$help
            |
            |${parser.help}
            |```""".trimMargin()
    }

    final override fun execute(event: CommandEvent) {
        if (!parser.idMap.idExists("help")) {
            helpMessage
            parser.registerParameter(
                Switch("help")
                    .setShortFlag('h')
                    .setLongFlag("help")
                    .setHelp("shows this message")
            )
        }

        val result: JSAPResult = parser.parse(event.args)

        when {
            result.getBoolean("help", false) -> event.reply(helpMessage)
            !result.success() -> event.replyWarning(
                "${result.errorMessageIterator.next().toString().replace(
                    "'",
                    "`"
                )}\n$helpMessage"
            )
            else -> execute(event, result)
        }
    }

    abstract fun execute(event: CommandEvent, arguments: JSAPResult)
}
