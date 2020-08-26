package nl.pindab0ter.eggbot.database

import org.jetbrains.exposed.dao.IdTable

object Contracts : IdTable<String>() {
    override val id = text("id").uniqueIndex().entityId()
    val name = text("name")
    val finalGoal = double("final_goal")
}
