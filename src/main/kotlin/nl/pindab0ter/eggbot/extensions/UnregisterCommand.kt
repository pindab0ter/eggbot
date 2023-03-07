package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.checks.hasRole
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalUser
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.annotation.KordPreview
import mu.KotlinLogging
import nl.pindab0ter.eggbot.config
import nl.pindab0ter.eggbot.converters.optionalFarmer
import nl.pindab0ter.eggbot.databases
import nl.pindab0ter.eggbot.helpers.mentionUser
import nl.pindab0ter.eggbot.helpers.toListing
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.model.database.Farmers
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class UnregisterCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() = config.servers.forEach { server ->
        class UnregisterArguments : Arguments() {
            val discordUser by optionalUser {
                name = "member"
                description = "The member to unregister."
            }
            val farmer: Farmer? by optionalFarmer {
                name = "farmer"
                description = "Find out which member has registered this farmer."

                autoComplete {
                    val farmerInput: String = command.options["farmer"]?.value as String

                    val farmers = transaction(databases[server.name]) {
                        Farmer
                            .find { Farmers.inGameName like "%$farmerInput%" }
                            .orderBy(Farmers.inGameName to ASC)
                            .limit(25)
                            .associate { farmer -> farmer.inGameName to farmer.inGameName }
                    }

                    suggestStringMap(farmers)
                }
            }
        }

        publicSlashCommand(::UnregisterArguments) {
            name = "unregister"
            description = "Unregister a member, removing their farmers from our database. This does not affect their game."
            guild(server.snowflake)

            check {
                hasRole(server.role.admin)
                passIf(event.interaction.user.id == config.botOwner)
            }

            action {
                when {
                    arguments.discordUser != null && arguments.farmer != null -> {
                        val message = "You can't specify both a member and a farmer."

                        logger.trace { message }
                        respond { content = message }
                    }

                    arguments.discordUser != null -> {
                        logger.trace { "Unregistering by Discord user: ${arguments.discordUser}" }

                        val databaseDiscordUser = transaction(databases[server.name]) {
                            arguments.discordUser!!.id.let { DiscordUser.findBy(it)?.load(DiscordUser::farmers) }
                        }

                        if (databaseDiscordUser == null) {
                            val message = "${arguments.discordUser?.mention} is not registered."
                            logger.trace { message }
                            respond { content = message }
                            return@action
                        }

                        val farmersListing = transaction(databases[server.name]) {
                            databaseDiscordUser.farmers.toListing()
                        }
                        logger.trace { "Farmers to delete: $farmersListing" }

                        transaction(databases[server.name]) {
                            databaseDiscordUser.delete()
                        }

                        respond {
                            val message = "Unregistered ${arguments.discordUser?.mention}, removing their farmers: ${farmersListing}."
                            logger.trace { message }
                            content = message
                        }
                    }

                    arguments.farmer != null -> {
                        logger.trace { "Unregistering by Farmer: ${arguments.farmer!!.inGameName}" }

                        val discordUser = transaction(databases[server.name]) {
                            arguments.farmer!!.discordUser.load(DiscordUser::farmers)
                        }
                        logger.trace { "DiscordUser associated with the farmer: ${discordUser.tag}" }

                        // If this is the only farmer
                        if (discordUser.farmers.minus(arguments.farmer).isEmpty()) {
                            logger.trace { "This is the only farmer of this Discord user" }

                            val discordUserMention = guild?.mentionUser(discordUser.snowflake)

                            transaction(databases[server.name]) {
                                arguments.farmer?.delete()
                                discordUser.delete()
                            }

                            respond {
                                val message = "Unregistered `${arguments.farmer?.inGameName}` and member $discordUserMention along with it."

                                logger.trace { message }
                                content = message
                            }
                        } else {
                            logger.trace { "There are still other farmers registered to this Discord user" }

                            transaction(databases[server.name]) {
                                arguments.farmer?.delete()
                            }

                            respond {
                                val message = buildString {
                                    appendLine("Unregistered `${arguments.farmer?.inGameName}`.")

                                    val count = discordUser.farmers.count()
                                    if (count > 0) {
                                        append("${discordUser.farmers.toListing()} ")
                                        if (count == 1L) append("is") else append("are")
                                        append(" still registered to ${guild?.mentionUser(discordUser.snowflake)}.")
                                    }
                                }

                                logger.trace { message }
                                content = message
                            }
                        }
                    }
                }
            }
        }
    }
}
