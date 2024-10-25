package com.rian.osu.beatmap.hitobject

import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.math.Vector2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents a spinner.
 */
class Spinner(
    /**
     * The time at which this [Spinner] starts, in milliseconds.
     */
    startTime: Double,

    override val endTime: Double,

    /**
     * Whether this [Spinner] starts a new combo.
     */
    isNewCombo: Boolean,
) : HitObject(startTime, Vector2(256f, 192f), isNewCombo, 0) {
    override val difficultyStackedPosition = position
    override val difficultyStackedEndPosition = position

    override val gameplayStackedPosition = gameplayPosition
    override val gameplayStackedEndPosition = gameplayPosition

    override fun applySamples(controlPoints: BeatmapControlPoints, scope: CoroutineScope?) {
        super.applySamples(controlPoints, scope)

        val samplePoints = controlPoints.sample.between(startTime + CONTROL_POINT_LENIENCY, endTime + CONTROL_POINT_LENIENCY)

        auxiliarySamples.clear()

        auxiliarySamples.add(SequenceHitSampleInfo(
            samplePoints.map {
                scope?.ensureActive()

                it.time to it.applyTo(baseSpinnerSpinSample)
            }
        ))

        auxiliarySamples.add(SequenceHitSampleInfo(
            samplePoints.map {
                scope?.ensureActive()

                it.time to it.applyTo(baseSpinnerBonusSample)
            }
        ))
    }

    companion object {
        private val baseSpinnerSpinSample = BankHitSampleInfo("spinnerspin")
        private val baseSpinnerBonusSample = BankHitSampleInfo("spinnerbonus")
    }
}
