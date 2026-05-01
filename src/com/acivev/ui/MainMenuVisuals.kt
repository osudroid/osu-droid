package com.acivev.ui

import org.andengine.entity.modifier.MoveXModifier
import org.andengine.entity.primitive.Rectangle
import org.andengine.entity.scene.Scene
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.andengine.util.modifier.ease.EaseBounceOut
import org.andengine.util.modifier.ease.EaseExponentialOut
import ru.nsu.ccfit.zuev.osu.Config
import kotlin.math.max
import kotlin.math.min

/**
 * All MainScene visual effects in one place:
 *  - 4 spectrum rings (audio-reactive, continuously spinning)
 *  - Smooth gradient beat-flash on left / right edges
 *
 * Usage:
 *   1. Call [load] once after the Scene is created.
 *   2. Call [updateFrame] every frame (flash decay + ring spin).
 *   3. Call [updateAudio] every frame while music is PLAYING (FFT width/alpha).
 *   4. Call [clearBars] when music is not playing.
 *   5. Call [onBeat] on each detected beat.
 *   6. Call [onMenuShow] / [onMenuHide] when the side menu opens/closes.
 *   7. Call [onExit] when the exit animation begins.
 */
class MainMenuVisuals {

    private companion object {
        const val GRAD_STEPS            = 16
        const val EDGE_WIDTH            = 0.22f

        const val EXTRA_RING_COUNT      = 3
        val       RING_BASE_OFFSET      = floatArrayOf(0f, 90f, 180f, 270f)

        /** Inner gap so bars never render under the logo. */
        const val INNER_RADIUS          = 130f

        const val RING_SPIN_SPEED       = 12f   // °/sec idle
        const val RING_SPIN_SPEED_KIAI  = 40f   // °/sec during kiai
        const val BEAT_SPEED_BOOST      = 160f  // °/sec boost on beat
        const val MAX_SPIN_SPEED        = 110f  // hard upper cap
        const val BOOST_DECAY           = 4f    // exponential decay rate
    }

    private val flashLeft  = arrayOfNulls<Rectangle>(GRAD_STEPS)
    private val flashRight = arrayOfNulls<Rectangle>(GRAD_STEPS)
    private var flashAlphaLeft  = 0f
    private var flashAlphaRight = 0f
    private var flashLeftNext   = true

    private val spectrum   = arrayOfNulls<Rectangle>(120)
    private val extraRings = Array(EXTRA_RING_COUNT) { arrayOfNulls<Rectangle>(120) }

    private val ringRotation = FloatArray(4)
    private var ringSpeedBoost = 0f

    /** Create and attach all rectangles. Call once right after the scene is ready. */
    fun load(scene: Scene, vbo: VertexBufferObjectManager) {
        val cX   = Config.getRES_WIDTH().toFloat()  / 2f
        val cY   = Config.getRES_HEIGHT().toFloat() / 2f
        val pX   = cX + INNER_RADIUS
        val barW = 260f - INNER_RADIUS

        // Ring 0 — main spectrum
        for (i in 0 until 120) {
            spectrum[i] = Rectangle(pX, cY, barW, 10f, vbo).also {
                it.setRotationCenter(-INNER_RADIUS, 5f)
                it.setScaleCenter(-INNER_RADIUS, 5f)
                it.setRotation(-220f + i * 3f)
                it.setAlpha(0f)
                scene.attachChild(it)
            }
        }

        // Rings 1-3 — rotated copies (90°, 180°, 270°)
        for (k in 0 until EXTRA_RING_COUNT) {
            val baseOff = RING_BASE_OFFSET[k + 1]
            for (i in 0 until 120) {
                extraRings[k][i] = Rectangle(pX, cY, barW, 10f, vbo).also {
                    it.setRotationCenter(-INNER_RADIUS, 5f)
                    it.setScaleCenter(-INNER_RADIUS, 5f)
                    it.setRotation(-220f + i * 3f + baseOff)
                    it.setAlpha(0f)
                    scene.attachChild(it)
                }
            }
        }

        // Gradient flash — 16 thin slices per side
        val edgeW  = Config.getRES_WIDTH().toFloat() * EDGE_WIDTH
        val sliceW = edgeW / GRAD_STEPS
        val scrW   = Config.getRES_WIDTH().toFloat()
        val scrH   = Config.getRES_HEIGHT().toFloat()
        for (i in 0 until GRAD_STEPS) {
            flashLeft[i] = Rectangle(i * sliceW, 0f, sliceW, scrH, vbo).also {
                it.setColor(1f, 1f, 1f); it.setAlpha(0f); scene.attachChild(it)
            }
            flashRight[i] = Rectangle(scrW - (i + 1) * sliceW, 0f, sliceW, scrH, vbo).also {
                it.setColor(1f, 1f, 1f); it.setAlpha(0f); scene.attachChild(it)
            }
        }
    }

    /**
     * Per-frame update: flash decay, ring spin, bar rotation.
     * Call every frame regardless of music state.
     */
    fun updateFrame(secondsElapsed: Float, isContinuousKiai: Boolean) {
        // Flash decay + quadratic falloff per slice
        flashAlphaLeft  = max(0f, flashAlphaLeft  - secondsElapsed * 3.5f)
        flashAlphaRight = max(0f, flashAlphaRight - secondsElapsed * 3.5f)
        for (i in 0 until GRAD_STEPS) {
            val t       = i.toFloat() / (GRAD_STEPS - 1)
            val falloff = (1f - t) * (1f - t)
            flashLeft[i]!!.setAlpha(flashAlphaLeft  * falloff)
            flashRight[i]!!.setAlpha(flashAlphaRight * falloff)
        }

        // Ring spin with beat-boost decay
        ringSpeedBoost *= max(0f, 1f - BOOST_DECAY * secondsElapsed)
        val base = if (isContinuousKiai) RING_SPIN_SPEED_KIAI else RING_SPIN_SPEED
        val step = (base + ringSpeedBoost) * secondsElapsed
        ringRotation[0] += step;  ringRotation[1] -= step
        ringRotation[2] += step;  ringRotation[3] -= step

        // Apply rotation to all bars
        for (i in 0 until 120) {
            spectrum[i]!!.setRotation(-220f + i * 3f + ringRotation[0])
        }
        for (k in 0 until EXTRA_RING_COUNT) {
            val baseOff = RING_BASE_OFFSET[k + 1]
            for (i in 0 until 120) {
                extraRings[k][i]!!.setRotation(-220f + i * 3f + baseOff + ringRotation[k + 1])
            }
        }
    }

    /**
     * Audio-reactive update: bar width and alpha driven by FFT data.
     * Call only when music is PLAYING.
     */
    fun updateAudio(peakLevel: FloatArray, peakAlpha: FloatArray, isContinuousKiai: Boolean) {
        val kiaiBoost = if (isContinuousKiai) 1.8f else 1.0f
        val baseW     = 250f - INNER_RADIUS
        for (i in 0 until 120) {
            spectrum[i]!!.setWidth(baseW + peakLevel[i])
            spectrum[i]!!.setAlpha(min(peakAlpha[i] * kiaiBoost, 1f))
        }
        for (k in 0 until EXTRA_RING_COUNT) {
            for (i in 0 until 120) {
                extraRings[k][i]!!.setWidth(baseW + peakLevel[i])
                extraRings[k][i]!!.setAlpha(min(peakAlpha[i] * kiaiBoost, 1f))
            }
        }
    }

    /** Hide all spectrum bars. Call when music is stopped/paused. */
    fun clearBars() {
        for (bar in spectrum)            { bar!!.setWidth(0f); bar.setAlpha(0f) }
        for (ring in extraRings) for (bar in ring) { bar!!.setWidth(0f); bar.setAlpha(0f) }
    }

    /** Fire beat flash and rotation boost on a detected beat. */
    fun onBeat(isContinuousKiai: Boolean) {
        val peak = if (isContinuousKiai) 0.32f else 0.18f
        if (flashLeftNext) flashAlphaLeft = peak else flashAlphaRight = peak
        flashLeftNext = !flashLeftNext

        val base = if (isContinuousKiai) RING_SPIN_SPEED_KIAI else RING_SPIN_SPEED
        ringSpeedBoost = min(ringSpeedBoost + BEAT_SPEED_BOOST, MAX_SPIN_SPEED - base)
    }

    /** Force everything invisible when the exit animation starts. */
    fun onExit() {
        clearBars()
        for (i in 0 until GRAD_STEPS) { flashLeft[i]!!.setAlpha(0f); flashRight[i]!!.setAlpha(0f) }
        flashAlphaLeft = 0f; flashAlphaRight = 0f
    }

    /** Slide spectrum rings left when the side menu opens. */
    fun onMenuShow() {
        val from = Config.getRES_WIDTH().toFloat() / 2f + INNER_RADIUS
        val to   = Config.getRES_WIDTH().toFloat() / 3f + INNER_RADIUS
        for (bar in spectrum) bar!!.registerEntityModifier(MoveXModifier(0.3f, from, to, EaseExponentialOut.getInstance()))
        for (ring in extraRings) for (bar in ring) bar!!.registerEntityModifier(MoveXModifier(0.3f, from, to, EaseExponentialOut.getInstance()))
    }

    /** Return spectrum rings to centre when the side menu closes. */
    fun onMenuHide() {
        val from = Config.getRES_WIDTH().toFloat() / 3f + INNER_RADIUS
        val to   = Config.getRES_WIDTH().toFloat() / 2f + INNER_RADIUS
        for (bar in spectrum) bar!!.registerEntityModifier(MoveXModifier(1f, from, to, EaseBounceOut.getInstance()))
        for (ring in extraRings) for (bar in ring) bar!!.registerEntityModifier(MoveXModifier(1f, from, to, EaseBounceOut.getInstance()))
    }
}

