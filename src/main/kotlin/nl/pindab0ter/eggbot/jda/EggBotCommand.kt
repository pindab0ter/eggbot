package nl.pindab0ter.eggbot.jda

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Parameter
import com.martiansoftware.jsap.Switch
import net.dv8tion.jda.api.entities.ChannelType.PRIVATE
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.Register
import nl.pindab0ter.eggbot.utilities.isAdmin
import nl.pindab0ter.eggbot.utilities.isRegistered

abstract class EggBotCommand : Command() {

    private val parser: JSAP = JSAP()
    private lateinit var helpMessage: String
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
        arguments = parser.usage
        helpMessage = generateHelp()
        parser.registerParameter(helpFlag)
    }

    private fun generateHelp() = StringBuilder().apply {
        append("ℹ️ **`${Config.prefix}$name ")
        if (parameters != null) {
            append(" ")
            append(parameters?.joinToString(" ") { parameter -> parameter.syntax })
        }
        appendln("`**")
        when (aliases.size) {
            0 -> Unit
            1 -> appendln(aliases.joinToString(prefix = "    Alias: ") { "`${Config.prefix}$it`" })
            else -> appendln(aliases.joinToString(prefix = "    Aliases: ") { "`${Config.prefix}$it`" })
        }
        if (adminRequired) appendln("Admin only.")
        appendln("__**Description:**__")
        appendln("    $help")
        if (parameters != null) {
            appendln("__**Arguments:**__")
            parameters?.forEach { parameter ->
                appendln("`${parameter.syntax}`")
                appendln("    ${parameter.help}")
            }
        }
    }.toString()

    final override fun execute(event: CommandEvent) {
        if (sendTyping) event.channel.sendTyping().queue()
        val result: JSAPResult = parser.parse(event.args)

        when {
            event.author.isRegistered < registrationRequired ->
                event.replyError("You are not yet registered. Please register using `${commandClient.textualPrefix}${Register.name}`.")
            event.author.isAdmin < adminRequired ->
                event.replyError("You must have a role called `${EggBot.adminRole.name}` or higher to use that!")
            dmOnly && event.channelType != PRIVATE ->
                event.replyError("This command can only be used in DMs. Please try again by DMing ${EggBot.jdaClient.selfUser.asMention}.")
            result.getBoolean("help", false) -> event.reply(helpMessage)
            !result.success() -> {
                val errorMessage = result.errorMessageIterator.next().toString().replace("'", "`")
                event.replyWarning("$errorMessage Type `${Config.prefix}$name --\u200Dhelp` or `${Config.prefix}$name -\u200Dh` for help with this command.")
            }
            else -> execute(event, result)
        }
    }

    abstract fun execute(event: CommandEvent, parameters: JSAPResult)

}