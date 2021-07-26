package nl.pindab0ter.eggbot.helpers

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.model.Config

val guild: Guild? get() = runBlocking { getKoin().get<Kord>().getGuild(Config.guild) }

@OptIn(KordPreview::class)
suspend fun <T : Arguments> SlashCommandContext<T>.publicFollowUp(messages: List<String>) {
    publicFollowUp {
        content = messages.first()
    }
    messages.tail().forEach { message ->
        channel.createMessage(message)
    }
}

suspend fun CommandContext.emoteMention(snowflake: Snowflake?): String? = snowflake?.let {
    getGuild()?.asGuildOrNull()?.getEmojiOrNull(snowflake)?.mention
}
