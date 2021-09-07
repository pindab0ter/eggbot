package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.ReferenceOption.SET_NULL
import org.joda.time.DateTime


object Coops : IntIdTable() {
    val name = text("name")
    val contractId = text("contract_id")
    val leaderId = reference("leader_id", Farmers, CASCADE, SET_NULL).nullable()
    val roleId = text("role_id").nullable()
    val channelId = text("channel_id").nullable()
    val createdAt = datetime("createdAt").default(DateTime.now())
    val modifiedAt = datetime("modifiedAt").default(DateTime.now())

    init {
        this.index(true, name, contractId)
    }
}
