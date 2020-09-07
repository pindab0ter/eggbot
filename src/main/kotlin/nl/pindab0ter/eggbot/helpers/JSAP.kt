package nl.pindab0ter.eggbot.helpers

import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.JSAPResult
import com.martiansoftware.jsap.Switch
import com.martiansoftware.jsap.UnflaggedOption
import nl.pindab0ter.eggbot.controller.ContractIDs
import nl.pindab0ter.eggbot.model.Config

const val COMPACT: String = "compact"
const val EXTENDED: String = "extended"
const val CONTRACT_ID: String = "contract id"
const val COOP_ID: String = "co-op id"
const val NO_ROLE: String = "no role"
const val FORCE: String = "force"
const val FORCE_REPORTED_ONLY: String = "reported state"

val compactSwitch: Switch = Switch(COMPACT)
    .setShortFlag('c')
    .setLongFlag("compact")
    .setHelp("Use a narrower output to better fit mobile devices.") as Switch
val extendedSwitch: Switch = Switch(EXTENDED)
    .setShortFlag('e')
    .setLongFlag("extended")
    .setHelp("Show unshortened output.") as Switch
val contractIdOption: UnflaggedOption = UnflaggedOption(CONTRACT_ID)
    .setRequired(REQUIRED)
    .setHelp("The contract ID. Can be found using ${Config.prefix}${ContractIDs.name}.") as UnflaggedOption
val coopIdOption: UnflaggedOption = UnflaggedOption(COOP_ID)
    .setRequired(REQUIRED)
    .setHelp("The co-op ID. Can be found in either `#roll-call` or in-game under \"CO-OP INFO\" in the current egg information screen.") as UnflaggedOption
val forceReportedOnlySwitch: Switch = Switch(FORCE_REPORTED_ONLY)
    .setShortFlag('r')
    .setLongFlag("force-reported-only")
    .setHelp("Force working strictly with reported data instead of simulating everyone as if they are always recently updated.") as Switch

fun JSAPResult.getIntOrNull(id: String) = if (contains(id)) getInt(id) else null
fun JSAPResult.getStringOrNull(id: String) = if (contains(id)) getString(id) else null
