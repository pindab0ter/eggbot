package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass

class Farmer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Farmer>(Farmers)

    var discordTag by Farmers.discordTag
    var inGameName by Farmers.inGameName
    var role by Farmers.role
}

class Contract(id: EntityID<String>) : Entity<String>(id) {

}