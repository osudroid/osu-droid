package org.andengine.graphics

import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import kotlin.math.max
import kotlin.math.min

/**
 * Nested clip rectangles in surface pixels (origin bottom-left, as `glScissor` takes them). Each
 * [push] intersects with the rectangle below it; the top rectangle is set as the GL scissor for
 * direct draws and as the clip of the [QuadBatcher]. The first push enables the scissor test and the
 * last [pop] restores the state found then. GL thread only.
 */
object ClipStack {

    // x, y, width, height per level.
    private var rects = FloatArray(4 * 8)
    private var size = 0
    private var scissorWasEnabled = false

    /** Whether no clip is active. */
    @JvmStatic
    val isEmpty: Boolean get() = size == 0

    /** Clips the following draws to ([x], [y], [width], [height]) within the current clip. */
    @JvmStatic
    fun push(x: Float, y: Float, width: Float, height: Float) {
        var left = x
        var bottom = y
        var right = x + width
        var top = y + height

        if (size > 0) {
            val o = (size - 1) * 4
            left = max(left, rects[o])
            bottom = max(bottom, rects[o + 1])
            right = min(right, rects[o] + rects[o + 2])
            top = min(top, rects[o + 1] + rects[o + 3])
        } else {
            val state = GlesBackend.state
            scissorWasEnabled = state.isScissorEnabled
            state.setScissorEnabled(true)
        }

        if (rects.size < (size + 1) * 4) {
            rects = rects.copyOf(rects.size * 2)
        }

        val o = size * 4
        rects[o] = left
        rects[o + 1] = bottom
        // Disjoint rectangles intersect in nothing; GL rejects a negative size.
        rects[o + 2] = max(0f, right - left)
        rects[o + 3] = max(0f, top - bottom)
        size++
        applyTop()
    }

    /** Removes the last [push]; does nothing without one. */
    @JvmStatic
    fun pop() {
        if (size == 0) {
            return
        }

        size--

        if (size > 0) {
            applyTop()
        } else {
            QuadBatcher.clearClip()
            GlesBackend.state.setScissorEnabled(scissorWasEnabled)
        }
    }

    /** Removes all clips, e.g. after a frame that ended without popping them. */
    @JvmStatic
    fun clear() {
        while (size > 0) pop()
    }

    private fun applyTop() {
        val o = (size - 1) * 4
        val x = rects[o].toInt()
        val y = rects[o + 1].toInt()
        val width = rects[o + 2].toInt()
        val height = rects[o + 3].toInt()
        GlesBackend.state.scissor(x, y, width, height)
        QuadBatcher.setClip(x, y, width, height)
    }
}
