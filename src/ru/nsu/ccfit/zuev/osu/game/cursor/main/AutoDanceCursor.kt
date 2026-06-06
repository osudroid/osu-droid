package ru.nsu.ccfit.zuev.osu.game.cursor.main

import ru.nsu.ccfit.zuev.osu.game.GameObject
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener
import ru.nsu.ccfit.zuev.osu.game.GameplaySpinner
import kotlin.math.*

/**
 * An [AutoCursor] variant that sweeps arcing ("dance") paths between hit objects
 * instead of moving in straight lines.
 *
 * Between every two consecutive hit objects the cursor traces a half-circle arc that
 * alternates to the left and right with each new target, producing the characteristic
 * "dance" appearance.
 */
class AutoDanceCursor : AutoCursor() {

    // move state
    private var fromX = 0f
    private var fromY = 0f
    private var toX = 0f
    private var toY = 0f

    /** Accumulated elapsed time (seconds) since the current move started. */
    private var moveElapsed = 0f

    /** Total duration (seconds) allowed for the current move. */
    private var moveDuration = 0f

    /**
     * Incremented each time a new move target is issued.
     * Parity (even / odd) determines the arc direction.
     */
    private var danceFrameIndex = 0

    /** Whether we are currently interpolating a dance move. */
    private var isDancing = false

    /**
     * Captures the move parameters and resets the dance interpolation so that
     * [update] can compute arc positions each frame.
     *
     * The parent's `moveTo()` animation is intentionally bypassed here;
     * we set the cursor position manually every frame instead.
     */
    override fun moveToObject(obj: GameObject?, secPassed: Float, listener: GameObjectListener) {
        if (obj == null || currentObjectId == obj.id) {
            return
        }

        val targetX = obj.position.x
        var targetY = obj.position.y

        var deltaT = obj.hitTime - secPassed

        if (obj is GameplaySpinner) {
            targetY += 50f
        }

        currentObjectId = obj.id

        if (deltaT < 0.085f && obj !is GameplaySpinner) {
            deltaT = 0.085f
        }

        // Record from/to for arc interpolation.
        fromX = x
        fromY = y
        toX = targetX
        toY = targetY

        moveDuration = deltaT
        moveElapsed = 0f
        isDancing = true
        danceFrameIndex++

        // Stop the inherited easing modifier so it cannot interfere.
        clearEntityModifiers()

        // Immediately notify so that flashlight / other listeners know the destination.
        listener.onUpdatedAutoCursor(targetX, targetY)
    }

    /**
     * Advances the dance arc interpolation every frame and positions the cursor accordingly.
     */
    override fun update(pSecondsElapsed: Float) {
        if (isDancing && moveDuration > 0f) {
            moveElapsed += pSecondsElapsed

            val blend = (moveElapsed / moveDuration).coerceAtMost(1.0f)

            var x = lerp(fromX, toX, blend)
            var y = lerp(fromY, toY, blend)

            val dx = toX - fromX
            val dy = toY - fromY
            val dist = sqrt(dx * dx + dy * dy)
            val length = dist / 2f

            // Only apply the arc when the distance is large enough to look deliberate.
            if (length >= MIN_DANCE_DISTANCE) {
                // Map blend [0,1] to the sine/cosine phase angles.
                val phaseX = lerp(Math.PI, 0.0, blend.toDouble())        // π → 0
                val phaseY = lerp(Math.PI / 2.0, -Math.PI / 2.0, blend.toDouble()) // π/2 → -π/2

                val danceX = (sin(phaseX) * length).toFloat()
                val danceY = (cos(phaseY) * length).toFloat()

                if (danceFrameIndex % 2 == 0) {
                    x += danceX
                    y += danceY
                } else {
                    x -= danceX
                    y -= danceY
                }
            }

            setPosition(x, y)

            if (blend >= 1.0f) {
                isDancing = false
            }
        }

        // Delegate cursor sprite + trail updates and entity modifier processing.
        super.update(pSecondsElapsed)
    }

    companion object {
        private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

        private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

        /**
         * Minimum half-distance (screen pixels) between the current and target hit object
         * positions before the arc effect is applied. Below this threshold the cursor just
         * travels in a straight line (avoids tiny loops for overlapping notes).
         */
        private const val MIN_DANCE_DISTANCE = 20f
    }
}

