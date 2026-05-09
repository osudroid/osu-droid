package com.osudroid.beatmaps.hitobjects.sliderobject

import com.osudroid.GameMode
import com.osudroid.beatmaps.EmptyHitWindow
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import kotlinx.coroutines.CoroutineScope

/**
 * Represents a [SliderHitObject] that is at the end of a [Slider]'s path.
 */
abstract class SliderEndCircle(
    /**
     * The [Slider] to which this [SliderEndCircle] belongs to.
     */
    protected val slider: Slider,

    /**
     * The index of the span at which this [SliderEndCircle] lies.
     */
    val spanIndex: Int
) : SliderHitObject(
    slider.startTime + slider.spanDuration * (spanIndex + 1),
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

    override fun createHitWindow(mode: GameMode) = EmptyHitWindow()
}