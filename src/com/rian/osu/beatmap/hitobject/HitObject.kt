package com.rian.osu.beatmap.hitobject

import com.rian.osu.GameMode
import com.rian.osu.beatmap.constants.SampleBank
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import com.rian.osu.utils.Cached
import com.rian.osu.utils.CircleSizeCalculator
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants

/**
 * Represents a hit object.
 */
abstract class HitObject(
    /**
     * The time at which this [HitObject] starts, in milliseconds.
     */
    @JvmField
    val startTime: Double,

    /**
     * The position of this [HitObject] in osu!pixels.
     */
    position: Vector2,

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
     * The end time of this [HitObject].
     */
    open val endTime
        get() = startTime

    /**
     * The duration of this [HitObject], in milliseconds.
     */
    val duration
        get() = endTime - startTime

    /**
     * The position of this [HitObject] in osu!pixels.
     */
    open var position = position
        set(value) {
            field = value

            difficultyStackedPositionCache.invalidate()
            gameplayPositionCache.invalidate()
            gameplayStackedPositionCache.invalidate()
        }

    /**
     * The end position of this [HitObject] in osu!pixels.
     */
    open val endPosition
        get() = position

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
     * The index of this [HitObject]'s combo in relation to the beatmap, with all aggregates applied.
     */
    var comboIndexWithOffsets = 0
        private set

    /**
     * Whether this is the last [HitObject] in the current combo.
     */
    var isLastInCombo = false
        internal set

    /**
     * The time at which the approach circle of this [HitObject] should appear before [startTime] in milliseconds.
     */
    @JvmField
    var timePreempt = 600.0

    /**
     * The time at which this [HitObject] should fade after this [HitObject] appears with respect to [timePreempt] in milliseconds.
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
    var auxiliarySamples = mutableListOf<SequenceHitSampleInfo>()

    /**
     * Whether this [HitObject] is in kiai time.
     */
    @JvmField
    var kiai = false

    /**
     * Whether this [HitObject] is the first [HitObject] in the beatmap.
     */
    val isFirstNote
        get() = comboIndex == 1 && indexInCurrentCombo == 0

    /**
     * The multiplier used to calculate stack offset.
     */
    open var stackOffsetMultiplier = 0f
        set(value) {
            field = value

            difficultyStackOffsetCache.invalidate()
            difficultyStackedPositionCache.invalidate()
            gameplayStackOffsetCache.invalidate()
            gameplayStackedPositionCache.invalidate()
        }

    // Difficulty calculation object positions

    /**
     * The stack height of this [HitObject] in difficulty calculation.
     */
    open var difficultyStackHeight = 0
        set(value) {
            field = value

            difficultyStackOffsetCache.invalidate()
            difficultyStackedPositionCache.invalidate()
        }

    /**
     * The osu!standard scale of this [HitObject] in difficulty calculation.
     */
    open var difficultyScale = 0f
        set(value) {
            field = value

            difficultyStackOffsetCache.invalidate()
            difficultyStackedPositionCache.invalidate()
        }

    /**
     * The radius of this [HitObject] in difficulty calculation, in osu!pixels.
     */
    val difficultyRadius
        get() = (OBJECT_RADIUS * difficultyScale).toDouble()

    private val difficultyStackOffsetCache = Cached(Vector2(0))

    /**
     * The stack offset of this [HitObject] in difficulty calculation, in osu!pixels.
     */
    open val difficultyStackOffset: Vector2
        get() {
            if (!difficultyStackOffsetCache.isValid) {
                difficultyStackOffsetCache.value =
                    Vector2(difficultyStackHeight * difficultyScale * stackOffsetMultiplier)
            }

            return difficultyStackOffsetCache.value
        }

    private val difficultyStackedPositionCache = Cached(position)

    /**
     * The stacked position of this [HitObject] in difficulty calculation, in osu!pixels.
     */
    open val difficultyStackedPosition: Vector2
        get() {
            if (!difficultyStackedPositionCache.isValid) {
                difficultyStackedPositionCache.value = position + difficultyStackOffset
            }

            return difficultyStackedPositionCache.value
        }

    /**
     * The stacked end position of this [HitObject] in difficulty calculation, in osu!pixels.
     */
    open val difficultyStackedEndPosition
        get() = difficultyStackedPosition

    // Gameplay object positions

    /**
     * The stack offset of this [HitObject] in gameplay.
     */
    open var gameplayStackHeight = 0
        set(value) {
            field = value

            gameplayStackOffsetCache.invalidate()
            gameplayStackedPositionCache.invalidate()
        }

    /**
     * The scale of this [HitObject] in gameplay.
     */
    open var gameplayScale = 0f
        set(value) {
            field = value

            gameplayStackOffsetCache.invalidate()
            gameplayStackedPositionCache.invalidate()
        }

    private val gameplayPositionCache = Cached(convertPositionToRealCoordinates(position))

    /**
     * The position of this [HitObject] in gameplay, in pixels.
     */
    val gameplayPosition: Vector2
        get() {
            if (!gameplayPositionCache.isValid) {
                gameplayPositionCache.value = convertPositionToRealCoordinates(position)
            }

            return gameplayPositionCache.value
        }

    /**
     * The end position of this [HitObject] in gameplay, in pixels.
     */
    open val gameplayEndPosition
        get() = gameplayPosition

    /**
     * The radius of this [HitObject] in gameplay, in pixels.
     */
    val gameplayRadius
        get() = (OBJECT_RADIUS * gameplayScale).toDouble()

    private val gameplayStackOffsetCache = Cached(Vector2(0))

    /**
     * The stack offset of this [HitObject] in gameplay, in pixels.
     */
    val gameplayStackOffset: Vector2
        get() {
            if (!gameplayStackOffsetCache.isValid) {
                gameplayStackOffsetCache.value = Vector2(gameplayStackHeight * gameplayScale * stackOffsetMultiplier)
            }

            return gameplayStackOffsetCache.value
        }

    private val gameplayStackedPositionCache = Cached(gameplayPosition)

    /**
     * The stacked position of this [HitObject] in gameplay, in pixels.
     */
    open val gameplayStackedPosition: Vector2
        get() {
            if (!gameplayStackedPositionCache.isValid) {
                gameplayStackedPositionCache.value = gameplayPosition + gameplayStackOffset
            }

            return gameplayStackedPositionCache.value
        }

    /**
     * The stacked end position of this [HitObject] in gameplay, in pixels.
     */
    open val gameplayStackedEndPosition
        get() = gameplayStackedPosition

    /**
     * Applies defaults to this [HitObject].
     *
     * @param controlPoints The control points.
     * @param difficulty The difficulty settings to use.
     * @param mode The [GameMode] to use.
     * @param scope The [CoroutineScope] to use for coroutines.
     */
    @JvmOverloads
    open fun applyDefaults(controlPoints: BeatmapControlPoints, difficulty: BeatmapDifficulty, mode: GameMode, scope: CoroutineScope? = null) {
        kiai = controlPoints.effect.controlPointAt(startTime + CONTROL_POINT_LENIENCY).isKiai

        timePreempt = BeatmapDifficulty.difficultyRange(difficulty.ar.toDouble(), PREEMPT_MAX, PREEMPT_MID, PREEMPT_MIN)

        // Preempt time can go below 450ms. Normally, this is achieved via the DT mod which uniformly speeds up all animations game wide regardless of AR.
        // This uniform speedup is hard to match 1:1, however we can at least make AR>10 (via mods) feel good by extending the upper linear function above.
        // Note that this doesn't exactly match the AR>10 visuals as they're classically known, but it feels good.
        // This adjustment is necessary for AR>10, otherwise timePreempt can become smaller leading to hit circles not fully fading in.
        timeFadeIn = 400 * min(1.0, timePreempt / PREEMPT_MIN)

        stackOffsetMultiplier = when (mode) {
            GameMode.Droid -> 4f
            GameMode.Standard -> -6.4f
        }

        difficultyScale = when (mode) {
            GameMode.Droid -> {
                val droidScale = CircleSizeCalculator.droidCSToDroidDifficultyScale(difficulty.difficultyCS)
                val radius = CircleSizeCalculator.droidScaleToStandardRadius(droidScale)
                val standardCS = CircleSizeCalculator.standardRadiusToStandardCS(radius, true)

                CircleSizeCalculator.standardCSToStandardScale(standardCS, true)
            }

            GameMode.Standard -> CircleSizeCalculator.standardCSToStandardScale(difficulty.gameplayCS, true)
        }

        gameplayScale = when (mode) {
            GameMode.Droid -> CircleSizeCalculator.droidCSToDroidGameplayScale(difficulty.gameplayCS)
            GameMode.Standard -> difficultyScale
        }
    }

    /**
     * Applies samples to this [HitObject].
     *
     * @param controlPoints The control points.
     * @param scope The [CoroutineScope] to use for coroutines.
     */
    open fun applySamples(controlPoints: BeatmapControlPoints, scope: CoroutineScope?) {
        val sampleControlPoint = controlPoints.sample.controlPointAt(endTime + CONTROL_POINT_LENIENCY)

        samples = samples.map {
            scope?.ensureActive()

            sampleControlPoint.applyTo(it)
        }.toMutableList()
    }

    /**
     * Given the previous [HitObject] in the beatmap, update relevant combo information.
     */
    fun updateComboInformation(lastObj: HitObject?) {
        comboIndex = lastObj?.comboIndex ?: 0
        comboIndexWithOffsets = lastObj?.comboIndexWithOffsets ?: 0
        indexInCurrentCombo = if (lastObj != null) lastObj.indexInCurrentCombo + 1 else 0

        if (isNewCombo || lastObj == null || lastObj is Spinner) {
            indexInCurrentCombo = 0
            ++comboIndex

            if (this !is Spinner) {
                // Spinners do not affect combo color offsets.
                comboIndexWithOffsets += comboOffset + 1
            }

            if (lastObj != null) {
                lastObj.isLastInCombo = true
            }
        }
    }

    /**
     * Converts a position in osu!pixels to real screen coordinates.
     *
     * @param position The position in osu!pixels.
     * @return The position in real screen coordinates.
     */
    protected fun convertPositionToRealCoordinates(position: Vector2) =
        position *
        // Scale the position to the actual play field size on screen
        Vector2(Constants.MAP_ACTUAL_WIDTH / Constants.MAP_WIDTH.toFloat(), Constants.MAP_ACTUAL_HEIGHT / Constants.MAP_HEIGHT.toFloat()) +
        // Center the position on the screen
        Vector2(Config.getRES_WIDTH() - Constants.MAP_ACTUAL_WIDTH, Config.getRES_HEIGHT() - Constants.MAP_ACTUAL_HEIGHT) / 2f

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
        BankHitSampleInfo(sampleName, SampleBank.Normal)

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
