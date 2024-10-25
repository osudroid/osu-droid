package com.rian.osu.difficulty

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.beatmap.hitobject.sliderobject.SliderRepeat
import com.rian.osu.math.Precision.almostEquals
import com.rian.osu.math.Vector2
import com.rian.osu.mods.ModHidden
import kotlin.math.*

/**
 * Represents a [HitObject] with additional information for difficulty calculation.
 */
abstract class DifficultyHitObject(
    /**
     * The [HitObject] that this [DifficultyHitObject] wraps.
     */
    @JvmField
    val obj: HitObject,

    /**
     * The [HitObject] that occurs before [obj].
     */
    private val lastObj: HitObject?,

    /**
     * The [HitObject] that occurs before [lastObj].
     */
    private val lastLastObj: HitObject?,

    /**
     * The clock rate being calculated.
     */
    clockRate: Double,

    /**
     * Other hit objects in the beatmap, including this hit object.
     */
    protected val difficultyHitObjects: Array<out DifficultyHitObject>,

    /**
     * The index of this hit object in the list of all hit objects.
     *
     * This is one less than the actual index of the hit object in the beatmap.
     */
    @JvmField
    val index: Int,

    /**
     * The great window of the hit object.
     */
    greatWindow: Double
) {
    /**
     * The normalized distance from the "lazy" end position of the previous hit object to the start position of this hit object.
     *
     * The "lazy" end position is the position at which the cursor ends up if the previous hit object is followed with as minimal movement as possible (i.e. on the edge of slider follow circles).
     */
    @JvmField
    var lazyJumpDistance = 0.0

    /**
     * The normalized shortest distance to consider for a jump between the previous hit object and this hit object.
     *
     * This is bounded from above by [lazyJumpDistance], and is smaller than the former if a more natural path is able to be taken through the previous hit object.
     *
     * Suppose a linear slider - circle pattern. Following the slider lazily (see: [lazyJumpDistance]) will result in underestimating the true end position of the slider as being closer towards the start position.
     * As a result, [lazyJumpDistance] overestimates the jump distance because the player is able to take a more natural path by following through the slider to its end,
     * such that the jump is felt as only starting from the slider's true end position.
     *
     * Now consider a slider - circle pattern where the circle is stacked along the path inside the slider.
     * In this case, the lazy end position correctly estimates the true end position of the slider and provides the more natural movement path.
     */
    @JvmField
    var minimumJumpDistance = 0.0

    /**
     * The time taken to travel through [minimumJumpDistance], with a minimum value of 25ms.
     */
    var minimumJumpTime = MIN_DELTA_TIME.toDouble()

    /**
     * The normalized distance between the start and end position of this hit object.
     */
    @JvmField
    var travelDistance = 0.0

    /**
     * The time taken to travel through [travelDistance], with a minimum value of 25ms for sliders.
     */
    @JvmField
    var travelTime = MIN_DELTA_TIME.toDouble()

    /**
     * Angle the player has to take to hit this hit object.
     *
     * Calculated as the angle between the circles (current-2, current-1, current).
     */
    @JvmField
    var angle: Double? = null

    /**
     * The amount of milliseconds elapsed between this hit object and the last hit object.
     */
    @JvmField
    val deltaTime = if (lastObj != null) (obj.startTime - lastObj.startTime) / clockRate else 0.0

    /**
     * The amount of milliseconds elapsed since the start time of the previous hit object, with a minimum of 25ms.
     */
    // Capped to 25ms to prevent difficulty calculation breaking from simultaneous objects.
    @JvmField
    val strainTime = if (lastObj != null) max(deltaTime, MIN_DELTA_TIME.toDouble()) else 0.0

    /**
     * Adjusted start time of the hit object, taking speed multiplier into account.
     */
    @JvmField
    val startTime = obj.startTime / clockRate

    /**
     * Adjusted end time of the hit object, taking speed multiplier into account.
     */
    @JvmField
    val endTime = obj.endTime / clockRate

    /**
     * The full great window of the hit object.
     */
    @JvmField
    val fullGreatWindow = greatWindow * 2

    protected abstract val mode: GameMode
    protected abstract val scalingFactor: Float

    protected open val maximumSliderRadius = NORMALIZED_RADIUS * 2.4f
    private val assumedSliderRadius = NORMALIZED_RADIUS * 1.8f

    /**
     * Computes the properties of this [DifficultyHitObject].
     *
     * @param clockRate The clock rate to compute,
     */
    open fun computeProperties(clockRate: Double, objects: List<HitObject>) = setDistances(clockRate)

    /**
     * Gets the [DifficultyHitObject] at a specific index with respect to the current
     * [DifficultyHitObject]'s index.
     *
     * Will return `null` if the index is out of range.
     *
     * @param backwardsIndex The index to move backwards for.
     * @return The [DifficultyHitObject] at the index with respect to the current
     * [DifficultyHitObject]'s index, or `null` if the index is out of range.
     */
    open fun previous(backwardsIndex: Int) = if (index - (backwardsIndex + 1) >= 0) difficultyHitObjects[index - (backwardsIndex + 1)] else null

    /**
     * Gets the [DifficultyHitObject] at a specific index with respect to the current
     * [DifficultyHitObject]'s index.
     *
     * Will return `null` if the index is out of range.
     *
     * @param forwardsIndex The index to move forwards for.
     * @return The [DifficultyHitObject] at the index with respect to the current
     * [DifficultyHitObject]'s index, or `null` if the index is out of range.
     */
    open fun next(forwardsIndex: Int) = if (index + forwardsIndex + 1 < difficultyHitObjects.size) difficultyHitObjects[index + forwardsIndex + 1] else null

    /**
     * Calculates the opacity of the hit object at a given time.
     *
     * @param time The time to calculate the hit object's opacity at.
     * @param isHidden Whether Hidden mod is used.
     * @return The opacity of the hit object at the given time.
     */
    fun opacityAt(time: Double, isHidden: Boolean): Double {
        if (time > obj.startTime) {
            // Consider a hit object as being invisible when its start time is passed.
            // In reality the hit object will be visible beyond its start time up until its hittable window has passed,
            // but this is an approximation and such a case is unlikely to be hit where this function is used.
            return 0.0
        }

        val fadeInStartTime = obj.startTime - obj.timePreempt
        val fadeInDuration = obj.timeFadeIn
        val nonHiddenOpacity = ((time - fadeInStartTime) / fadeInDuration).coerceIn(0.0, 1.0)

        if (isHidden) {
            val fadeOutStartTime = fadeInStartTime + fadeInDuration
            val fadeOutDuration = obj.timePreempt * ModHidden.FADE_OUT_DURATION_MULTIPLIER

            return min(nonHiddenOpacity, 1 - ((time - fadeOutStartTime) / fadeOutDuration).coerceIn(0.0, 1.0))
        }

        return nonHiddenOpacity
    }

    /**
     * How possible is it to doubletap this object together with the next one and get perfect
     * judgement in range from 0 to 1.
     *
     * A value closer to 1 indicates a higher possibility.
     */
    val doubletapness: Double
        get() {
            val next = next(0) ?: return 1.0

            val currentDeltaTime = max(1.0, deltaTime)
            val nextDeltaTime = max(1.0, next.deltaTime)
            val deltaDifference = abs(nextDeltaTime - currentDeltaTime)

            val speedRatio = currentDeltaTime / max(currentDeltaTime, deltaDifference)
            val windowRatio = min(1.0, currentDeltaTime / fullGreatWindow).pow(2)

            return 1 - speedRatio.pow(1 - windowRatio)
        }

    private fun setDistances(clockRate: Double) {
        if (obj is Slider) {
            computeSliderCursorPosition(obj)
            travelDistance = obj.lazyTravelDistance.toDouble()

            // Bonus for repeat sliders until a better per nested object strain system can be achieved.
            travelDistance *= when (mode) {
                GameMode.Droid -> (1 + obj.repeatCount / 4.0).pow(1 / 4.0)
                GameMode.Standard -> (1 + obj.repeatCount / 2.5).pow(1 / 2.5)
            }

            travelTime = max(obj.lazyTravelTime / clockRate, MIN_DELTA_TIME.toDouble())
        }

        // We don't need to calculate either angle or distance when one of the last->curr objects
        // is a spinner or there is no object before the current object.
        if (lastObj == null || obj is Spinner || lastObj is Spinner) {
            return
        }

        val lastCursorPosition = getEndCursorPosition(lastObj)

        lazyJumpDistance = (obj.difficultyStackedPosition * scalingFactor - lastCursorPosition * scalingFactor).length.toDouble()
        minimumJumpTime = strainTime
        minimumJumpDistance = lazyJumpDistance

        if (lastObj is Slider) {
            val lastTravelTime = max(lastObj.lazyTravelTime / clockRate, MIN_DELTA_TIME.toDouble())

            minimumJumpTime = max(strainTime - lastTravelTime, MIN_DELTA_TIME.toDouble())

            // There are two types of slider-to-object patterns to consider in order to better approximate the real movement a player will take to jump between the hit objects.
            //
            // 1. The anti-flow pattern, where players cut the slider short in order to move to the next hit object.
            //
            //      <======o==>  ← slider
            //             |     ← most natural jump path
            //             o     ← a follow-up hit circle
            //
            // In this case the most natural jump path is approximated by LazyJumpDistance.
            //
            // 2. The flow pattern, where players follow through the slider to its visual extent into the next hit object.
            //
            //      <======o==>---o
            //                  ↑
            //        most natural jump path
            //
            // In this case the most natural jump path is better approximated by a new distance called "tailJumpDistance" - the distance between the slider's tail and the next hit object.
            //
            // Thus, the player is assumed to jump the minimum of these two distances in all cases.
            val tailJumpDistance = (lastObj.tail.difficultyStackedPosition - obj.difficultyStackedPosition).length * scalingFactor

            minimumJumpDistance = max(
                0.0,
                min(
                    lazyJumpDistance - (maximumSliderRadius - assumedSliderRadius),
                    (tailJumpDistance - maximumSliderRadius).toDouble()
                )
            )
        }

        if (lastLastObj != null && lastLastObj !is Spinner) {
            val lastLastCursorPosition = getEndCursorPosition(lastLastObj)
            val v1 = lastLastCursorPosition - lastObj.difficultyStackedPosition
            val v2 = obj.difficultyStackedPosition - lastCursorPosition

            val dot = v1.dot(v2)
            val det = v1.x * v2.y - v1.y * v2.x

            angle = abs(atan2(det.toDouble(), dot.toDouble()))
        }
    }

    private fun computeSliderCursorPosition(slider: Slider) {
        if (slider.lazyEndPosition != null) {
            return
        }

        if (mode == GameMode.Droid) {
            // Temporary lazy end position until a real result can be derived.
            slider.lazyEndPosition = slider.difficultyStackedPosition

            // Stop here if the slider has very short duration, allowing the player to essentially
            // complete the slider without movement, making travel distance and time irrelevant.
            if (almostEquals(slider.startTime, slider.endTime)) {
                return
            }
        }

        slider.lazyTravelTime = slider.nestedHitObjects[slider.nestedHitObjects.size - 1].startTime - slider.startTime

        var endTimeMin = slider.lazyTravelTime / slider.spanDuration
        if (endTimeMin % 2 >= 1) {
            endTimeMin = 1 - endTimeMin % 1
        } else {
            endTimeMin %= 1.0
        }

        // Temporary lazy end position until a real result can be derived.
        slider.lazyEndPosition = slider.difficultyStackedPosition + slider.path.positionAt(endTimeMin)

        var currentCursorPosition = slider.difficultyStackedPosition
        val scalingFactor = NORMALIZED_RADIUS / slider.difficultyRadius

        for (i in 1 until slider.nestedHitObjects.size) {
            val currentMovementObject = slider.nestedHitObjects[i]
            var currentMovement = currentMovementObject.difficultyStackedPosition - currentCursorPosition
            var currentMovementLength = scalingFactor * currentMovement.length

            // The amount of movement required so that the cursor position needs to be updated.
            var requiredMovement = assumedSliderRadius.toDouble()

            if (i == slider.nestedHitObjects.size - 1) {
                // The end of a slider has special aim rules due to the relaxed time constraint on position.
                // There is both a lazy end position and the actual end slider position. We assume the player takes the simpler movement.
                // For sliders that are circular, the lazy end position may actually be farther away than the sliders' true end.
                // This code is designed to prevent buffing situations where lazy end is actually a less efficient movement.
                val lazyMovement = slider.lazyEndPosition!! - currentCursorPosition
                if (lazyMovement.length < currentMovement.length) {
                    currentMovement = lazyMovement
                }

                currentMovementLength = scalingFactor * currentMovement.length
            } else if (currentMovementObject is SliderRepeat) {
                // For a slider repeat, assume a tighter movement threshold to better assess repeat sliders.
                requiredMovement = NORMALIZED_RADIUS.toDouble()
            }

            if (currentMovementLength > requiredMovement) {
                // This finds the positional delta from the required radius and the current position,
                // and updates the currentCursorPosition accordingly, as well as rewarding distance.

                // The extra brackets at the end here is necessary as the arithmetic operation of the
                // latter must be done first, otherwise the precision loss order will not match the
                // real algorithm.
                currentCursorPosition = currentCursorPosition + currentMovement * ((currentMovementLength - requiredMovement) / currentMovementLength)

                currentMovementLength *= (currentMovementLength - requiredMovement) / currentMovementLength

                slider.lazyTravelDistance += currentMovementLength.toFloat()
            }

            if (i == slider.nestedHitObjects.size - 1) {
                slider.lazyEndPosition = currentCursorPosition
            }
        }
    }

    private fun getEndCursorPosition(obj: HitObject): Vector2 {
        var pos = obj.difficultyStackedPosition

        if (obj is Slider) {
            computeSliderCursorPosition(obj)
            pos = obj.lazyEndPosition ?: pos
        }

        return pos
    }

    companion object {
        /**
         * A distance by which all distances should be scaled in order to assume a uniform circle size.
         */
        @JvmStatic
        protected val NORMALIZED_RADIUS = 50f

        /**
         * The minimum delta time between hit objects.
         */
        @JvmStatic
        val MIN_DELTA_TIME = 25
    }
}
