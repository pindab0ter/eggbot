package nl.pindab0ter.eggbot.kord.commands

import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType.PUBLIC
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.slash.converters.impl.optionalEnumChoice
import dev.kord.common.annotation.KordPreview
import nl.pindab0ter.eggbot.helpers.DisplayMode
import nl.pindab0ter.eggbot.helpers.publicMultipartFollowUp
import nl.pindab0ter.eggbot.helpers.timeSinceBackup
import nl.pindab0ter.eggbot.model.AuxBrain
import nl.pindab0ter.eggbot.model.EarningsBonus
import nl.pindab0ter.eggbot.model.database.DiscordUser
import nl.pindab0ter.eggbot.model.database.Farmer
import nl.pindab0ter.eggbot.view.earningsBonusResponse
import org.jetbrains.exposed.sql.transactions.transaction

@KordPreview
object Contract {
    class ContractArguments : Arguments() {
    }

    val command: suspend SlashCommand<out ContractArguments>.() -> Unit = {
        name = "contract"
        description = ""
        autoAck = PUBLIC

        check {
        }

        action {
        }

        subCommand {
            name = "add"
            description = ""

            check {

            }

            action {
                // TODO: Create channels
            }
        }

        subCommand {
            name = "remove"
            description = ""

            check {

            }

            action {
                // TODO: Remove channels
            }
        }
    }
}
