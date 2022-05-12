package nl.pindab0ter.eggbot.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.annotation.KordPreview
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.Config
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
class RegisterCommand : Extension() {
    val logger = KotlinLogging.logger { }
    override val name: String = javaClass.simpleName

    override suspend fun setup() {

        class RegisterArguments : Arguments() {
            val eggIncId by string {
                name = "egg-inc-id"
                description = "Find your Egg, Inc. ID in Menu → Settings → More, at the bottom of that screen."

                mutate { it.uppercase() }

                validate {
                    val eggIncId = mutator?.invoke(value) ?: value

                    failIf(
                        !eggIncId.startsWith(prefix = "EI") || eggIncId.length != 18,
                        "Your _Egg, Inc. ID_ starts with `EI` and is followed by exactly 16 digits."
                    )
                    throwIfFailed()

                    val farmerBackup = AuxBrain.getFarmerBackup(eggIncId)
                    failIf(
                        farmerBackup?.game == null || farmerBackup.stats == null,
                        "Could not find a farmer with _Egg, Inc. ID_ `$eggIncId`"
                    )
                    throwIfFailed()

                    transaction {
                        if (Farmer.findById(eggIncId)?.discordUser != null) {
                            fail("This player is already registered to ${runBlocking { context.getMember()?.mention }}")
                        }
                    }
                }
            }
        }

        ephemeralSlashCommand(::RegisterArguments) {
            name = "register"
            description = "Register an Egg, Inc. account."
            guild(Config.guild)

            action {

                val farmerBackup = AuxBrain.getFarmerBackup(arguments.eggIncId)
                if (farmerBackup == null) {
                    respond { content = "Could not find a farmer with the ID `${arguments.eggIncId}`" }
                    return@action
                }

                val discordUser = transaction {
                    DiscordUser.findOrCreate(event.interaction.user)
                }

                val farmer = transaction {
                    Farmer.new(discordUser, farmerBackup)
                }

                if (farmer == null) {
                    respond { content = "**Error:** Could not register your account. Please contact the admins." }
                    return@action
                }

                respond { content = "Farmer `${farmer.inGameName}` has been registered to ${runBlocking { getMember()?.mention }}." }
            }
        }
    }
}
