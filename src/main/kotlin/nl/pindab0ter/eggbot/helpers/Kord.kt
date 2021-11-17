package nl.pindab0ter.eggbot.helpers

import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.model.Config

val configuredGuild: Guild? get() = runBlocking { getKoin().get<Kord>().getGuild(Config.guild) }

suspend fun CommandContext.emoteMention(snowflake: Snowflake?): String? = snowflake?.let {
    getGuild()?.asGuildOrNull()?.getEmojiOrNull(snowflake)?.mention
}
