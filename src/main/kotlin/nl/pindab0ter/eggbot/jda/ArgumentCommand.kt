package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Parameter
import com.martiansoftware.jsap.Switch
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot.log
import nl.pindab0ter.eggbot.utilities.parameters

abstract class ArgumentCommand : Command() {

    private val log = KotlinLogging.logger {}
    private val parser: JSAP = JSAP()
    private lateinit var helpMessage: String
    protected lateinit var parameters: List<Parameter>

    private val helpFlag = Switch("help")
        .setShortFlag('h')
        .setLongFlag("help")
        .setHelp("Display a help message.")

    /** Child MUST call this method **/
    protected fun init() {
        parameters.forEach { parser.registerParameter(it) }
        arguments = parser.usage
        helpMessage = generateHelp()
        parser.registerParameter(helpFlag)
    }

    private fun generateHelp() = StringBuilder().apply {
        append("ℹ️ **`${Config.prefix}$name ")
        append(parameters.joinToString(" ") { parameter -> parameter.syntax })
        appendln("`**")
        when (aliases.size) {
            0 -> Unit
            1 -> appendln(aliases.joinToString(prefix = "    Alias: ") { "`${Config.prefix}$it`" })
            else -> appendln(aliases.joinToString(prefix = "    Aliases: ") { "`${Config.prefix}$it`" })
        }
        appendln("__**Description:**__")
        appendln("    $help")
        appendln("__**Arguments:**__")
        parameters.forEach { parameter ->
            appendln("`${parameter.syntax}`")
            appendln("    ${parameter.help}")
        }
    }.toString()


    final override fun execute(event: CommandEvent) {
        val result: JSAPResult = parser.parse(event.args)

        when {
            result.getBoolean("help", false) -> event.reply(helpMessage)
            !result.success() -> {
                val errorMessage = result.errorMessageIterator.next().toString().replace("'", "`")
                event.replyWarning("$errorMessage Type `${Config.prefix}$name --\u200Dhelp` or `${Config.prefix}$name -\u200Dh` for help with this command.")
            }
            else -> execute(event, result)
        }
    }

    abstract fun execute(event: CommandEvent, arguments: JSAPResult)

}
