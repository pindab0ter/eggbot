package nl.pindab0ter.eggbot.model

import nl.pindab0ter.eggbot.model.database.Farmer
import java.math.BigDecimal


// bonusPerSoulEgg = (.1 + soulEggResearchLevel*.01)
// bonusPerProphecyEgg = (1.05 + prophecyEggResearchLevel*.01)
// prophecyBonus = int(bonusPerProphecyEgg*100)**PE/100**PE
// totalBonus = int(bonusPerSoulEgg * soulEggs * prophecyBonus)
// bonusPerEgg = bonusPerSoulEgg * prophecyBonus
// l = len(str(totalBonus)) or 1
// seToNextRank = math.ceil(10**(l) // (bonusPerProphecyEgg**PE * (.1 + soulEggResearchLevel*.01)))
// peToNextRank = math.ceil(math.log(10**l/((.1 + soulEggResearchLevel*.01)*soulEggs), (1.05 + prophecyEggResearchLevel*.01)))

fun soulEggsToNextRank(
    soulEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal,
    prophecyEggs: BigDecimal,
    prophecyEggsResearchLevel: BigDecimal,
): BigDecimal = BigDecimal.ONE

fun soulEggsToNextRank(farmer: Farmer): BigDecimal = soulEggsToNextRank(
    farmer.soulEggs,
    farmer.soulEggResearchLevel.toBigDecimal(),
    farmer.prophecyEggs.toBigDecimal(),
    farmer.prophecyEggResearchLevel.toBigDecimal(),
)

fun prophecyEggsToNextRank(
    soulEggs: BigDecimal,
    soulEggsResearchLevel: BigDecimal,
    prophecyEggs: BigDecimal,
    prophecyEggsResearchLevel: BigDecimal,
): BigDecimal = BigDecimal.ONE

fun prophecyEggsToNextRank(farmer: Farmer): BigDecimal = prophecyEggsToNextRank(
    farmer.soulEggs,
    farmer.soulEggResearchLevel.toBigDecimal(),
    farmer.prophecyEggs.toBigDecimal(),
    farmer.prophecyEggResearchLevel.toBigDecimal(),
)
