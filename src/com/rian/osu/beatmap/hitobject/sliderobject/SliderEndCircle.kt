package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import kotlinx.coroutines.CoroutineScope

/**
 * Represents a [SliderHitObject] that is at the end of a slider path.
 */
abstract class SliderEndCircle(
    /**
     * The [Slider] to which [SliderEndCircle] belongs to.
     */
    protected val slider: Slider,

    /**
     * The index of the span at which this [SliderEndCircle] lies.
     */
    val spanIndex: Int,

    /**
     * An optional start time to override this [SliderEndCircle]'s [startTime].
     */
    startTime: Double = slider.startTime + slider.spanDuration * (spanIndex + 1)
) : SliderHitObject(
    startTime,
    if (spanIndex % 2 == 0) slider.endPosition else slider.position
) {
    override fun applyDefaults(
        controlPoints: BeatmapControlPoints,
        difficulty: BeatmapDifficulty,
        mode: GameMode,
        scope: CoroutineScope?
    ) {
        super.applyDefaults(controlPoints, difficulty, mode, scope)

        if (spanIndex > 0) {
            // Repeat points after the first span should appear behind the still-visible one.
            timeFadeIn = 0.0

            // The next end circle should appear exactly after the previous circle (on the same end) is hit.
            timePreempt = slider.spanDuration * 2
        } else {
            // The first end circle should fade in with the slider.
            timePreempt += startTime - slider.startTime
        }
    }
}