package nl.pindab0ter.eggbot.utilities

import com.auxbrain.ei.EggInc
import java.math.BigDecimal

val EggInc.Backup.Game.soulBonus: Int get() = epicResearchList.find { it.id == "soul_eggs" }!!.level
val EggInc.Backup.Game.prophecyBonus: Int get() = epicResearchList.find { it.id == "prophecy_bonus" }!!.level
val EggInc.Backup.Simulation.habPopulation: List<BigDecimal> get() = habPopulationList.map { it.toBigDecimal() }
val EggInc.Contract.finalGoal: BigDecimal get() = BigDecimal(goalsList.maxBy { it.targetAmount }!!.targetAmount)
val EggInc.LocalContract.finalGoal: BigDecimal get() = contract.finalGoal
val EggInc.LocalContract.finished: Boolean get() = BigDecimal(lastAmountWhenRewardGiven) > contract.finalGoal
val EggInc.CoopStatusResponse.eggsLaid: BigDecimal get() = BigDecimal(totalAmount)
fun List<EggInc.Backup>.findContract(contractId: String): EggInc.LocalContract? = filter { backup ->
    backup.contracts.contractsList.plus(backup.contracts.archiveList).any { contract ->
        contract.contract.id == contractId
    }
}.maxBy { backup -> backup.approxTime }?.let { backup ->
    backup.contracts.contractsList.plus(backup.contracts.archiveList).find { contract ->
        contract.contract.id == contractId
    }
}

// @formatter:off
val EggInc.HabLevel.capacity: BigDecimal get() = when(this) {
    EggInc.HabLevel.NO_HAB ->                      BigDecimal.ZERO
    EggInc.HabLevel.COOP ->                     BigDecimal(250)
    EggInc.HabLevel.SHACK ->                    BigDecimal(500)
    EggInc.HabLevel.SUPER_SHACK ->            BigDecimal(1_000)
    EggInc.HabLevel.SHORT_HOUSE ->            BigDecimal(2_000)
    EggInc.HabLevel.THE_STANDARD ->           BigDecimal(5_000)
    EggInc.HabLevel.LONG_HOUSE ->            BigDecimal(10_000)
    EggInc.HabLevel.DOUBLE_DECKER ->         BigDecimal(20_000)
    EggInc.HabLevel.WAREHOUSE ->             BigDecimal(50_000)
    EggInc.HabLevel.CENTER ->               BigDecimal(100_000)
    EggInc.HabLevel.BUNKER ->               BigDecimal(200_000)
    EggInc.HabLevel.EGGKEA ->               BigDecimal(500_000)
    EggInc.HabLevel.HAB_1000 ->           BigDecimal(1_000_000)
    EggInc.HabLevel.HANGAR ->             BigDecimal(2_000_000)
    EggInc.HabLevel.TOWER ->              BigDecimal(5_000_000)
    EggInc.HabLevel.HAB_10_000 ->        BigDecimal(10_000_000)
    EggInc.HabLevel.EGGTOPIA ->          BigDecimal(25_000_000)
    EggInc.HabLevel.MONOLITH ->          BigDecimal(50_000_000)
    EggInc.HabLevel.PLANET_PORTAL ->    BigDecimal(100_000_000)
    EggInc.HabLevel.CHICKEN_UNIVERSE -> BigDecimal(600_000_000)
    EggInc.HabLevel.UNRECOGNIZED ->                BigDecimal.ZERO
}

val EggInc.VehicleType.capacity: BigDecimal get() = when (this) {
    EggInc.VehicleType.UNRECOGNIZED ->                  BigDecimal.ZERO
    EggInc.VehicleType.TRIKE ->                    BigDecimal(5_000)
    EggInc.VehicleType.TRANSIT ->                 BigDecimal(15_000)
    EggInc.VehicleType.PICKUP ->                  BigDecimal(50_000)
    EggInc.VehicleType.VEHICLE_10_FOOT ->        BigDecimal(100_000)
    EggInc.VehicleType.VEHICLE_24_FOOT ->        BigDecimal(250_000)
    EggInc.VehicleType.SEMI ->                   BigDecimal(500_000)
    EggInc.VehicleType.DOUBLE_SEMI ->          BigDecimal(1_000_000)
    EggInc.VehicleType.FUTURE_SEMI ->          BigDecimal(5_000_000)
    EggInc.VehicleType.MEGA_SEMI ->           BigDecimal(15_000_000)
    EggInc.VehicleType.HOVER_SEMI ->          BigDecimal(30_000_000)
    EggInc.VehicleType.QUANTUM_TRANSPORTER -> BigDecimal(50_000_000)
    EggInc.VehicleType.HYPERLOOP_TRAIN ->     BigDecimal(50_000_000)
}

// @formatter:on