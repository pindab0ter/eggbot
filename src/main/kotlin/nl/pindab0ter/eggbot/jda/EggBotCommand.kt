package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Parameter
import com.martiansoftware.jsap.Switch
import mu.KLogger
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.ChannelType.PRIVATE
import nl.pindab0ter.eggbot.EggBot.adminRole
import nl.pindab0ter.eggbot.EggBot.jdaClient
import nl.pindab0ter.eggbot.controller.Register
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.model.Config

abstract class EggBotCommand : Command() {

    private val log: KLogger = KotlinLogging.logger {}
    private val parser: JSAP = JSAP()
    private lateinit var commandHelp: String
    protected var parameters: List<Parameter>? = null
    var adminRequired: Boolean = false
    var registrationRequired: Boolean = true
    private var dmOnly: Boolean = false
    var sendTyping: Boolean = false

    init {
        guildOnly = false
    }

    private val helpFlag = Switch("help")
        .setShortFlag('h')
        .setLongFlag("help")
        .setHelp("Display a help message.")

    /** Child MUST call this method **/
    protected fun init() {
        parameters?.forEach { parser.registerParameter(it) }
        arguments = parser.cleanUsage
        commandHelp = generateCommandHelp()
        parser.registerParameter(helpFlag)
    }

    private fun clean(syntax: String): String = syntax.replace(
        Regex("""\(([-|\w]+)\)|(?:(\[)(?:<)([-|\w]+?)(?:>)(]))""")
    ) { matchResult ->
        matchResult.groupValues.drop(1).joinToString("")
    }

    private val JSAP.cleanUsage
        get() = clean(usage)
    private val Parameter.cleanSyntax
        get() = clean(syntax)

    private fun generateCommandHelp() = buildString {
        append("ℹ️ **`${Config.prefix}$name")
        if (parameters != null) {
            append(" ")
            append(parameters?.joinToString(" ") { parameter ->
                parameter.cleanSyntax
            })
        }
        appendLine("`**")
        when (aliases.size) {
            0 -> Unit
            1 -> appendLine(aliases.joinToString(prefix = "    Alias: ") { "`${Config.prefix}$it`" })
            else -> appendLine(aliases.joinToString(prefix = "    Aliases: ") { "`${Config.prefix}$it`" })
        }
        if (adminRequired) appendLine("Admin only.")
        appendLine("__**Description**__")
        appendLine("    $help")
        if (parameters != null) {
            appendLine("__**Arguments**__")
            parameters?.forEach { parameter ->
                appendLine("`${parameter.cleanSyntax}`")
                appendLine("    ${parameter.help}")
            }
        }
    }

    final override fun execute(event: CommandEvent) {
        if (sendTyping) event.channel.sendTyping().queue()
        val cleanedArgs = event.args.replace(Regex("""[„“”]"""), "\"")
        val result: JSAPResult = parser.parse(cleanedArgs)

        when {
            event.author.isRegistered < registrationRequired ->
                "You are not yet registered. Please register using `${commandClient.textualPrefix}${Register.name}`.".let {
                    log.debug { it }
                    event.replyError(it)
                }
            event.author.isAdmin < adminRequired ->
                "You must have a role called `${adminRole.name}` or higher to use that!".let {
                    log.debug { it }
                    event.replyError(it)
                }
            dmOnly && event.channelType != PRIVATE ->
                "This command can only be used in DMs. Please try again by DMing ${jdaClient.selfUser.asMention}.".let {
                    log.debug { it }
                    event.replyError(it)
                }
            result.getBoolean(COMPACT, false) && result.getBoolean(EXTENDED, false) ->
                "Cannot use both `${compactSwitch.cleanSyntax}` and `${extendedSwitch.cleanSyntax}` at the same time.".let {
                    log.debug { it }
                    event.replyWarning(it)
                }
            result.getBoolean("help") -> event.reply(commandHelp)
            !result.success() -> {
                val errorMessage = result.errorMessageIterator.next().toString().replace("'", "`")
                """
                $errorMessage
                The syntax for this command is: `${Config.prefix}$name ${arguments}`.
                For more information on this command, type `${Config.prefix}$name --‍help` or `${Config.prefix}$name -‍h`.
                """.trimIndent().let {
                    log.debug { it }
                    event.replyWarning(it)
                }
            }
            else -> execute(event, result)
        }
    }

    abstract fun execute(event: CommandEvent, parameters: JSAPResult)

}