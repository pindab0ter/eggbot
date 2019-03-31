package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import org.jetbrains.exposed.dao.*

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, DiscordUser>(DiscordUsers)

    var discordTag by DiscordUsers.id
    private val _inGameNames by InGameName referrersOn InGameNames.discordTag
    val inGameNames: List<String> get() = _inGameNames.map { it.inGameName }
}

class InGameName(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<InGameName>(InGameNames)
    var discordTag by DiscordUser referencedOn InGameNames.discordTag
    var inGameName by InGameNames.inGameName
}

class Contract(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Contract>(Contracts)

    val identifier: String get() = id.value
    var name by Contracts.title
    var description by Contracts.description
    var egg: EggInc.Egg by Contracts.egg
    var coopAllowed by Contracts.coopAllowed
    var maxCoopSize by Contracts.coopSize
    var expirationTime by Contracts.validUntil
    var lengthSeconds by Contracts.duration
}