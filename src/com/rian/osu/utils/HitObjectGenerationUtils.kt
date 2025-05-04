package com.rian.osu.utils

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.beatmap.hitobject.sliderobject.SliderHitObject
import com.rian.osu.math.Precision
import com.rian.osu.math.Random
import com.rian.osu.math.Vector2
import com.rian.osu.math.Vector4
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Utilities for [HitObject] generation.
 */
object HitObjectGenerationUtils {
    /**
     * The relative distance to the edge of the playfield before [HitObject] positions should start to "turn around" and
     * curve towards the middle. The closer the [HitObject]s draw to the border, the sharper the turn.
     */
    private const val PLAYFIELD_EDGE_RATIO = 0.375f

    /**
     * The amount of previous [HitObject]s to be shifted together when a [HitObject] is being moved.
     */
    private const val PRECEDING_OBJECTS_TO_SHIFT = 10

    val playfieldSize = Vector2(512, 384)
    val playfieldCenter = playfieldSize / 2

    private val borderDistance = playfieldSize * PLAYFIELD_EDGE_RATIO

    /**
     * Rotates a [HitObject] away from the edge of the playfield while keeping a constant distance from the previous
     * [HitObject].
     *
     * @param previousObjectPosition The position of the previous [HitObject].
     * @param positionRelativeToPrevious The position of the [HitObject] to be rotated relative to the previous
     * [HitObject].
     * @param rotationRatio The extent of the rotation. 0 means the [HitObject] is never rotated, while 1 means the
     * [HitObject] will be fully rotated towards the center of the playfield when it is originally at the edge of the
     * playfield.
     * @return The new position of the [HitObject] relative to the previous [HitObject].
     */
    @JvmOverloads
    fun rotateAwayFromEdge(
        previousObjectPosition: Vector2,
        positionRelativeToPrevious: Vector2,
        rotationRatio: Float = 0.5f
    ): Vector2 {
        val relativeRotationDistance = maxOf(
            if (previousObjectPosition.x < playfieldCenter.x) {
                (borderDistance.x - previousObjectPosition.x) / borderDistance.x
            } else {
                (previousObjectPosition.x - (playfieldSize.x - borderDistance.x)) / borderDistance.x
            },
            if (previousObjectPosition.y < playfieldCenter.y) {
                (borderDistance.y - previousObjectPosition.y) / borderDistance.y
            } else {
                (previousObjectPosition.y - (playfieldSize.y - borderDistance.y)) / borderDistance.y
            },
            0f
        )

        return rotateVectorTowardsVector(
            positionRelativeToPrevious,
            playfieldCenter - previousObjectPosition,
            min(1f, relativeRotationDistance * rotationRatio)
        )
    }

    /**
     * Rotates a [Vector2] towards another [Vector2].
     *
     * @param initial The [Vector2] to be rotated.
     * @param destination The [Vector2] that [initial] should be rotated towards.
     * @param rotationRatio How much [initial] should be rotated. 0 means no rotation. 1 mean [initial] is fully
     * rotated to equal [destination].
     * @return The rotated [Vector2].
     */
    fun rotateVectorTowardsVector(initial: Vector2, destination: Vector2, rotationRatio: Float): Vector2 {
        val initialAngle = atan2(initial.y, initial.x)
        val destinationAngle = atan2(destination.y, destination.x)

        var diff = destinationAngle - initialAngle

        // Normalize angle
        while (diff < -Math.PI) {
            diff += 2 * Math.PI.toFloat()
        }

        while (diff > Math.PI) {
            diff -= 2 * Math.PI.toFloat()
        }

        val finalAngle = initialAngle + rotationRatio * diff

        return Vector2(initial.length * cos(finalAngle), initial.length * sin(finalAngle))
    }

    //region Reflection

    /**
     * Reflects the position of a [HitObject] in the playfield horizontally.
     *
     * @param hitObject The [HitObject] to reflect.
     */
    fun reflectHorizontallyAlongPlayfield(hitObject: HitObject) {
        hitObject.position = reflectVectorHorizontallyAlongPlayfield(hitObject.position)

        if (hitObject is Slider) {
            modifySlider(hitObject) { Vector2(-x, y) }
        }
    }

    /**
     * Reflects the position of a [HitObject] in the playfield vertically.
     *
     * @param hitObject The [HitObject] to reflect.
     */
    fun reflectVerticallyAlongPlayfield(hitObject: HitObject) {
        hitObject.position = reflectVectorVerticallyAlongPlayfield(hitObject.position)

        if (hitObject is Slider) {
            modifySlider(hitObject) { Vector2(x, -y) }
        }
    }

    /**
     * Flips the position of a [Slider] around its start position horizontally.
     *
     * @param slider The [Slider] to be flipped.
     */
    fun flipSliderInPlaceHorizontally(slider: Slider) {
        modifySlider(slider) { Vector2(-x, y) }
    }

    private fun modifySlider(slider: Slider, modifyControlPoint: Vector2.() -> Vector2) {
        slider.path = SliderPath(
            slider.path.pathType,
            slider.path.controlPoints.map { it.modifyControlPoint() },
            slider.path.expectedDistance
        )
    }

    private fun reflectVectorHorizontallyAlongPlayfield(vector: Vector2) = Vector2(playfieldSize.x - vector.x, vector.y)
    private fun reflectVectorVerticallyAlongPlayfield(vector: Vector2) = Vector2(vector.x, playfieldSize.y - vector.y)

    //endregion

    //region Reposition

    /**
     * Generates a list of [HitObjectPositionInfo]s containing information for how the given list of [HitObject]s are
     * positioned.
     *
     * @param hitObjects A list of [HitObject]s to process.
     * @return A list of [HitObjectPositionInfo]s describing how each [HitObject] is positioned relative to the previous
     * one.
     */
    fun generatePositionInfos(hitObjects: Iterable<HitObject>): List<HitObjectPositionInfo> {
        val positionInfos = mutableListOf<HitObjectPositionInfo>()
        var previousPosition = playfieldCenter
        var previousAngle = 0f

        for (obj in hitObjects) {
            val relativePosition = obj.position - previousPosition
            var absoluteAngle = atan2(relativePosition.y, relativePosition.x)
            val relativeAngle = absoluteAngle - previousAngle

            val positionInfo = HitObjectPositionInfo(obj)
            positionInfo.relativeAngle = relativeAngle
            positionInfo.distanceFromPrevious = relativePosition.length

            if (obj is Slider) {
                val absoluteRotation = getSliderRotation(obj)
                positionInfo.rotation = absoluteRotation - absoluteAngle
                absoluteAngle = absoluteRotation
            }

            previousPosition = obj.endPosition
            previousAngle = absoluteAngle

            positionInfos.add(positionInfo)
        }

        return positionInfos
    }

    /**
     * Repositions [HitObject]s according to the information in [positionInfos].
     *
     * @param positionInfos The position information for each [HitObject].
     * @param scope The [CoroutineScope] to use for the repositioning. Useful for cancellation.
     * @return The repositioned [HitObject]s.
     */
    @JvmOverloads
    fun repositionHitObjects(
        positionInfos: List<HitObjectPositionInfo>,
        scope: CoroutineScope? = null
    ): List<HitObject> {
        val workingObjects = positionInfos.map { WorkingObject(it) }
        var previous: WorkingObject? = null

        for (i in workingObjects.indices) {
            scope?.ensureActive()

            val current = workingObjects[i]
            val hitObject = current.hitObject

            if (hitObject is Spinner) {
                previous = current
                continue
            }

            computeModifiedPosition(current, previous, workingObjects.getOrNull(i - 2))

            // Move hit objects back into the playfield if they are outside of it.
            var shift = when (hitObject) {
                is HitCircle -> clampHitCircleToPlayfield(current)
                is Slider -> clampSliderToPlayfield(current)
                else -> Vector2(0)
            }

            if (shift.x != 0f || shift.y != 0f) {
                val toBeShifted = mutableListOf<HitObject>()

                for (j in i - 1 downTo max(i - PRECEDING_OBJECTS_TO_SHIFT, 0)) {
                    // Only shift hit circles
                    if (workingObjects[j].hitObject is HitCircle) {
                        toBeShifted.add(workingObjects[j].hitObject)
                    }
                }

                applyDecreasingShift(toBeShifted, shift)
            }

            previous = current
        }

        return workingObjects.map {
            scope?.ensureActive()
            it.hitObject
        }
    }

    /**
     * Determines whether a [HitObject] is on a beat.
     *
     * @param beatmap The [Beatmap] the [HitObject] is a part of.
     * @param hitObject The [HitObject] to check.
     * @param downbeatsOnly If `true`, whether this method only returns `true` is on a downbeat.
     * @return `true` if the [hitObject] is on a (down-)beat, `false` otherwise.
     */
    @JvmOverloads
    fun isHitObjectOnBeat(beatmap: Beatmap, hitObject: HitObject, downbeatsOnly: Boolean = false): Boolean {
        val timingPoint = beatmap.controlPoints.timing.controlPointAt(hitObject.startTime)
        val timeSinceTimingPoint = hitObject.startTime - timingPoint.time

        var beatLength = timingPoint.msPerBeat

        if (downbeatsOnly) {
            beatLength *= timingPoint.timeSignature
        }

        // Ensure within 1ms of expected location.
        return abs(timeSinceTimingPoint + 1) % beatLength < 2
    }

    /**
     * Rotates a [Vector2] by the specified angle.
     *
     * @param vec The [Vector2] to be rotated.
     * @param rotation The angle to rotate [vec] by, in radians.
     * @return The rotated [Vector2].
     */
    private fun rotateVector(vec: Vector2, rotation: Float): Vector2 {
        val angle = atan2(vec.y, vec.x) + rotation
        val length = vec.length

        return Vector2(length * cos(angle), length * sin(angle))
    }

    /**
     * Generates a random number from a Normal distribution using the Box-Muller transform.
     *
     * @param random A [Random] to get the random number from.
     * @param mean The mean of the distribution.
     * @param stdDev The standard deviation of the distribution.
     * @return The random number.
     */
    @JvmOverloads
    fun randomGaussian(random: Random, mean: Float = 0f, stdDev: Float = 1f): Float {
        // Generate 2 random numbers in the interval (0,1].
        // x1 must not be 0 since log(0) = undefined.
        val x1 = 1 - random.nextDouble()
        val x2 = 1 - random.nextDouble()

        val stdNormal = sqrt(-2 * ln(x1)) * sin(2 * Math.PI * x2)

        return mean + stdDev * stdNormal.toFloat()
    }

    /**
     * Obtains the absolute rotation of a [Slider], defined as the angle from its start position to the end of its path.
     *
     * @param slider The [Slider] to obtain the rotation from.
     * @return The angle in radians.
     */
    private fun getSliderRotation(slider: Slider): Float {
        val pathEndPosition = slider.path.positionAt(1.0)

        return atan2(pathEndPosition.y, pathEndPosition.x)
    }

    /**
     * Computes the modified position of a [HitObject] while attempting to keep it inside the playfield.
     *
     * @param current The [WorkingObject] representing the [HitObject] to have the modified position computed for.
     * @param previous The [WorkingObject] representing the [HitObject] immediately preceding [current].
     * @param beforePrevious The [WorkingObject] representing the [HitObject] immediately preceding [previous].
     */
    private fun computeModifiedPosition(
        current: WorkingObject,
        previous: WorkingObject?,
        beforePrevious: WorkingObject?
    ) {
        var previousAbsoluteAngle = 0f

        if (previous != null) {
            if (previous.hitObject is Slider) {
                previousAbsoluteAngle = getSliderRotation(previous.hitObject as Slider)
            } else {
                val earliestPosition = beforePrevious?.hitObject?.endPosition ?: playfieldCenter
                val relativePosition = previous.hitObject.position - earliestPosition
                previousAbsoluteAngle = atan2(relativePosition.y, relativePosition.x)
            }
        }

        var absoluteAngle = previousAbsoluteAngle + current.positionInfo.relativeAngle

        var positionRelativeToPrevious = Vector2(
            current.positionInfo.distanceFromPrevious * cos(absoluteAngle),
            current.positionInfo.distanceFromPrevious * sin(absoluteAngle)
        )

        val lastEndPosition = previous?.endPositionModified ?: playfieldCenter

        positionRelativeToPrevious = rotateAwayFromEdge(lastEndPosition, positionRelativeToPrevious)

        current.positionModified = lastEndPosition + positionRelativeToPrevious

        val slider = current.hitObject as? Slider ?: return
        absoluteAngle = atan2(positionRelativeToPrevious.y, positionRelativeToPrevious.x)

        val centerOfMassOriginal = calculateCenterOfMass(slider)
        val centerOfMassModified = rotateAwayFromEdge(
            current.positionModified,
            rotateVector(
                centerOfMassOriginal,
                current.positionInfo.rotation + absoluteAngle - getSliderRotation(slider)
            )
        )

        val relativeRotation =
            atan2(centerOfMassModified.y, centerOfMassModified.x) - atan2(centerOfMassOriginal.y, centerOfMassOriginal.x)

        if (!Precision.almostEquals(relativeRotation, 0f)) {
            rotateSlider(slider, relativeRotation)
        }
    }

    /**
     * Moves the modified position of a [HitCircle] so that it fits inside the playfield.
     *
     * @param workingObject The [WorkingObject] that represents the [HitCircle].
     * @return The deviation from the original modified position in order to fit within the playfield.
     */
    private fun clampHitCircleToPlayfield(workingObject: WorkingObject) = workingObject.run {
        val previousPosition = positionModified

        positionModified = clampToPlayfield(positionModified, hitObject.gameplayRadius.toFloat())

        endPositionModified = positionModified
        hitObject.position = positionModified

        positionModified - previousPosition
    }

    /**
     * Moves a [Slider] and all necessary [SliderHitObject]s into the playfield if they are not in the playfield.
     *
     * @param workingObject The [WorkingObject] that represents the [Slider].
     * @return The deviation from the original modified position in order to fit within the playfield.
     */
    private fun clampSliderToPlayfield(workingObject: WorkingObject): Vector2 {
        val slider = workingObject.hitObject as Slider
        var possibleMovementBounds = calculatePossibleMovementBounds(slider)

        // The slider rotation applied in computeModifiedPosition might make it impossible to fit the slider into the
        // playfield. For example, a long horizontal slider will be off-screen when rotated by 90 degrees.
        // In this case, limit the rotation to either 0 or 180 degrees.
        if (possibleMovementBounds.width < 0 || possibleMovementBounds.height < 0) {
            val currentRotation = getSliderRotation(slider)
            val diff1 = getAngleDifference(workingObject.rotationOriginal, currentRotation)
            val diff2 = getAngleDifference(workingObject.rotationOriginal + Math.PI.toFloat(), currentRotation)

            if (diff1 < diff2) {
                rotateSlider(slider, workingObject.rotationOriginal - currentRotation)
            } else {
                rotateSlider(slider, workingObject.rotationOriginal + Math.PI.toFloat() - currentRotation)
            }

            possibleMovementBounds = calculatePossibleMovementBounds(slider)
        }

        val previousPosition = workingObject.positionModified

        // Clamp slider position to the placement area.
        // If the slider is larger than the playfield, at least make sure that the head circle is inside the playfield.
        val newX =
            if (possibleMovementBounds.width < 0) possibleMovementBounds.left.coerceIn(0f, playfieldSize.x)
            else previousPosition.x.coerceIn(possibleMovementBounds.left, possibleMovementBounds.right)

        val newY =
            if (possibleMovementBounds.height < 0) possibleMovementBounds.top.coerceIn(0f, playfieldSize.y)
            else previousPosition.y.coerceIn(possibleMovementBounds.top, possibleMovementBounds.bottom)

        workingObject.positionModified = Vector2(newX, newY)
        slider.position = workingObject.positionModified

        workingObject.endPositionModified = slider.endPosition

        return workingObject.positionModified - previousPosition
    }

    /**
     * Clamps a [Vector2] into the playfield, keeping a specified distance from the edge of the playfield.
     *
     * @param vec The [Vector2] to clamp.
     * @param padding The minimum distance allowed from the edge of the playfield.
     * @return The clamped [Vector2].
     */
    private fun clampToPlayfield(vec: Vector2, padding: Float) = Vector2(
        vec.x.coerceIn(padding, playfieldSize.x - padding),
        vec.y.coerceIn(padding, playfieldSize.y - padding)
    )

    /**
     * Decreasingly shifts a list of [HitObject]s by a specified amount.
     *
     * The first item in the list is shifted by the largest amount, while the last item is shifted by the smallest
     * amount.
     *
     * @param hitObjects The list of [HitObject]s to be shifted.
     * @param shift The amount to shift the [HitObject]s by.
     */
    private fun applyDecreasingShift(hitObjects: List<HitObject>, shift: Vector2) {
        for (i in hitObjects.indices) {
            val hitObject = hitObjects[i]

            // The first object is shifted by a vector slightly smaller than shift.
            // The last object is shifted by a vector slightly larger than zero.
            val position = hitObject.position + shift * (hitObjects.size - i) / (hitObjects.size + 1f)

            hitObject.position = clampToPlayfield(position, hitObject.gameplayRadius.toFloat())
        }
    }

    /**
     * Calculates a [Vector4] which contains all possible movements of a [Slider] (in relative X/Y coordinates) such
     * that the entire [Slider] is inside the playfield.
     *
     * If the [Slider] is larger than the playfield, the returned [Vector4] may have a Z/W component that is smaller
     * than its X/Y component.
     *
     * @param slider The [Slider] whose movement bound is to be calculated.
     * @return A [Vector4] which contains all possible movements of a [Slider] (in relative X/Y coordinates) such
     * that the entire [Slider] is inside the playfield.
     */
    fun calculatePossibleMovementBounds(slider: Slider): Vector4 {
        val pathPositions = slider.path.getPathToProgress(0.0, 1.0)

        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY

        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        // Compute the bounding box of the slider.
        for (position in pathPositions) {
            minX = min(minX, position.x)
            maxX = max(maxX, position.x)

            minY = min(minY, position.y)
            maxY = max(maxY, position.y)
        }

        // Take the radius into account.
        val radius = slider.gameplayRadius.toFloat()

        minX -= radius
        minY -= radius

        maxX += radius
        maxY += radius

        // Given the bounding box of the slider (via min/max X/Y), the amount that the slider can move to the left is
        // minX (with the sign flipped, since positive X is to the right), and the amount that it can move to the right
        // is WIDTH - maxX. The same calculation applies for the Y axis.
        val left = -minX
        val right = playfieldSize.x - maxX
        val top = -minY
        val bottom = playfieldSize.y - maxY

        return Vector4(left, top, right, bottom)
    }

    /**
     * Rotates a [Slider] around its start position by the specified angle.
     *
     * @param slider The [Slider] to rotate.
     * @param rotation The angle to rotate [slider] by, in radians.
     */
    fun rotateSlider(slider: Slider, rotation: Float) {
        modifySlider(slider) { rotateVector(this, rotation) }
    }

    /**
     * Estimates the center of mass of a [Slider] relative to its start position.
     *
     * @param slider The [Slider] whose center mass is to be estimated.
     * @return The estimated center of mass of [slider].
     */
    private fun calculateCenterOfMass(slider: Slider): Vector2 {
        val sampleStep = 50

        // Only sample the start and end positions if the slider is too short.
        if (slider.distance <= sampleStep) {
            return slider.path.positionAt(1.0) / 2
        }

        var count = 0
        var sum = Vector2(0)
        var i = 0

        while (i < slider.distance) {
            sum.plusAssign(slider.path.positionAt(i / slider.distance))
            ++count
            i += sampleStep
        }

        return sum / count
    }

    /**
     * Calculates the absolute difference between two angles in radians.
     *
     * @param angle1 The first angle.
     * @param angle2 The second angle.
     * @return THe absolute difference within interval `[0, Math.PI]`.
     */
    private fun getAngleDifference(angle1: Float, angle2: Float): Float {
        val diff = abs(angle1 - angle2) % (2 * Math.PI.toFloat())

        return min(diff, 2 * Math.PI.toFloat() - diff)
    }

    /**
     * Contains information about a [HitObject]'s position.
     */
    data class HitObjectPositionInfo(
        /**
         * The [HitObject] associated with this [HitObjectPositionInfo].
         */
        @JvmField
        val hitObject: HitObject
    ) {
        /**
         * The jump angle from the previous [HitObject] to this one, relative to the previous [HitObject]'s jump angle.
         *
         * The [relativeAngle] of the first [HitObject] in a beatmap represents the absolute angle from the center of
         * the playfield to the [HitObject].
         *
         * If [relativeAngle] is 0, the player's cursor does not need to change its direction of movement when passing
         * from the previous [HitObject] to this one.
         */
        @JvmField
        var relativeAngle = 0f

        /**
         * The jump distance from the previous [HitObject] to this one.
         *
         * The [distanceFromPrevious] of the first [HitObject] in a beatmap is relative to the center of the playfield.
         */
        @JvmField
        var distanceFromPrevious = 0f

        /**
         * The rotation of this [HitObject] relative to its jump angle.
         *
         * For [Slider]s, this is defined as the angle from the [Slider]'s start position to the end of its path
         * relative to its jump angle. For [HitCircle]s and [Spinner]s, this property is ignored.
         */
        @JvmField
        var rotation = 0f
    }

    private data class WorkingObject(val positionInfo: HitObjectPositionInfo) {
        val rotationOriginal = if (hitObject is Slider) getSliderRotation(hitObject as Slider) else 0f
        var positionModified = hitObject.position
        var endPositionModified = hitObject.endPosition

        val hitObject
            get() = positionInfo.hitObject
    }

    //endregion
}