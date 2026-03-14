package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.IBeatmap
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * A class for specifying parameters for osu!standard performance calculation.
 */
class StandardPerformanceCalculationParameters : PerformanceCalculationParameters() {
    override fun populate(beatmap: IBeatmap, stat: StatisticV2) {
        super.populate(beatmap, stat)

        comboBreakingSliderNestedMisses = if (stat.sliderTickHits >= 0 && stat.sliderRepeatHits >= 0) {
            beatmap.hitObjects.sliderTickCount + beatmap.hitObjects.sliderRepeatCount -
                    (stat.sliderTickHits + stat.sliderRepeatHits)
        } else null
    }
}