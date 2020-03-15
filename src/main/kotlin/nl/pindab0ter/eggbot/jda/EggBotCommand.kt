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
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.EggBot.adminRole
import nl.pindab0ter.eggbot.EggBot.jdaClient
import nl.pindab0ter.eggbot.commands.Register
import nl.pindab0ter.eggbot.utilities.isAdmin
import nl.pindab0ter.eggbot.utilities.isRegistered

abstract class EggBotCommand : Command() {

    private val log: KLogger = KotlinLogging.logger {}
    private val parser: JSAP = JSAP()
    private lateinit var commandHelp: String
    protected var parameters: List<Parameter>? = null
    var adminRequired: Boolean = false
    var registrationRequired: Boolean = true
    var dmOnly: Boolean = false
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
        arguments = parser.usage.replace("[<", "[").replace(">]", "]")
        commandHelp = generateCommandHelp()
        parser.registerParameter(helpFlag)
    }

    private fun generateCommandHelp() = StringBuilder().apply {
        append("ℹ️ **`${Config.prefix}$name")
        if (parameters != null) {
            append(" ")
            append(parameters?.joinToString(" ") { parameter ->
                parameter.syntax.replace("[<", "[").replace(">]", "]")
            })
        }
        appendln("`**")
        when (aliases.size) {
            0 -> Unit
            1 -> appendln(aliases.joinToString(prefix = "    Alias: ") { "`${Config.prefix}$it`" })
            else -> appendln(aliases.joinToString(prefix = "    Aliases: ") { "`${Config.prefix}$it`" })
        }
        if (adminRequired) appendln("Admin only.")
        appendln("__**Description**__")
        appendln("    $help")
        if (parameters != null) {
            appendln("__**Arguments**__")
            parameters?.forEach { parameter ->
                appendln("`${parameter.syntax.replace("[<", "[").replace(">]", "]")}`")
                appendln("    ${parameter.help}")
            }
        }
    }.toString()

    final override fun execute(event: CommandEvent) {
        if (sendTyping) event.channel.sendTyping().queue()
        val result: JSAPResult = parser.parse(event.args)

        when {
            event.author.isRegistered < registrationRequired -> "You are not yet registered. Please register using `${commandClient.textualPrefix}${Register.name}`.".let {
                log.debug { it }
                event.replyError(it)
            }
            event.author.isAdmin < adminRequired -> "You must have a role called `${adminRole.name}` or higher to use that!".let {
                log.debug { it }
                event.replyError(it)
            }
            dmOnly && event.channelType != PRIVATE -> "This command can only be used in DMs. Please try again by DMing ${jdaClient.selfUser.asMention}.".let {
                log.debug { it }
                event.replyError(it)
            }
            result.getBoolean("help") -> event.reply(commandHelp)
            !result.success() -> {
                val errorMessage = result.errorMessageIterator.next().toString().replace("'", "`")
                "$errorMessage The syntax for this command is: `${Config.prefix}$name ${arguments}`.\nFor more information on this command, type `${Config.prefix}$name --\u200Dhelp` or `${Config.prefix}$name -\u200Dh`.".let {
                    log.debug { it }
                    event.replyWarning(it)
                }
            }
            else -> execute(event, result)
        }
    }

    abstract fun execute(event: CommandEvent, parameters: JSAPResult)

}