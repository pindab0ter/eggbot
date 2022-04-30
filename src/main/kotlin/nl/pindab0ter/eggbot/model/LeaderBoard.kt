package nl.pindab0ter.eggbot.model

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import java.util.*

enum class LeaderBoard : ChoiceEnum {
    EARNINGS_BONUS, SOUL_EGGS, PROPHECY_EGGS, PRESTIGES, DRONE_TAKEDOWNS, ELITE_DRONE_TAKEDOWNS;

    override val readableName: String
        get() = name.split("_").joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar { letter -> letter.titlecase(Locale.getDefault()) }
        }
}
