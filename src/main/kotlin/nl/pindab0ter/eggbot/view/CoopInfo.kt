package nl.pindab0ter.eggbot.view

import nl.pindab0ter.eggbot.EggBot.toEmote
import nl.pindab0ter.eggbot.helpers.*
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.FOUR
import nl.pindab0ter.eggbot.helpers.BigDecimal.Companion.SIXTY
import nl.pindab0ter.eggbot.helpers.HabsStatus.BottleneckReached
import nl.pindab0ter.eggbot.helpers.HabsStatus.MaxedOut
import nl.pindab0ter.eggbot.model.Table.AlignedColumn.Alignment.RIGHT
import nl.pindab0ter.eggbot.model.simulation.new.CoopContractState
import org.joda.time.Duration
import java.math.BigDecimal
import kotlin.random.Random


fun coopInfoResponseNew(
    state: CoopContractState,
    compact: Boolean = false,
): List<String> = buildString {
    state.apply {
        val shortenedNames = farmers.map { farmer ->
            farmer.name.let { name ->
                if (name.length <= 10) name
                else "${name.substring(0 until 9)}…"
            }
        }

        appendLine("`${coopId}` vs. _${contractName}_:")

        // region Goals

        appendTable {
            title = "__${egg.toEmote()} **Goals** (${goalsReached}/${goals.count()}):__"
            displayHeader = false
            topPadding = 1
            bottomPadding = 1

            incrementColumn(suffix = ".")
            column {
                leftPadding = 1
                cells = goals.map { (target, _) -> target.asIllions(NumberFormatter.OPTIONAL_DECIMALS) }
            }
            column {
                leftPadding = 2
                rightPadding = 2
                cells = goals.map { (_, moment) ->
                    when {
                        moment == null || moment > timeRemaining -> "🔴"
                        moment == Duration.ZERO -> "🏁"
                        else -> "🟢"
                    }
                }
            }
            column {
                cells = goals.map { (_, moment) ->
                    when (moment) {
                        null -> "More than a year"
                        Duration.ZERO -> "Goal reached!"
                        else -> moment.asDaysHoursAndMinutes(compact)
                    }
                }
            }
        }

        // endregion Goals

        // region Basic info and totals

        appendLine("__🗒️ **Basic info**__ ```")
        appendLine("Time remaining:   ${timeRemaining.asDaysHoursAndMinutes(compact)}")
        append("Eggspected:       ${eggspected.asIllions()} ")
        if (!compact) append("(${
            farmers.sumByBigDecimal { farmer -> eggIncrease(farmer.finalState.habs, farmer.constants) }
                .multiply(SIXTY).asIllions()
        })")
        appendLine()
        append("Current eggs:     ${
            farmers.sumByBigDecimal { farmer -> farmer.initialState.eggsLaid }.asIllions()
        } ")
        if (!compact) append("(${
            farmers.sumByBigDecimal { farmer ->
                eggIncrease(farmer.initialState.habs, farmer.constants)
            }.multiply(SIXTY).asIllions()
        })")
        appendLine()
        append("Current chickens: ${farmers.sumByBigDecimal { farmer -> farmer.initialState.population }.asIllions()} ")
        if (!compact) append("(${
            farmers.sumByBigDecimal { farmer ->
                chickenIncrease(farmer.initialState.habs, farmer.constants)
                    .multiply(FOUR - farmer.initialState.habs.fullCount())
            }.multiply(SIXTY).asIllions()
        }/hr)")
        appendLine()
        appendLine("Tokens available: $tokensAvailable")
        appendLine("Tokens spent:     $tokensSpent")
        if (public) appendLine("Access:           This co-op is PUBLIC")
        appendLine("```\u200B")

        // endregion Basic info and totals

        val memberEmoji = when (Random.nextBoolean()) {
            true -> "👨‍🌾"
            false -> "👩‍🌾"
        }

        if (!compact) {

            // region Non-compact

            @Suppress("SpellCheckingInspection")
            appendTable {
                title = "__**${memberEmoji} Members** (${farmers.count()}/${maxCoopSize}):__"

                incrementColumn(":")
                column {
                    header = "Name"
                    leftPadding = 1
                    rightPadding = 3
                    cells = farmers.map { farmer -> farmer.name + if (!farmer.isSleeping) "" else " zZ" }
                }
                column {
                    header = "Eggs"
                    alignment = RIGHT
                    cells = farmers.map { farmer -> farmer.initialState.eggsLaid.asIllions() }
                }
                divider()
                column {
                    header = "/hr"
                    rightPadding = 3
                    cells = farmers.map { farmer ->
                        if (farmer.awayTimeRemaining <= Duration.ZERO) BigDecimal.ZERO.asIllions()
                        else eggIncrease(farmer.initialState.habs, farmer.constants)
                            .multiply(SIXTY).asIllions()
                    }
                }
                column {
                    header = "Chickens"
                    alignment = RIGHT
                    cells = farmers.map { farmer -> farmer.initialState.population.asIllions() }
                }
                divider()
                column {
                    header = "/hr"
                    rightPadding = 3
                    cells = farmers.map { farmer ->
                        chickenIncrease(farmer.initialState.habs, farmer.constants)
                            .multiply(FOUR - farmer.initialState.habs.fullCount())
                            .multiply(SIXTY).asIllions()
                    }
                }
                column {
                    header = "Tkns"
                    alignment = RIGHT
                    cells = farmers.map { farmer ->
                        if (farmer.constants.tokensAvailable > 0) "${farmer.constants.tokensAvailable}" else ""
                    }
                }
                divider()
                column {
                    header = "Spent"
                    cells = farmers.map { farmer ->
                        if (farmer.constants.tokensSpent > 0) "${farmer.constants.tokensSpent}" else ""
                    }
                }
            }

            append('\u200B')

            val bottleneckedFarmers = farmers.filter { farmer ->
                willReachBottleneckBeforeDone(farmer, timeRemaining, goals.last().moment)
            }

            if (bottleneckedFarmers.isNotEmpty()) appendTable {
                title = "__**⚠ Bottlenecks**__"
                topPadding = 1

                column {
                    header = "Name"
                    if (!compact) rightPadding = 2
                    cells = bottleneckedFarmers.map { farmer -> farmer.name }
                }

                column {
                    header = "Habs"
                    leftPadding = 1
                    alignment = RIGHT
                    cells = bottleneckedFarmers.map { farmer ->
                        when (farmer.finalState.habsStatus) {
                            is BottleneckReached -> when (farmer.finalState.habsStatus.moment) {
                                Duration.ZERO -> "Full!"
                                else -> farmer.finalState.habsStatus.moment.asDaysHoursAndMinutes(true)
                            }
                            is MaxedOut -> when (farmer.finalState.habsStatus.moment) {
                                Duration.ZERO -> "Maxed!"
                                else -> farmer.finalState.habsStatus.moment.asDaysHoursAndMinutes(true)
                            }
                            else -> ""
                        }
                    }
                }

                emojiColumn {
                    header = "🏘️"
                    leftPadding = 1
                    cells = bottleneckedFarmers.map { farmer ->
                        when (farmer.finalState.habsStatus) {
                            is BottleneckReached -> if (farmer.finalState.habsStatus.moment == Duration.ZERO) "🛑" else "⚠️"
                            is MaxedOut -> "🟢"
                            else -> "➖"
                        }
                    }
                }

                divider()

                column {
                    header = "Transport"
                    leftPadding = 1
                    alignment = RIGHT
                    cells = bottleneckedFarmers.map { farmer ->
                        when (farmer.finalState.transportBottleneck) {
                            null -> ""
                            Duration.ZERO -> "Full!"
                            else -> farmer.finalState.transportBottleneck.asDaysHoursAndMinutes(true)
                        }
                    }
                }

                emojiColumn {
                    header = "🚛"
                    leftPadding = 1
                    cells = bottleneckedFarmers.map { farmer ->
                        when (farmer.finalState.transportBottleneck) {
                            null -> "➖"
                            Duration.ZERO -> "🛑"
                            else -> "⚠️"
                        }
                    }
                }

                divider()

                column {
                    header = "Silos"
                    leftPadding = 1
                    alignment = RIGHT
                    cells = bottleneckedFarmers.map { farmer ->
                        when {
                            farmer.awayTimeRemaining <= Duration.ZERO -> "Empty!"
                            farmer.awayTimeRemaining < Duration.standardHours(12L) ->
                                farmer.awayTimeRemaining.asDaysHoursAndMinutes(true)
                            else -> ""
                        }
                    }
                }

                emojiColumn {
                    header = "⌛"
                    leftPadding = 1
                    cells = bottleneckedFarmers.map { farmer ->
                        when {
                            farmer.awayTimeRemaining <= Duration.ZERO ->
                                "🛑"
                            farmer.awayTimeRemaining < Duration.standardHours(12L) ->
                                "⚠️"
                            else -> "➖"
                        }
                    }
                }

                divider(intersection = '╡')
            }

            // endregion Non-compact

        } else {

            // region Compact

            appendTable {
                title = "__**${memberEmoji} Members** (${farmers.count()}/${maxCoopSize}):__"

                column {
                    header = "Name"
                    rightPadding = 2
                    cells = farmers.zip(shortenedNames).map { (farmer, name) ->
                        "$name${if (!farmer.isSleeping) "" else " zZ"}"
                    }
                }
                column {
                    header = "Eggs"
                    alignment = RIGHT
                    cells = farmers.map { farmer -> farmer.initialState.eggsLaid.asIllions() }
                }
                divider()
                column {
                    header = "/hr"
                    rightPadding = 2
                    cells = farmers.map { farmer ->
                        eggIncrease(farmer.initialState.habs, farmer.constants).multiply(SIXTY).asIllions()
                    }
                }
            }

            append('\u200B')

            appendTable {
                title = "__**🎫 Tokens**__"
                topPadding = 1
                column {
                    header = "Name"
                    rightPadding = 2
                    cells = shortenedNames
                }
                column {
                    header = "Tokens"
                    alignment = RIGHT
                    cells = farmers.map { farmer -> "${farmer.constants.tokensAvailable}" }
                }
                divider()
                column {
                    header = "Spent"
                    cells = farmers.map { farmer -> "${farmer.constants.tokensSpent}" }
                }
            }

            append('\u200B')

            val bottleneckedFarmers = farmers.zip(shortenedNames).filter { (farmer, _) ->
                willReachBottleneckBeforeDone(farmer, timeRemaining, goals.last().moment)
            }

            if (bottleneckedFarmers.isNotEmpty()) appendTable {
                title = "__**⚠ Bottlenecks**__"
                topPadding = 1

                column {
                    header = "Name"
                    cells = bottleneckedFarmers.map { (_, shortenedName) -> shortenedName }
                }

                emojiColumn {
                    header = "🏘️"
                    leftPadding = 1
                    cells = bottleneckedFarmers.map { (farmer, _) ->
                        when (farmer.finalState.habsStatus) {
                            is BottleneckReached -> if (farmer.finalState.habsStatus.moment == Duration.ZERO) "🛑" else "⚠️"
                            is MaxedOut -> "🟢"
                            else -> "➖"
                        }
                    }
                }

                divider()

                emojiColumn {
                    header = "🚛"
                    cells = bottleneckedFarmers.map { (farmer, _) ->
                        when (farmer.finalState.transportBottleneck) {
                            null -> "➖"
                            Duration.ZERO -> "🛑"
                            else -> "⚠️"
                        }
                    }
                }

                divider()

                emojiColumn {
                    header = "⌛"
                    cells = bottleneckedFarmers.map { (farmer, _) ->
                        when {
                            farmer.awayTimeRemaining <= Duration.ZERO ->
                                "🛑"
                            farmer.awayTimeRemaining < Duration.standardHours(12L) ->
                                "⚠️"
                            else -> "➖"
                        }
                    }
                }
                // endregion Compact
            }
        }
    }
}.splitMessage(separator = '\u200B')
