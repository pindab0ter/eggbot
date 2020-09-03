package nl.pindab0ter.eggbot.model

import com.auxbrain.ei.Contract
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.helpers.mapCartesianProducts
import nl.pindab0ter.eggbot.helpers.sumByBigDecimal
import nl.pindab0ter.eggbot.model.database.Coop
import nl.pindab0ter.eggbot.model.database.Farmer
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val FILL_PERCENTAGE = 0.8
private val roleColor = Color(15, 212, 57) // #0FD439

private fun createCoops(
    farmers: List<Farmer>,
    contract: Contract,
    preferredCoopSize: Int,
    baseName: String,
): List<Coop> = transaction {
    val amountOfCoops = (farmers.size / preferredCoopSize) + 1
    val coopNames = coopNames(amountOfCoops, baseName)
    List(amountOfCoops) { index ->
        val roleId = EggBot.guild.createRole().run {
            setName(coopNames[index])
            setMentionable(true)
            setColor(roleColor)
            complete()
        }.id
        Coop.new {
            this.contractId = contract.id
            this.name = coopNames[index]
            this.roleId = roleId
        }
    }
}

fun createRollCall(
    farmers: List<Farmer>,
    contract: Contract,
    baseName: String,
): List<Coop> {
    val activeFarmers = farmers.filter { it.isActive }.sortedByDescending { it.earningsBonus }
    val inactiveFarmers = farmers.filter { !it.isActive }.sortedBy { it.earningsBonus }
    val preferredCoopSize: Int =
        if (contract.maxCoopSize <= 10) contract.maxCoopSize
        else (contract.maxCoopSize * FILL_PERCENTAGE).roundToInt()
    val coops = createCoops(farmers, contract, preferredCoopSize, baseName)

    transaction {
        // Fill each co-op with the next strongest player so that all co-ops have one
        coops.forEachIndexed { i, coop ->
            coop.farmers = SizedCollection(coop.farmers.plus(activeFarmers[i]))
        }

        // With the remaining active farmers keep adding the next highest rated to the lowest rated co-op
        activeFarmers.drop(coops.size).forEach { activeFarmer ->
            coops.filter { coop -> coop.farmers.count() <= preferredCoopSize }
                .filter { coop -> coop.farmers.count() == coops.map { it.farmers.count() }.minOrNull() }
                .minByOrNull { coop -> coop.farmers.sumByBigDecimal { it.earningsBonus } }!!
                .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(activeFarmer)) }
        }

        // Finally spread inactive farmers over the coops
        inactiveFarmers.forEach { inactiveFarmer ->
            coops.sortedBy { coop -> coop.farmers.count() }
                .minByOrNull { coop -> coop.farmers.count { farmer -> !farmer.isActive } }!!
                .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(inactiveFarmer)) }
        }
    }
    return coops
}

private fun coopNames(amount: Int, baseName: String): List<String> = when {
    amount <= 26 -> ('a' until 'a' + amount).map { char -> "$char$baseName" }
    else -> {
        val chunks = ceil(amount.div(26.0)).toInt()
        val chunkSize = floor(amount.toDouble().div(chunks)).toInt()
        val remainder = amount - chunks * chunkSize
        ('a' until 'a' + chunkSize).mapCartesianProducts(1..chunks) { char: Char, digit: Int ->
            "$char$digit$baseName"
        }.plus(('a' until 'a' + remainder).map { c -> "${c + chunkSize}$baseName" })
    }
}
