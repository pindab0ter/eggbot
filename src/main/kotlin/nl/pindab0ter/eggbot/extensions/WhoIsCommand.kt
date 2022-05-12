package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import dev.kord.core.entity.User
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class WhoIsCommand : Extension() {
    override val name: String = javaClass.simpleName

    override suspend fun setup() {
        class WhoIsThisArguments : Arguments() {
            val discordUser: User by user {
                name = "member"
                description = "Find out which farmers this member has registered."
            }
        }

        publicSlashCommand(::WhoIsThisArguments) {
            name = "whoisthis"
            description = "Find out who is who."

            action {
                val farmerList = transaction {
                    val farmers = Farmer.find { Farmers.discordId eq arguments.discordUser.id.toString() }
                    buildString {
                        farmers.forEach { farmer ->
                            when (farmer) {
                                farmers.first() -> append(farmer.inGameName)
                                farmers.last() -> append(" and ${farmer.inGameName}")
                                else -> append(", ${farmer.inGameName}")
                            }
                        }
                    }
                }

                respond { content = "${arguments.discordUser.mention} has: $farmerList" }
            }
        }
    }
}
