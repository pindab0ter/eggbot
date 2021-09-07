package nl.pindab0ter.eggbot.model.database

import org.jetbrains.exposed.dao.IdTable
import org.joda.time.DateTime

object DiscordUsers : IdTable<String>() {
    override val id = text("discord_id").entityId()
    val discordTag = text("discord_tag")
    val inactiveUntil = datetime("inactive_until").nullable()
    val optedOutOfCoopLeadAt = datetime("opted_out_of_coop_lead_at").nullable()
    val createdAt = Farmers.datetime("createdAt").default(DateTime.now())
    val modifiedAt = Farmers.datetime("modifiedAt").default(DateTime.now())
}
