package nl.pindab0ter.eggbot.utilities

import com.martiansoftware.jsap.JSAP.REQUIRED
import com.martiansoftware.jsap.Parameter
import com.martiansoftware.jsap.Switch
import com.martiansoftware.jsap.UnflaggedOption
import nl.pindab0ter.eggbot.Config
import nl.pindab0ter.eggbot.commands.ContractIDs

const val COMPACT: String = "compact"
const val CONTRACT_ID: String = "contract id"
const val COOP_ID: String = "co-op id"
const val NO_ROLE: String = "no role"

val compactSwitch: Parameter = Switch(COMPACT)
    .setShortFlag('c')
    .setLongFlag("compact")
    .setHelp("Use a narrower output to better fit mobile devices.")
val contractIdOption: Parameter = UnflaggedOption(CONTRACT_ID)
    .setRequired(REQUIRED)
    .setHelp("The contract ID. Can be found using ${Config.prefix}${ContractIDs.name}.")
val coopIdOption: Parameter = UnflaggedOption(COOP_ID)
    .setRequired(REQUIRED)
    .setHelp("The co-op ID. Can be found in either `#roll-call` or in-game under \"CO-OP INFO\" in the current egg information screen.")
