package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.Backup
import com.auxbrain.ei.Contract
import com.auxbrain.ei.HabLevel.NO_HAB
import com.auxbrain.ei.HabLevel.SHACK
import com.auxbrain.ei.LocalContract
import com.auxbrain.ei.MyContracts
import com.auxbrain.ei.VehicleType.TRIKE
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import java.math.BigDecimal

class ContractSimulationTest : DescribeSpec({

    val farm = Backup.Simulation {
        commonResearch = CommonResearch.toResearchItems()
        habs = listOf(SHACK, NO_HAB, NO_HAB, NO_HAB)
        habPopulation = listOf(0L, 0L, 0L, 0L)
        vehicles = listOf(TRIKE)
    }

    val localContract = LocalContract {
        contract = Contract {
            goals = listOf(
                Contract.Goal { targetAmount = 1000.0 }
            )
        }
    }

    val backup = Backup {
        contracts = MyContracts { contracts = listOf(localContract) }
        farms = listOf(farm)
        game = Backup.Game { epicResearch = EpicResearch.toResearchItems() }
    }
    lateinit var simulation: ContractSimulation

    describe("A Contract Simulation") {
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
                simulation.currentPopulation.shouldBeEqualComparingTo(BigDecimal.ONE)
            }

            it("produces two eggs per minute") {
                simulation.step()
                simulation.projectedEggs.shouldBeEqualComparingTo(BigDecimal("2"))
            }
        }
    }
})
