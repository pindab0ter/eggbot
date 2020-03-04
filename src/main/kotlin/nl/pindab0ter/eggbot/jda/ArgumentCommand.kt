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

    private val log = KotlinLogging.logger {}
    private lateinit var parser: JSAP
    private lateinit var helpMessage: String
    protected lateinit var parameters: List<Parameter>
    lateinit var usage: String
        private set

    /** Child MUST call this method **/
    protected fun init() {
        parser = JSAP().apply {
            parameters.forEach { registerParameter(it) }
        }
        usage = parser.usage
        helpMessage = """```
            |Usage: ${Config.prefix}$name ${parser.usage}
            |
            |$help
            |
            |${parser.help}
            |```""".trimMargin()
        parser.registerParameter(
            Switch("help")
                .setShortFlag('h')
                .setLongFlag("help")
                .setHelp("Display a help message.")
        )
    }

    // TODO: Write custom help formatter

    final override fun execute(event: CommandEvent) {
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
