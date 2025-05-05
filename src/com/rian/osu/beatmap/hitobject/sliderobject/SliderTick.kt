package com.rian.osu.beatmap.hitobject.sliderobject

import com.rian.osu.GameMode
import com.rian.osu.beatmap.EmptyHitWindow
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import kotlinx.coroutines.CoroutineScope

/**
 * Represents a slider tick.
 */
class SliderTick(
    /**
     * The time at which this [SliderTick] starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this [SliderTick] relative to the play field.
     */
    position: Vector2,

    /**
     * The index of the span at which this [SliderTick] lies.
     */
    private val spanIndex: Int,

    /**
     * The start time of the span at which this [SliderTick] lies, in milliseconds.
     */
    private val spanStartTime: Double
) : SliderHitObject(startTime, position) {
    override fun applyDefaults(
        controlPoints: BeatmapControlPoints,
        difficulty: BeatmapDifficulty,
        mode: GameMode,
        scope: CoroutineScope?
    ) {
        super.applyDefaults(controlPoints, difficulty, mode, scope)

        // Adding 200 to include the offset stable used.
        // This is so on repeats ticks don't appear too late to be visually processed by the player.
        val offset = if (spanIndex > 0) 200.0 else timePreempt * 0.66

        timePreempt = (startTime - spanStartTime) / 2 + offset
    }

    override fun createHitWindow(mode: GameMode) = EmptyHitWindow()
}
