package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Contract
import com.auxbrain.ei.HabLevel.NO_HAB
import com.auxbrain.ei.HabLevel.SHACK
import com.auxbrain.ei.LocalContract
import com.auxbrain.ei.MyContracts
import com.auxbrain.ei.VehicleType.TRIKE
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import nl.pindab0ter.eggbot.utilities.toDouble
import org.joda.time.DateTime
import org.joda.time.Duration
import java.math.BigDecimal

class ContractSimulationTest : DescribeSpec({

    // Mock DateTime.now() since it provides the exact intended scope
    // This does require the usage of DateTime.now() instead of DateTime()
    val time = DateTime("2000-01-01T12:00")
    mockkStatic(DateTime::class)
    every { DateTime.now() } answers { time }

    val farm = Backup.Simulation {
        commonResearch = CommonResearch.toResearchItems()
        habs = listOf(SHACK, NO_HAB, NO_HAB, NO_HAB)
        habPopulation = listOf(0L, 0L, 0L, 0L)
        vehicles = listOf(TRIKE)
    }

    val firstGoal = Contract.Goal { targetAmount = 4.0 }
    val secondGoal = Contract.Goal { targetAmount = 6.0 }
    val thirdGoal = Contract.Goal { targetAmount = 8.0 }
    val localContract = LocalContract {
        contract = Contract {
            timeAccepted = time.toDouble()
            lengthSeconds = 10.0
            goals = listOf(firstGoal, secondGoal, thirdGoal)
        }
    }

    val backup = Backup {
        contracts = MyContracts { contracts = listOf(localContract) }
        farms = listOf(farm)
        game = Backup.Game { epicResearch = EpicResearch.toResearchItems() }
        approxTime = time.minusHours(1).toDouble()
    }

    lateinit var simulation: ContractSimulation

    describe("A Contract Simulation") {

        beforeTest {
            simulation = ContractSimulation(
                backup,
                backup.farms.first(),
                backup.contracts!!.contracts.first()
            )
        }

        it("shows the correct amount of time remaining") {
            simulation.timeRemaining.shouldBe(Duration.standardSeconds(10))
        }

        it("shows the correct time since last backup") {
            simulation.timeSinceLastUpdate.shouldBe(Duration.standardHours(1))
        }

        it("keeps track of elapsed time") {
            simulation.elapsed.shouldBe(Duration.standardMinutes(0))
            simulation.step()
            simulation.elapsed.shouldBe(Duration.standardMinutes(1))
        }

        context("with one chicken") {
            val oneChickenBackup = backup.copy {
                farms = listOf(farm.copy { habPopulation = listOf(1L, 0L, 0L, 0L) })
            }

            beforeTest {
                simulation = ContractSimulation(
                    oneChickenBackup,
                    oneChickenBackup.farms.first(),
                    oneChickenBackup.contracts!!.contracts.first()
                )
            }

            it("has one chicken") {
                simulation.currentPopulation.shouldBeEqualComparingTo(BigDecimal(1))
            }

            it("produces two eggs per minute") {
                simulation.step()
                simulation.projectedEggs.shouldBeEqualComparingTo(BigDecimal(2))
            }

            it("is not finished") {
                simulation.isFinished.shouldBeFalse()
            }

            it("has not reached any bottlenecks") {
                simulation.habBottleneckReached.shouldBeNull()
                simulation.transportBottleneckReached.shouldBeNull()
            }

            it("has not reached any goals") {
                simulation.goalsReached.shouldBeZero()
            }
        }

        context("some basic research") {

            val basicResearchBackup = backup.copy {
                farms = listOf(farm.copy {
                    commonResearch = CommonResearch.toResearchItems(
                        mapOf(CommonResearch.COMFORTABLE_NESTS to 10)
                    )
                })
            }

            it("correctly applies bonuses") {
                TODO()
            }
        }
    }
})
