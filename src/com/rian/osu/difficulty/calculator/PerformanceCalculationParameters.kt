package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.IBeatmap
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.sliderobject.SliderRepeat
import com.rian.osu.beatmap.hitobject.sliderobject.SliderTail
import com.rian.osu.beatmap.hitobject.sliderobject.SliderTick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayObjectData
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * A class for specifying parameters for performance calculation.
 */
open class PerformanceCalculationParameters(
    /**
     * The maximum combo achieved.
     */
    @JvmField
    var maxCombo: Int = 0,

    /**
     * The amount of 300 (great) hits achieved.
     */
    @JvmField
    var countGreat: Int = 0,

    /**
     * The amount of 100 (ok) hits achieved.
     */
    @JvmField
    var countOk: Int = 0,

    /**
     * The amount of 50 (meh) hits achieved.
     */
    @JvmField
    var countMeh: Int = 0,

    /**
     * The amount of misses achieved.
     */
    @JvmField
    var countMiss: Int = 0,

    /**
     * The amount of slider nested object misses that do not break combo.
     */
    @JvmField
    var nonComboBreakingSliderNestedMisses: Int? = null,

    /**
     * The amount of slider nested object misses that break combo.
     */
    @JvmField
    var comboBreakingSliderNestedMisses: Int? = null
) {
    /**
     * Whether this score uses classic slider calculation.
     */
    val usingClassicSliderCalculation
        get() = nonComboBreakingSliderNestedMisses == null || comboBreakingSliderNestedMisses == null

    /**
     * Populates this [PerformanceCalculationParameters] with the given [IBeatmap] and [StatisticV2].
     *
     * @param beatmap The [IBeatmap] to populate this [PerformanceCalculationParameters] with.
     * @param stat The [StatisticV2] to populate this [PerformanceCalculationParameters]
     */
    open fun populate(beatmap: IBeatmap, stat: StatisticV2) {
        maxCombo = stat.getScoreMaxCombo()
        countGreat = stat.hit300
        countOk = stat.hit100
        countMeh = stat.hit50
        countMiss = stat.misses

        nonComboBreakingSliderNestedMisses =
            if (stat.sliderEndHits >= 0) beatmap.hitObjects.sliderCount - stat.sliderEndHits else null
    }

    /**
     * Populates parameters related to slider nested object misses using the given [IBeatmap] and [ReplayObjectData]s.
     *
     * @param beatmap The [IBeatmap] to populate this [PerformanceCalculationParameters] with.
     * @param replayObjectData The array of [ReplayObjectData]s to populate this [PerformanceCalculationParameters] with.
     * @param scope The [CoroutineScope] to use for coroutines.
     */
    fun populateNestedSliderObjectParameters(
        beatmap: IBeatmap,
        replayObjectData: Array<ReplayObjectData>,
        scope: CoroutineScope? = null
    ) {
        nonComboBreakingSliderNestedMisses = 0
        comboBreakingSliderNestedMisses = 0

        val objects = beatmap.hitObjects.objects

        for (i in objects.indices) {
            scope?.ensureActive()

            val obj = objects[i] as? Slider ?: continue
            val objData = replayObjectData.getOrNull(i)

            if (objData?.tickSet == null) {
                // No object data - assume all slider ticks and the end were dropped.
                nonComboBreakingSliderNestedMisses = (nonComboBreakingSliderNestedMisses ?: 0) + 1
                comboBreakingSliderNestedMisses = (comboBreakingSliderNestedMisses ?: 0) + obj.nestedHitObjects.size - 2 - obj.repeatCount
                continue
            }

            for (j in 1 until obj.nestedHitObjects.size) {
                scope?.ensureActive()

                if (objData.tickSet[j - 1]) {
                    continue
                }

                when (obj.nestedHitObjects[j]) {
                    is SliderTick, is SliderRepeat ->
                        comboBreakingSliderNestedMisses = (comboBreakingSliderNestedMisses ?: 0) + 1

                    is SliderTail -> nonComboBreakingSliderNestedMisses = (nonComboBreakingSliderNestedMisses ?: 0) + 1
                }
            }
        }
    }
}