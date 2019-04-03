package nl.pindab0ter.eggbot.database

import com.auxbrain.ei.EggInc
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID

class DiscordUser(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, DiscordUser>(DiscordUsers)

    val discordId: String get() = id.value
    var discordTag by DiscordUsers.discordTag
    val farmers by Farmer referrersOn Farmers.discordId
}

class Farmer(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Farmer>(Farmers)

    val inGameId: String get() = id.value
    var discordId by DiscordUser referencedOn Farmers.discordId
    var inGameName by Farmers.inGameName
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
