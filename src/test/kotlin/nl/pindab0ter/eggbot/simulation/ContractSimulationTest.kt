package nl.pindab0ter.eggbot.simulation

import com.auxbrain.ei.EggInc
import com.auxbrain.ei.EggInc.Backup
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class ContractSimulationTest : DescribeSpec({

    lateinit var contractSimulation: ContractSimulation

    val farm = Backup.Simulation.newBuilder()
        .setEgg(EggInc.Egg.EDIBLE)
        .setContractId(CONTRACT_ID)
        .addAllHabPopulation(listOf(0L, 0L, 0L, 0L))
        .build()
    val goal = EggInc.Contract.Goal.newBuilder()
        .setTargetAmount(1000.0)
    val contract = EggInc.Contract.newBuilder()
        .addGoals(goal)
    val localContract = EggInc.LocalContract.newBuilder()
        .setContract(contract)
        .build()
    val myContracts = EggInc.MyContracts.newBuilder()
        .addContracts(localContract)
    val backup = Backup.newBuilder()
        .setContracts(myContracts)
        .addFarms(farm)
        .build()

    describe("Contract simulation") {
        context("with one chicken") {
            val oneChickenBackup = backup
                .toBuilder().apply {
                    farmsBuilderList.first().addAllHabPopulation(listOf(1L, 0L, 0L, 0L))
                }.build()

            beforeTest {
                contractSimulation = ContractSimulation(
                    oneChickenBackup,
                    oneChickenBackup.getFarms(0),
                    oneChickenBackup.contracts.getContracts(0)
                )
            }

            it("has one chicken") {
                contractSimulation.currentPopulation.shouldBe(BigDecimal.ONE)
            }
        }
    }
}) {
    companion object {
        const val CONTRACT_ID = "test_contract"
        const val CONTRACT_NAME = "Test Contract"
        val EGG = EggInc.Egg.EDIBLE
    }
}