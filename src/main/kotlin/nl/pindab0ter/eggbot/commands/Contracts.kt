package nl.pindab0ter.eggbot.commands

import com.github.kittinunf.fuel.Fuel
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import nl.pindab0ter.eggbot.auxbrain.EggInc
import nl.pindab0ter.eggbot.database.Contract
import nl.pindab0ter.eggbot.daysAndHours
import nl.pindab0ter.eggbot.formattedName
import nl.pindab0ter.eggbot.network.GET_CONTRACTS_URL
import nl.pindab0ter.eggbot.network.base64Decoded
import nl.pindab0ter.eggbot.toDateTime
import nl.pindab0ter.eggbot.toPeriod
import org.jetbrains.exposed.sql.transactions.transaction

object Contracts : Command() {
    init {
        name = "contracts"
        help = "Shows currently active contracts"
        guildOnly = false
    }


    // TODO: Refactor and handle failure
    override fun execute(event: CommandEvent) {
        Fuel.get(GET_CONTRACTS_URL).response { _, response, _ ->
            EggInc.GetContractsResponse.parseFrom(response.body().base64Decoded())
                .contractsList
                .forEach { contract ->
                    transaction {
                        Contract.new(contract.identifier) {
                            name = contract.name
                            description = contract.description
                            egg = contract.egg
                            coopAllowed = contract.coopAllow == 1
                            coopSize = contract.coopSize
                            validUntil = contract.validUntil
                            duration = contract.duration
                        }
                    }
                }

            val contracts = transaction {
                // TODO: Filter in query, not after
                Contract.all()
                    .filter { contract -> contract.validUntil.toDateTime().isAfterNow }
                    .sortedBy { it.validUntil }
            }

            val embed = EmbedBuilder()
                .setTitle("**Currently active contracts**")
                .apply {
                    contracts
                        .ifEmpty {
                            setDescription("There are currently no active contracts")
                            contracts
                        }
                        .forEach { contract ->
                            addField(
                                "**${contract.name}** - *${contract.egg.formattedName}*",
                                """
                                ${contract.description}
                                **Co-op allowed**: **${if (contract.coopAllowed) "✓ (${contract.coopSize})" else "✗"}**
                                **Duration**: ${daysAndHours.print(contract.duration.toPeriod())}

                            """.trimIndent(),
                                false
                            )
                        }
                }
                .build()

            event.reply(embed)

        }
    }
}
