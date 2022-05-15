package nl.pindab0ter.eggbot.helpers

import com.auxbrain.ei.Egg
import com.kotlindiscord.kord.extensions.utils.getKoin
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.GuildChannel
import kotlinx.coroutines.runBlocking
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.database.DiscordGuild
import org.jetbrains.exposed.sql.transactions.transaction

val kord: Kord get() = runBlocking { getKoin().get() }
val guilds: List<Guild> get() = transaction { DiscordGuild.all().mapNotNull(DiscordGuild::asGuild) }

suspend fun Guild.emoteMention(snowflake: Snowflake?): String? = snowflake?.let {
    getEmojiOrNull(snowflake)?.mention
}

fun Guild.emoteMention(egg: Egg): String? = runBlocking {
    Config.eggsToEmotes[egg]?.let { getEmojiOrNull(it) }?.mention
}

suspend fun GuildBehavior.getChannelOrNull(channelId: Snowflake?): GuildChannel? = when (channelId) {
    null -> null
    else -> getChannelOrNull(channelId)
}

suspend fun GuildBehavior.getRoleOrNull(roleId: Snowflake?): Role? = when (roleId) {
    null -> null
    else -> getRoleOrNull(roleId)
}
