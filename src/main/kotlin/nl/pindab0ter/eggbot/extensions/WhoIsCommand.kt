package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalUser
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.annotation.KordPreview
import dev.kord.core.entity.User
import nl.pindab0ter.eggbot.converters.optionalFarmer
import nl.pindab0ter.eggbot.helpers.configuredGuild
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import org.jetbrains.exposed.sql.SortOrder.DESC
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class WhoIsCommand : Extension() {
    override val name: String = javaClass.simpleName

    override suspend fun setup() {
        class WhoIsThisArguments : Arguments() {
            val discordUser: User? by optionalUser {
                name = "member"
                description = "Find out which farmers this member has registered."
            }
            val farmer: Farmer? by optionalFarmer {
                name = "farmer"
                description = "Find out which member has registered this farmer."

                autoComplete {
                    val farmerInput: String = command.options["name"]?.value as String? ?: ""

                    val farmers = transaction {
                        Farmer
                            .find { Farmers.inGameName like "%$farmerInput%" }
                            .orderBy(Farmers.inGameName to DESC)
                            .limit(25)
                            .associate { farmer -> farmer.inGameName to farmer.inGameName }
                    }

                    suggestStringMap(farmers)
                }
            }
        }

        publicSlashCommand(::WhoIsThisArguments) {
            name = "whoisthis"
            description = "Find out who is who."

            action {
                when {
                    arguments.discordUser != null && arguments.farmer != null -> {
                        respond { content = "You can't specify both a member and a farmer." }
                    }

                    arguments.discordUser != null -> {
                        val farmerList = transaction {
                            val farmers = Farmer.find { Farmers.discordId eq arguments.discordUser?.id.toString() }
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

                        respond {
                            content =
                                if (farmerList.isNotEmpty()) "${arguments.discordUser?.mention} has: $farmerList"
                                else "${arguments.discordUser?.mention} has not registered any farmers."
                        }
                    }

                    arguments.farmer != null -> {
                        val discordUser = arguments.farmer?.discordId?.let { configuredGuild?.getMember(it) }

                        respond {
                            content =
                                if (discordUser != null) "${arguments.farmer?.inGameName} is registered by ${discordUser.mention}"
                                else "**Error:** Failed to find member for ${arguments.farmer?.inGameName}."
                        }
                    }
                }
            }
        }
    }
}
