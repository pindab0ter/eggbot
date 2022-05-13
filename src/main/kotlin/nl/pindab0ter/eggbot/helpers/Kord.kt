package nl.pindab0ter.eggbot.helpers

import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.model.Config

val kord: Kord get() = runBlocking { getKoin().get() }
val guild: Guild? get() = runBlocking { kord.getGuild(Config.guild) }

suspend fun emoteMention(snowflake: Snowflake?): String? = snowflake?.let {
    guild?.asGuildOrNull()?.getEmojiOrNull(snowflake)?.mention
}
