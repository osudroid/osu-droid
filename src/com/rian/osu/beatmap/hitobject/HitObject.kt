package com.rian.osu.beatmap.hitobject

import com.rian.osu.GameMode
import com.rian.osu.beatmap.constants.SampleBank
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import com.rian.osu.utils.CircleSizeCalculator
import kotlin.math.min

/**
 * Represents a hit object.
 */
abstract class HitObject(
    /**
     * The time at which this [HitObject] starts, in milliseconds.
     */
    @JvmField
    var startTime: Double,

    /**
     * The position of this [HitObject].
     */
    @JvmField
    var position: Vector2,

    /**
     * Whether this [HitObject] starts a new combo.
     */
    @JvmField
    val isNewCombo: Boolean = false,

    /**
     * How many combo colors to skip, if this [HitObject] starts a new combo.
     */
    @JvmField
    val comboColorOffset: Int = 0
) {
    /**
     * The stack height of this [HitObject].
     */
    @JvmField
    var stackHeight = 0

    /**
     * The osu!standard scale of this [HitObject].
     */
    open var scale = 0f

    /**
     * The time at which the approach circle of this [HitObject] should appear before [startTime].
     */
    @JvmField
    var timePreempt = 600.0

    /**
     * The time at which this [HitObject] should fade after this [HitObject] appears with respect to [timePreempt].
     */
    @JvmField
    var timeFadeIn = 400.0

    /**
     * The samples to be played when this [HitObject] is hit.
     *
     * In the case of [Slider]s, this is the sample of the curve body
     * and can be treated as the default samples for the [HitObject].
     */
    var samples = mutableListOf<HitSampleInfo>()

    /**
     * Any samples which may be used by this [HitObject] that are non-standard.
     */
    var auxiliarySamples = mutableListOf<HitSampleInfo>()

    /**
     * Whether this [HitObject] is in kiai time.
     */
    @JvmField
    var kiai: Boolean = false

    /**
     * The radius of this [HitObject].
     */
    val radius
        get() = (OBJECT_RADIUS * scale).toDouble()

    /**
     * Gets the stacked position of this [HitObject].
     *
     * @param mode The [GameMode] to calculate for.
     */
    open fun getStackedPosition(mode: GameMode) = evaluateStackedPosition(position, mode)

    /**
     * Applies defaults to this [HitObject].
     *
     * @param controlPoints The control points.
     * @param difficulty The difficulty settings to use.
     * @param mode The [GameMode] to use.
     */
    open fun applyDefaults(controlPoints: BeatmapControlPoints, difficulty: BeatmapDifficulty, mode: GameMode) {
        kiai = controlPoints.effect.controlPointAt(startTime + CONTROL_POINT_LENIENCY).isKiai

        timePreempt = BeatmapDifficulty.difficultyRange(difficulty.ar.toDouble(), 1800.0, 1200.0, PREEMPT_MIN)

        // Preempt time can go below 450ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
        // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear function above.
        // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
        // This adjustment is necessary for AR>10, otherwise timePreempt can become smaller leading to hit circles not fully fading in.
        timeFadeIn = 400 * min(1.0, timePreempt / PREEMPT_MIN)

        scale = when (mode) {
            GameMode.Droid -> CircleSizeCalculator.standardCSToDroidScale(difficulty.cs)
            GameMode.Standard -> CircleSizeCalculator.standardCSToStandardScale(difficulty.cs)
        }
    }

    /**
     * Applies samples to this [HitObject].
     *
     * @param controlPoints The control points.
     */
    open fun applySamples(controlPoints: BeatmapControlPoints) {
        val sampleControlPoint = controlPoints.sample.controlPointAt(getEndTime() + CONTROL_POINT_LENIENCY)

        samples = samples.map { sampleControlPoint.applyTo(it) }.toMutableList()
    }

    /**
     * Evaluates a stacked position relative to this [HitObject].
     *
     * @param mode The [GameMode] to evaluate for.
     * @return The evaluated stacked position.
     */
    protected fun evaluateStackedPosition(position: Vector2, mode: GameMode) = position + getStackOffset(mode)

    /**
     * Creates a [BankHitSampleInfo] based on the sample settings of the first [BankHitSampleInfo.HIT_NORMAL] sample in [samples].
     * If no sample is available, sane default settings will be used instead.
     *
     * In the case an existing sample exists, all settings apart from the sample name will be inherited. This includes volume and bank.
     *
     * @param sampleName The name of the sample.
     * @return A populated [BankHitSampleInfo].
     */
    protected fun createHitSampleInfo(sampleName: String) =
        samples.filterIsInstance<BankHitSampleInfo>().find { it.name == BankHitSampleInfo.HIT_NORMAL }?.copy(name = sampleName) ?:
        BankHitSampleInfo(sampleName, SampleBank.None)

    private fun getStackOffset(mode: GameMode) = when (mode) {
        GameMode.Droid -> Vector2(stackHeight * scale * 4f)
        GameMode.Standard -> Vector2(stackHeight * scale * -6.4f)
    }

    companion object {
        /**
         * The radius of hit objects (i.e. the radius of a circle) relative to osu!standard.
         */
        const val OBJECT_RADIUS = 64f

        /**
         * A small adjustment to the start time of control points to account for rounding/precision errors.
         */
        internal const val CONTROL_POINT_LENIENCY = 5

        /**
         * Minimum preempt time at AR=10.
         */
        const val PREEMPT_MIN = 450.0
    }
}
