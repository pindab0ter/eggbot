package nl.pindab0ter.eggbot.auxbrain

import com.auxbrain.ei.EggInc
import com.auxbrain.ei.EggInc.VehicleType.*

// @formatter:off
val EggInc.VehicleType.capacity get() = when (this) {
    UNRECOGNIZED         -> 0
    TRIKE                -> 5000
    TRANSIT              -> 15000
    PICKUP               -> 50000
    VEHICLE_10_FOOT      -> 100000
    VEHICLE_24_FOOT      -> 250000
    SEMI                 -> 500000
    DOUBLE_SEMI          -> 1000000
    FUTURE_SEMI          -> 5000000
    MEGA_SEMI            -> 15000000
    HOVER_SEMI           -> 30000000
    QUANTUM_TRANSPORTER  -> 50000000
}
// @formatter:on
