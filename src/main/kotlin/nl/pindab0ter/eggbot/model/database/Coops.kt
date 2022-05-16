package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.jodatime.datetime
import org.joda.time.DateTime.now


object Coops : IntIdTable() {
    override val tableName = "coops"
    val name = text("name")
    val contractId = text("contract_id")
    val roleId = text("role_id").nullable()
    val channelId = text("channel_id").nullable()
    val createdAt = datetime("created_at").clientDefault { now() }
    val updatedAt = datetime("updated_at").clientDefault { now() }

    init {
        this.index(true, name, contractId)
    }
}
