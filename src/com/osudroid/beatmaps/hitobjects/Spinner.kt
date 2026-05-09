package com.osudroid.beatmaps.hitobjects

import com.osudroid.GameMode
import com.osudroid.beatmaps.EmptyHitWindow
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.math.Vector2
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
    override val difficultyStackedPosition
        get() = position

    override val difficultyStackedEndPosition
        get() = position

    override val gameplayStackedPosition
        get() = position

    override val gameplayStackedEndPosition
        get() = position

    override val screenSpaceGameplayStackedPosition
        get() = screenSpaceGameplayPosition

    override val screenSpaceGameplayStackedEndPosition
        get() = screenSpaceGameplayPosition

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

    override fun createHitWindow(mode: GameMode) = EmptyHitWindow()

    companion object {
        private val baseSpinnerSpinSample = BankHitSampleInfo("spinnerspin")
        private val baseSpinnerBonusSample = BankHitSampleInfo("spinnerbonus")
    }
}
