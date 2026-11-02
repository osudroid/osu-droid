package com.osudroid.game.cursor.trail

import org.andengine.graphics.BlendMode
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.MeshRenderer
import com.reco1l.andengine.component.UIComponent
import org.andengine.engine.camera.Camera
import org.andengine.graphics.DrawContext
import kotlin.math.cos
import kotlin.math.hypot

/**
 * A ribbon-style cursor trail that renders a rainbow-colored triangle-strip path.
 *
 * Each trail control-point is added when the cursor moves ≥ 5 units, then fades
 * out over ~0.5 s. The ribbon is drawn with additive blending and per-vertex
 * rainbow colors through the engine [MeshRenderer].
 */
class FancyCursorTrail : UIComponent() {

    /** Half-width of the ribbon at its newest (widest) point, in screen pixels. */
    var trailWidth = 8f

    /** Current cursor position; update this every frame before calling [update]. */
    var cursorX = 0f
    var cursorY = 0f

    // Trail state

    private class TrailPiece(
        val x: Float,
        val y: Float,
        val startWidth: Float
    ) {
        var alpha = 1f
        var r = 1f
        var g = 1f
        var b = 1f
        var width = startWidth
    }

    private val pieces = ArrayList<TrailPiece>(256)
    private var lastX = Float.NaN
    private var lastY = Float.NaN
    private var totalTime = 0f

    // Reusable vertex staging (interleaved: x, y, r, g, b, a), grown only when needed
    private var vertexData = FloatArray(0)

    // Reusable float array to avoid per-call allocation in rainbowColor()
    private val tempRgb = FloatArray(3)

    init {
        // GameScene drives updates manually; skip the engine's automatic pass.
        isIgnoreUpdate = true
    }


    /**
     * Advance the trail simulation by [dt] seconds and collect a new control-point
     * if the cursor has moved far enough.  Call this every game-loop frame.
     */
    fun update(dt: Float) {
        totalTime += dt

        // Age existing pieces (reverse iteration so removal is safe)
        var i = pieces.size - 1
        while (i >= 0) {
            val p = pieces[i]
            val hue = p.alpha * 10f + totalTime * 25f
            rainbowColor(hue, tempRgb)
            p.r = tempRgb[0]
            p.g = tempRgb[1]
            p.b = tempRgb[2]

            p.alpha -= dt * FADE_OUT_SPEED
            if (p.alpha <= 0f) {
                pieces.removeAt(i)
            } else {
                p.width = p.startWidth * p.alpha
            }
            i--
        }

        // Emit a new control-point when the cursor travels ≥ MIN_DIST
        if (lastX.isNaN()) {
            lastX = cursorX
            lastY = cursorY
        }
        val dx = cursorX - lastX
        val dy = cursorY - lastY
        if (hypot(dx, dy) >= MIN_DIST) {
            lastX = cursorX
            lastY = cursorY
            pieces.add(TrailPiece(cursorX, cursorY, trailWidth))
        }
    }

    /** Clear all trail state (e.g. when gameplay ends / cursor is hidden). */
    fun resetTrail() {
        pieces.clear()
        lastX = Float.NaN
        lastY = Float.NaN
        totalTime = 0f
    }

    // Entity rendering

    override fun doDraw(pGLState: DrawContext, pCamera: Camera) {
        super.doDraw(pGLState, pCamera)

        val n = pieces.size
        if (n < 2) return

        // Total vertices in the triangle-strip: 2 per segment between control-points
        val vertCount = (n - 1) * 2
        val floatsPerVert = 6          // x, y, r, g, b, a
        val totalFloats = vertCount * floatsPerVert

        var data = vertexData
        if (data.size < totalFloats) {
            data = FloatArray(totalFloats + 12 * floatsPerVert)
            vertexData = data
        }

        // Fill vertex data
        var o = 0
        for (idx in 1 until n) {
            val prev = pieces[idx - 1]
            val curr = pieces[idx]

            // Perpendicular to the segment direction
            val diffX = curr.x - prev.x
            val diffY = curr.y - prev.y
            var perpX = diffY
            var perpY = -diffX
            val len = hypot(perpX, perpY)
            if (len > 0f) {
                perpX /= len
                perpY /= len
            }

            // Vertex A - on the 'left' of prev
            data[o++] = prev.x - perpX * prev.width
            data[o++] = prev.y - perpY * prev.width
            data[o++] = prev.r
            data[o++] = prev.g
            data[o++] = prev.b
            data[o++] = prev.alpha

            // Vertex B - on the 'right' of curr
            data[o++] = curr.x + perpX * curr.width
            data[o++] = curr.y + perpY * curr.width
            data[o++] = curr.r
            data[o++] = curr.g
            data[o++] = curr.b
            data[o++] = curr.alpha
        }

        // Additive blending gives a nice neon glow effect
        MeshRenderer.drawColored(
            PrimitiveType.TriangleStrip,
            data, 0, vertCount,
            pGLState.modelViewGLMatrix, pGLState.projectionGLMatrix,
            BlendMode.AdditiveAllChannels, depthTest = null, depthWrite = false
        )
    }

    // Helpers

    /**
     * Cosine-based rainbow RGB. Result channels are in [0, 1].
     */
    private fun rainbowColor(t: Float, out: FloatArray) {
        val td = t.toDouble()
        val r = ((cos(td)           + 1.0) / 2.0).coerceIn(0.0, 1.0).toFloat()
        val g = ((cos(td - 2.09440) + 1.0) / 2.0).coerceIn(0.0, 1.0).toFloat()  // 2π/3
        val b = ((cos(td - 4.18879) + 1.0) / 2.0).coerceIn(0.0, 1.0).toFloat()  // 4π/3

        // Mix with white at saturation 0.5 and boost brightness
        val sat = 0.5f
        val bri = 1.5f
        out[0] = (r * sat * bri + (1f - sat)).coerceIn(0f, 1f)
        out[1] = (g * sat * bri + (1f - sat)).coerceIn(0f, 1f)
        out[2] = (b * sat * bri + (1f - sat)).coerceIn(0f, 1f)
    }

    companion object {
        private const val FADE_OUT_SPEED = 2f
        private const val MIN_DIST = 5f
    }
}

