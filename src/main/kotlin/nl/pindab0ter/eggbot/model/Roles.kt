package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.Contract
import dev.kord.core.entity.Role
import mu.KotlinLogging.logger
import nl.pindab0ter.eggbot.helpers.mapCartesianProducts
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.DiscordUser
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor


// TODO:
// fun deleteCoopsAndRoles(
//     coopsToRoles: List<Pair<Coop, Role?>>,
//     event: CommandEvent,
// ): Pair<Map<String, String?>, Map<String, String?>> {
//     val logger = logger("deleteCoopsAndRoles")
//     val successes = mutableListOf<Pair<String, String?>>()
//     val failures = mutableListOf<Pair<String, String?>>()
//
//     coopsToRoles.forEach { (coop, role) ->
//         val coopNameToRoleName = coop.name to role?.name
//         if (role != null) role.delete().submit().handle { _, exception ->
//             if (exception != null) "Failed to remove Discord role (${exception.localizedMessage})".let {
//                 failures.add(coopNameToRoleName)
//                 logger.warn { it }
//                 event.replyWarning(it)
//             } else {
//                 transaction { coop.delete() }
//                 successes.add(coopNameToRoleName)
//                 logger.info { "Co-op ${coopNameToRoleName.first} and role ${coopNameToRoleName.second} successfully removed." }
//             }
//         }.join() else {
//             transaction { coop.delete() }
//             successes.add(coopNameToRoleName)
//             logger.info { "Co-op ${coopNameToRoleName.first} successfully removed." }
//         }
//     }
//     return successes.toMap() to failures.toMap()
// }
