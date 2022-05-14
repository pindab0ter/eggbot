package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE


object Coops : IntIdTable() {
    override val tableName = "coops"
    val name = text("name")
    val contractId = text("contract_id")
    val roleId = text("role_id").nullable()
    val channelId = text("channel_id").nullable()
    val guildId = DiscordUsers.reference("guild_id", DiscordGuilds, CASCADE, CASCADE)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    init {
        this.index(true, name, contractId)
    }
}
