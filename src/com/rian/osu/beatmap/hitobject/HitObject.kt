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
     * The position of this [HitObject] in osu!pixels.
     */
    @JvmField
    var position: Vector2,

    /**
     * Whether this [HitObject] starts a new combo.
     */
    @JvmField
    val isNewCombo: Boolean,

    /**
     * When starting a new combo, the offset of the new combo relative to the current one.
     *
     * This is generally a setting provided by a beatmap creator to choreograph interesting color patterns
     * which can only be achieved by skipping combo colors with per-[HitObject] level.
     *
     * It is exposed via [comboIndexWithOffsets].
     */
    @JvmField
    val comboOffset: Int
) {
    /**
     * The index of this [HitObject] in the current combo.
     */
    var indexInCurrentCombo = 0
        private set

    /**
     * The index of this [HitObject]'s combo in relation to the beatmap.
     *
     * In other words, this is incremented by 1 each time an [isNewCombo] is reached.
     */
    var comboIndex = 0
        private set

    /**
     * The index of this [HitObject]'s combo in relation to the beatmap, with all aggregate s applied.
     */
    var comboIndexWithOffsets = 0
        private set

    /**
     * Whether this is the last [HitObject] in the current combo.
     */
    var lastInCombo = false
        internal set

    /**
     * The stack height of this [HitObject].
     */
    open var stackHeight = 0

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
    var kiai = false

    // Difficulty calculation object positions

    /**
     * The osu!standard scale of this [HitObject] in difficulty calculation.
     */
    open var difficultyScale = 0f

    /**
     * The radius of this [HitObject] in difficulty calculation, in osu!pixels.
     */
    val difficultyRadius
        get() = (OBJECT_RADIUS * difficultyScale).toDouble()

    /**
     * The stack offset of this [HitObject] in difficulty calculation, in osu!pixels.
     */
    open var difficultyStackOffset = Vector2(0f)

    /**
     * The stacked position of this [HitObject] in difficulty calculation, in osu!pixels.
     */
    open val difficultyStackedPosition
        get() = position + difficultyStackOffset

    // Gameplay object positions

    /**
     * The scale of this [HitObject] in gameplay.
     */
    open var gameplayScale = 0f

    /**
     * The radius of this [HitObject] in gameplay, in "gameplay" distance units.
     */
    val gameplayRadius
        get() = (OBJECT_RADIUS * gameplayScale).toDouble()

    /**
     * The stack offset of this [HitObject] in gameplay, in "gameplay" distance units.
     */
    open var gameplayStackOffset = Vector2(0f)

    /**
     * The stacked position of this [HitObject] in gameplay, in "gameplay" distance units.
     */
    open val gameplayStackedPosition
        get() = position + gameplayStackOffset

    /**
     * Applies defaults to this [HitObject].
     *
     * @param controlPoints The control points.
     * @param difficulty The difficulty settings to use.
     * @param mode The [GameMode] to use.
     */
    open fun applyDefaults(controlPoints: BeatmapControlPoints, difficulty: BeatmapDifficulty, mode: GameMode) {
        kiai = controlPoints.effect.controlPointAt(startTime + CONTROL_POINT_LENIENCY).isKiai

        timePreempt = BeatmapDifficulty.difficultyRange(difficulty.ar.toDouble(), PREEMPT_MAX, PREEMPT_MID, PREEMPT_MIN)

        // Preempt time can go below 450ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
        // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear function above.
        // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
        // This adjustment is necessary for AR>10, otherwise timePreempt can become smaller leading to hit circles not fully fading in.
        timeFadeIn = 400 * min(1.0, timePreempt / PREEMPT_MIN)

        difficultyScale = when (mode) {
            GameMode.Droid -> {
                val droidScale = CircleSizeCalculator.droidCSToDroidDifficultyScale(difficulty.cs)
                val radius = CircleSizeCalculator.droidScaleToStandardRadius(droidScale)
                val standardCS = CircleSizeCalculator.standardRadiusToStandardCS(radius, true)

                CircleSizeCalculator.standardCSToStandardScale(standardCS, true)
            }

            GameMode.Standard -> CircleSizeCalculator.standardCSToStandardScale(difficulty.cs, true)
        }

        difficultyStackOffset = when (mode) {
            GameMode.Droid -> Vector2(stackHeight * CircleSizeCalculator.standardScaleToDroidDifficultyScale(difficultyScale, true) * 4f)
            GameMode.Standard -> Vector2(stackHeight * difficultyScale * -6.4f)
        }

        gameplayScale = when (mode) {
            GameMode.Droid -> CircleSizeCalculator.droidCSToDroidGameplayScale(difficulty.cs)
            GameMode.Standard -> difficultyScale
        }

        gameplayStackOffset = when (mode) {
            GameMode.Droid -> Vector2(stackHeight * gameplayScale * 4f)
            GameMode.Standard -> difficultyStackOffset.copy()
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
     * Given the previous [HitObject] in the beatmap, update relevant combo information.
     */
    fun updateComboInformation(lastObj: HitObject?) {
        comboIndex = lastObj?.comboIndex ?: 0
        comboIndexWithOffsets = lastObj?.comboIndexWithOffsets ?: 0
        indexInCurrentCombo = if (lastObj != null) lastObj.indexInCurrentCombo + 1 else 0

        if (isNewCombo || lastObj == null) {
            indexInCurrentCombo = 0
            ++comboIndex
            comboIndexWithOffsets += comboOffset + 1

            if (lastObj != null) {
                lastObj.lastInCombo = true
            }
        }
    }

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
         * Maximum preempt time at AR=0.
         */
        const val PREEMPT_MAX = 1800.0

        /**
         * Median preempt time at AR=5.
         */
        const val PREEMPT_MID = 1200.0

        /**
         * Minimum preempt time at AR=10.
         */
        const val PREEMPT_MIN = 450.0
    }
}
