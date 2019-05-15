package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import com.auxbrain.ei.EggInc.HabLevel.*
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

// @formatter:off
val EggInc.HabLevel.capacity: BigDecimal get() = when(this) {
    NO_HAB ->                            ZERO
    COOP ->                   BigDecimal(250)
    SHACK ->                  BigDecimal(500)
    SUPER_SHACK ->           BigDecimal(1000)
    SHORT_HOUSE ->           BigDecimal(2000)
    THE_STANDARD ->          BigDecimal(5000)
    LONG_HOUSE ->           BigDecimal(10000)
    DOUBLE_DECKER ->        BigDecimal(20000)
    WAREHOUSE ->            BigDecimal(50000)
    CENTER ->              BigDecimal(100000)
    BUNKER ->              BigDecimal(200000)
    EGGKEA ->              BigDecimal(500000)
    HAB_1000 ->           BigDecimal(1000000)
    HANGAR ->             BigDecimal(2000000)
    TOWER ->              BigDecimal(5000000)
    HAB_10_000 ->        BigDecimal(10000000)
    EGGTOPIA ->          BigDecimal(25000000)
    MONOLITH ->          BigDecimal(50000000)
    PLANET_PORTAL ->    BigDecimal(100000000)
    CHICKEN_UNIVERSE -> BigDecimal(600000000)
    UNRECOGNIZED ->                      ZERO
}
// @formatter:on
