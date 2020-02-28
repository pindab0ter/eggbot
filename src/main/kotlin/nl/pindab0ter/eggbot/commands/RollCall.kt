package nl.pindab0ter.eggbot.commands

import com.auxbrain.ei.EggInc
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import mu.KotlinLogging
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.EggBot
import nl.pindab0ter.eggbot.commands.categories.AdminCategory
import nl.pindab0ter.eggbot.database.Coop
import nl.pindab0ter.eggbot.database.Coops
import nl.pindab0ter.eggbot.database.Farmer
import nl.pindab0ter.eggbot.network.AuxBrain
import nl.pindab0ter.eggbot.utilities.*
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object RollCall : Command() {

    private val log = KotlinLogging.logger { }
    private val allowedCharacters = Regex("""^[a-zA-Z0-9\-]+$""")
    private val guild get() = EggBot.guild

    init {
        name = "roll-call"
        arguments = "<contract id> <base name> [overwrite]"
        help = "Create a co-op roll call for the specified contract, " +
                "creating co-ops and server roles and assigns those roles. " +
                "The names will be based on `<base name>`, " +
                "which can only consist of letters, digits and dashes and cannot contain spaces."
        category = AdminCategory
        guildOnly = false
    }

    @Suppress("FoldInitializerAndIfToElvis")
    override fun execute(event: CommandEvent) {
        event.channel.sendTyping().queue()

        (checkPrerequisites(
            event,
            adminRequired = true,
            minArguments = 2,
            maxArguments = 3
        ) as? PrerequisitesCheckResult.Failure)?.message?.let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractId: String = event.arguments[0]
        val baseName: String = event.arguments[1]
        val force: Boolean = event.arguments.getOrNull(2)?.equals("overwrite") == true

        if (!allowedCharacters.matches(baseName)) "Only letters, digits and dashes are allowed.".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        val contractInfo: EggInc.Contract? = AuxBrain.getPeriodicals()?.contracts?.contractsList?.find {
            it.id == contractId
        }

        if (contractInfo == null) "No active contract found with id `${contractId}`".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        if (!contractInfo.coopAllowed) "Co-op is not allowed for this contract".let {
            event.replyWarning(it)
            log.debug { it }
            return
        }

        transaction {
            val existingCoops: List<Coop> = Coop.find { Coops.contract eq contractInfo.id }.toList()
            if (existingCoops.isNotEmpty()) {
                if (force) {
                    val roles = existingCoops.mapNotNull { coop ->
                        coop.roleId?.let { guild.getRoleById(it) }
                    }

                    if (roles.isEmpty()) "No roles found for `${contractInfo.id}`".let {
                        event.replyWarning(it)
                        log.warn { it }
                    }

                    val roleDeletions = roles.map { role ->
                        role.delete().submit()
                    }

                    if (roleDeletions.any { it.isCompletedExceptionally }) "Something went wrong. Please contact ${EggBot.jdaClient.getUserById(
                        Config.ownerId
                    )?.asMention ?: "the bot maintainer"}".let {
                        event.replyWarning(it)
                        log.error { it }
                    }

                    Coops.deleteWhere { Coops.contract eq contractInfo.id }
                    log.info { "Deleted contracts for ${contractInfo.id}" }
                } else "Co-ops are already generated for contract `${contractInfo.id}`. Add `overwrite` to override.".let {
                    event.replyWarning(it)
                    log.debug { it }
                    return@transaction
                }
            }

            val message = event.channel.sendMessage("Generating co-ops and creating roles…").complete()

            val farmers = transaction { Farmer.all().sortedByDescending { it.earningsBonus }.toList() }
            val coops: List<Coop> = PaddingDistribution.createRollCall(farmers, contractInfo, baseName)

            val progressBar = ProgressBarUpdater(farmers.count(), message, false)
            event.channel.sendTyping().queue()

            transaction {
                var i = 0
                coops.map { coop ->
                    coop.roleId?.let { guild.getRoleById(it) } to coop.farmers
                }.forEach { (role, coopFarmers) ->
                    coopFarmers.map { farmer ->
                        val discordId = farmer.discordUser.discordId
                        val discordTag = farmer.discordUser.discordTag
                        guild.addRoleToMember(discordId, role!!).submit().handle { _, exception ->
                            if (exception == null) log.info("Added $discordTag to ${role.name}")
                            else log.warn("Failed to add $discordTag to ${role.name}. Cause: ${exception.localizedMessage}")
                        }.join()
                        i++
                        progressBar.update(i)
                        event.channel.sendTyping().complete()
                    }
                }
            }

            // TODO: Replace with table

            message.editMessage(StringBuilder("Co-ops generated for `${contractInfo.id}`:").appendln().apply {
                append("```")
                coops.forEach { coop ->
                    append(coop.name)
                    appendPaddingCharacters(coop.name, coops.map { it.name })
                    append(" (")
                    appendPaddingCharacters(coop.farmers.count(), coops.map { it.farmers.count() })
                    append(coop.farmers.count())
                    append("/${contractInfo.maxCoopSize} members): ")
                    appendPaddingCharacters(
                        coop.activeEarningsBonus.formatIllions(true),
                        coops.map { it.activeEarningsBonus.formatIllions(true) })
                    append(coop.activeEarningsBonus.formatIllions(true) + " %")
                    appendln()
                }
                append("```")
            }.toString()).queue()

            coops.map { coop ->
                val role = coop.roleId?.let { guild.getRoleById(it) }
                StringBuilder().apply {
                    appendln("__**Co-op ${role?.asMention ?: coop.name} (`${coop.name}`)**__:")
                    coop.farmers.forEach { farmer ->
                        append(
                            guild.getMemberById(farmer.discordUser.discordId)?.asMention
                                ?: farmer.discordUser.discordName
                        )
                        append(" (`${farmer.inGameName}`)")
                        if (farmer.isActive.not()) append(" _Inactive_")
                        appendln()
                    }
                }.toString()
            }.forEach {
                event.reply(it)
            }
        }
    }

    object PaddingDistribution {
        private const val FILL_PERCENTAGE = 0.8

        private fun createCoops(
            farmers: List<Farmer>,
            contract: EggInc.Contract,
            preferredCoopSize: Int,
            baseName: String
        ): List<Coop> = transaction {
            val amountOfCoops = (farmers.size / preferredCoopSize) + 1
            val coopNames = coopNames(amountOfCoops, baseName)
            List(amountOfCoops) { index ->
                val roleId = guild.createRole()
                    .setName(coopNames[index])
                    .setMentionable(true)
                    .complete()
                    .id
                Coop.new {
                    this.contract = contract.id
                    this.name = coopNames[index]
                    this.roleId = roleId
                }
            }
        }

        fun createRollCall(
            farmers: List<Farmer>,
            contract: EggInc.Contract,
            baseName: String
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
                        .filter { coop -> coop.farmers.count() == coops.map { it.farmers.count() }.min() }
                        .minBy { coop -> coop.farmers.sumByBigDecimal { it.earningsBonus } }!!
                        .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(activeFarmer)) }
                }

                // Finally spread inactive farmers over the coops
                inactiveFarmers.forEach { inactiveFarmer ->
                    coops.sortedBy { coop -> coop.farmers.count() }
                        .minBy { coop -> coop.farmers.count { farmer -> !farmer.isActive } }!!
                        .let { coop -> coop.farmers = SizedCollection(coop.farmers.plus(inactiveFarmer)) }
                }
            }
            return coops
        }

        fun coopNames(amount: Int, baseName: String = Config.coopName): List<String> = when {
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
    }
}
