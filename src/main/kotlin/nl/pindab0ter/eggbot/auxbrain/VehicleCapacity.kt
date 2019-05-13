package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import com.auxbrain.ei.EggInc.VehicleType.*
import java.math.BigDecimal

// @formatter:off
val EggInc.VehicleType.capacity: BigDecimal get() = when (this) {
    UNRECOGNIZED         -> BigDecimal.ZERO
    TRIKE                -> BigDecimal(5000)
    TRANSIT              -> BigDecimal(15000)
    PICKUP               -> BigDecimal(50000)
    VEHICLE_10_FOOT      -> BigDecimal(100000)
    VEHICLE_24_FOOT      -> BigDecimal(250000)
    SEMI                 -> BigDecimal(500000)
    DOUBLE_SEMI          -> BigDecimal(1000000)
    FUTURE_SEMI          -> BigDecimal(5000000)
    MEGA_SEMI            -> BigDecimal(15000000)
    HOVER_SEMI           -> BigDecimal(30000000)
    QUANTUM_TRANSPORTER  -> BigDecimal(50000000)
}
// @formatter:on
