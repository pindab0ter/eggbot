package nl.pindab0ter.eggbot.utilities

import com.auxbrain.ei.*
import java.math.BigDecimal

val Backup.Game.soulBonus: Int get() = epicResearch.find { it.id == "soul_eggs" }!!.level
val Backup.Game.prophecyBonus: Int get() = epicResearch.find { it.id == "prophecy_bonus" }!!.level
val Contract.finalGoal: BigDecimal get() = BigDecimal(goals.maxByOrNull { it.targetAmount }!!.targetAmount)
val LocalContract.finalGoal: BigDecimal get() = contract!!.finalGoal
val LocalContract.finished: Boolean get() = BigDecimal(lastAmountWhenRewardGiven) > contract?.finalGoal
val CoopStatusResponse.eggsLaid: BigDecimal get() = BigDecimal(totalAmount)
fun List<Backup>.findContract(contractId: String): LocalContract? = filter { backup ->
    backup.contracts?.contracts?.plus(backup.contracts.archive)?.any { contract ->
        contract.contract?.id == contractId
    } == true
}.maxByOrNull { backup -> backup.approxTime }?.let { backup ->
    backup.contracts?.contracts?.plus(backup.contracts.archive)?.find { contract ->
        contract.contract?.id == contractId
    }
}

// @formatter:off
val HabLevel.capacity: BigDecimal get() = when(this) {
    HabLevel.NO_HAB ->                        BigDecimal.ZERO
    HabLevel.COOP ->                     BigDecimal(250)
    HabLevel.SHACK ->                    BigDecimal(500)
    HabLevel.SUPER_SHACK ->            BigDecimal(1_000)
    HabLevel.SHORT_HOUSE ->            BigDecimal(2_000)
    HabLevel.THE_STANDARD ->           BigDecimal(5_000)
    HabLevel.LONG_HOUSE ->            BigDecimal(10_000)
    HabLevel.DOUBLE_DECKER ->         BigDecimal(20_000)
    HabLevel.WAREHOUSE ->             BigDecimal(50_000)
    HabLevel.CENTER ->               BigDecimal(100_000)
    HabLevel.BUNKER ->               BigDecimal(200_000)
    HabLevel.EGGKEA ->               BigDecimal(500_000)
    HabLevel.HAB_1000 ->           BigDecimal(1_000_000)
    HabLevel.HANGAR ->             BigDecimal(2_000_000)
    HabLevel.TOWER ->              BigDecimal(5_000_000)
    HabLevel.HAB_10_000 ->        BigDecimal(10_000_000)
    HabLevel.EGGTOPIA ->          BigDecimal(25_000_000)
    HabLevel.MONOLITH ->          BigDecimal(50_000_000)
    HabLevel.PLANET_PORTAL ->    BigDecimal(100_000_000)
    HabLevel.CHICKEN_UNIVERSE -> BigDecimal(600_000_000)
    else ->                                   BigDecimal.ZERO
}

val VehicleType.capacity: BigDecimal get() = when (this) {
    VehicleType.TRIKE ->                    BigDecimal(5_000)
    VehicleType.TRANSIT ->                 BigDecimal(15_000)
    VehicleType.PICKUP ->                  BigDecimal(50_000)
    VehicleType.VEHICLE_10_FOOT ->        BigDecimal(100_000)
    VehicleType.VEHICLE_24_FOOT ->        BigDecimal(250_000)
    VehicleType.SEMI ->                   BigDecimal(500_000)
    VehicleType.DOUBLE_SEMI ->          BigDecimal(1_000_000)
    VehicleType.FUTURE_SEMI ->          BigDecimal(5_000_000)
    VehicleType.MEGA_SEMI ->           BigDecimal(15_000_000)
    VehicleType.HOVER_SEMI ->          BigDecimal(30_000_000)
    VehicleType.QUANTUM_TRANSPORTER -> BigDecimal(50_000_000)
    VehicleType.HYPERLOOP_TRAIN ->     BigDecimal(50_000_000)
    else ->                                        BigDecimal.ZERO
}

// @formatter:on