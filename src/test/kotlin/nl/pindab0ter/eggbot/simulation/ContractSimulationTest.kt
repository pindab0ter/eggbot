package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc.*
import com.auxbrain.ei.EggInc.Egg.*
import com.auxbrain.ei.EggInc.HabLevel.*
import com.auxbrain.ei.EggInc.VehicleType.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import java.math.BigDecimal

class ContractSimulationTest : DescribeSpec({

    val researchBuilder = Backup.ResearchItem.newBuilder()
    val farm = Backup.Simulation.newBuilder()
        .addAllCommonResearch(CommonResearch.values().map { commonResearch ->
            researchBuilder.setId(commonResearch.id).setLevel(0).build()
        })
        .addAllHabs(listOf(SHACK, NO_HAB, NO_HAB, NO_HAB))
        .addAllHabPopulation(listOf(0L, 0L, 0L, 0L))
        .addAllVehicles(listOf(TRIKE))
        .build()
    val goal = Contract.Goal.newBuilder().setTargetAmount(1000.0).build()
    val contract = Contract.newBuilder().setEgg(EDIBLE).addGoals(goal).build()
    val localContract = LocalContract.newBuilder().setContract(contract).build()
    val myContracts = MyContracts.newBuilder().addContracts(localContract).build()
    val game = Backup.Game.newBuilder()
        .addAllEpicResearch(EpicResearch.values().map { epicResearch ->
            researchBuilder.setId(epicResearch.id).setLevel(0).build()
        }).build()
    val backup = Backup.newBuilder()
        .setContracts(myContracts)
        .addFarms(farm)
        .setGame(game)
        .build()

    lateinit var simulation: ContractSimulation

    describe("Contract simulation") {
        context("with one chicken") {
            val oneChickenBackup = backup.toBuilder().apply {
                farmsBuilderList.first().addAllHabPopulation(listOf(1L, 0L, 0L, 0L))
            }.build()

            beforeTest {
                simulation = ContractSimulation(
                    oneChickenBackup,
                    oneChickenBackup.getFarms(0),
                    oneChickenBackup.contracts.getContracts(0)
                )
            }

            it("has one chicken") {
                simulation.currentPopulation.shouldBeEqualComparingTo(BigDecimal.ONE)
            }

            it("lays a certain amount of eggs per minute") {
                simulation.step()
                simulation.projectedEggs.shouldBeEqualComparingTo(BigDecimal("2"))
            }
        }
    }
})