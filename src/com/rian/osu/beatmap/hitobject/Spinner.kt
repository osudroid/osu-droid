package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2

/**
 * Represents a spinner.
 */
class Spinner(
    /**
     * The time at which this [Spinner] starts, in milliseconds.
     */
    startTime: Double,

    override var endTime: Double,

    /**
     * Whether this [Spinner] starts a new combo.
     */
    isNewCombo: Boolean,
) : HitObject(startTime, Vector2(256f, 192f), isNewCombo, 0), IHasDuration {
    init {
        auxiliarySamples.apply {
            samples.filterIsInstance<BankHitSampleInfo>().firstOrNull()?.let { add(it.copy(name = "spinnerspin")) }

            add(createHitSampleInfo("spinnerbonus"))
        }
    }

    override val difficultyStackedPosition = position

    override val gameplayStackedPosition = position

    override val duration: Double
        get() = endTime - startTime
}
