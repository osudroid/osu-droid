package com.osudroid.difficulty.calculator

import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * A class for specifying parameters for osu!standard performance calculation.
 */
class StandardPerformanceCalculationParameters : PerformanceCalculationParameters() {
    override fun populate(stat: StatisticV2, sliderCount: Int, sliderTickCount: Int, sliderRepeatCount: Int) {
        super.populate(stat, sliderCount, sliderTickCount, sliderRepeatCount)

        comboBreakingSliderNestedMisses = if (stat.sliderTickHits >= 0 && stat.sliderRepeatHits >= 0) {
            sliderTickCount + sliderRepeatCount - (stat.sliderTickHits + stat.sliderRepeatHits)
        } else null
    }
}