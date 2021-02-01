package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Parameter
import com.martiansoftware.jsap.Switch
import net.dv8tion.jda.api.entities.ChannelType.PRIVATE
import nl.pindab0ter.eggbot.EggBot.adminRole
import nl.pindab0ter.eggbot.EggBot.jdaClient
import nl.pindab0ter.eggbot.controller.Register
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.Typography.zwj
import nl.pindab0ter.eggbot.model.Config
import org.apache.logging.log4j.kotlin.Logging

abstract class EggBotCommand : Command(), Logging {

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
                event.replyAndLogError("You are not yet registered. Please register using `${commandClient.textualPrefix}${Register.name}`.")
            event.author.isAdmin < adminRequired ->
                event.replyAndLogError("You must have a role called `${adminRole.name}` or higher to use that!")
            dmOnly && event.channelType != PRIVATE ->
                event.replyAndLogError("This command can only be used in DMs. Please try again by DMing ${jdaClient.selfUser.asMention}.")
            result.getBoolean(COMPACT, false) && result.getBoolean(EXTENDED, false) ->
                event.replyAndLogError("Cannot use both `${compactSwitch.cleanSyntax}` and `${extendedSwitch.cleanSyntax}` at the same time.")
            result.getBoolean("help") -> event.reply(commandHelp)
            !result.success() ->
                event.replyAndLogError("""
                    ${result.errorMessageIterator.next().toString().replace("'", "`")}
                    The syntax for this command is: `${Config.prefix}$name ${arguments}`.
                    For more information on this command, type `${Config.prefix}$name --${zwj}help` or `${Config.prefix}$name -${zwj}h`.
                    """.trimIndent()
                )
            else -> execute(event, result)
        }
    }

    enum class ReplyType { Default, Warning, Error, Success }
    enum class LogType { Trace, Debug, Info, Warning, Error }

    fun CommandEvent.replyAndLog(
        message: String,
        replyType: ReplyType = ReplyType.Default,
        logType: LogType = LogType.Trace,
    ) {
        when (replyType) {
            ReplyType.Default -> reply(message)
            ReplyType.Warning -> replyWarning(message)
            ReplyType.Error -> replyError(message)
            ReplyType.Success -> replySuccess(message)
        }

        log(message, logType)
    }

    fun CommandEvent.replyInDmAndLog(
        message: String,
        logType: LogType = LogType.Trace,
    ) {
        replyInDm(message)
        log(message, logType)
    }

    private fun log(message: String, logType: LogType) {
        val singleLineMessage = message.replace("""\s+""".toRegex(RegexOption.DOT_MATCHES_ALL), " ")
        when (logType) {
            LogType.Trace -> logger.trace { singleLineMessage }
            LogType.Debug -> logger.debug { singleLineMessage }
            LogType.Info -> logger.info { singleLineMessage }
            LogType.Warning -> logger.warn { singleLineMessage }
            LogType.Error -> logger.error { singleLineMessage }
        }
    }

    fun CommandEvent.replyAndLogSuccess(message: String): Unit = replyAndLog(message, ReplyType.Success)
    fun CommandEvent.replyAndLogWarning(
        message: String,
        logType: LogType = LogType.Trace,
    ): Unit = replyAndLog(message, ReplyType.Warning, logType)

    fun CommandEvent.replyAndLogError(
        message: String,
        logType: LogType = LogType.Trace,
    ): Unit = replyAndLog(message, ReplyType.Error, logType)

    abstract fun execute(event: CommandEvent, parameters: JSAPResult)

}