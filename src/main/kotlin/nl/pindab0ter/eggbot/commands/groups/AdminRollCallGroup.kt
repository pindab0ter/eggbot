package nl.pindab0ter.eggbot.commands.groups

import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashGroup
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.createRole
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.rest.json.JsonErrorCode
import dev.kord.rest.request.KtorRequestException
import nl.pindab0ter.eggbot.model.Config

@KordPreview
val rollCallGroup: suspend SlashGroup.() -> Unit = {
    description = "Manage roll calls"

    subCommand {
        name = "create"
        description = "Create teams for a contract"

        check {

        }

        action {
            // TODO: Create channels
        }
    }

    subCommand {
        name = "clear"
        description = "Remove all teams for a contract"

        check {

        }

        action {
            // TODO: Remove channels
        }
    }
}