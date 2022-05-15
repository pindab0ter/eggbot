package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.jodatime.datetime


object Coops : IntIdTable() {
    override val tableName = "coops"
    val name = text("name")
    val contractId = text("contract_id")
    val roleId = text("role_id").nullable()
    val channelId = text("channel_id").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        this.index(true, name, contractId)
    }
}
