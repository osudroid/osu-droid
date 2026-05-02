package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.IBeatmap
import com.rian.osu.replay.SliderCheesePenalty
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * A class for specifying parameters for osu!droid performance calculation.
 */
class DroidPerformanceCalculationParameters : PerformanceCalculationParameters() {
    /**
     * The tap penalty to apply for penalized scores.
     */
    @JvmField
    var tapPenalty = 1.0

    /**
     * The slider cheese penalties to apply for penalized scores.
     */
    @JvmField
    var sliderCheesePenalty = SliderCheesePenalty()

    /**
     * The total score achieved.
     */
    @JvmField
    var totalScore = 0

    override fun populate(beatmap: IBeatmap, stat: StatisticV2) {
        super.populate(beatmap, stat)

        totalScore = stat.totalScoreWithMultiplier

        comboBreakingSliderNestedMisses = if (stat.sliderHeadHits >= 0 && stat.sliderTickHits >= 0 && stat.sliderRepeatHits >= 0) {
            beatmap.hitObjects.sliderCount + beatmap.hitObjects.sliderTickCount + beatmap.hitObjects.sliderRepeatCount -
                    (stat.sliderHeadHits + stat.sliderTickHits + stat.sliderRepeatHits)
        } else null
    }
}