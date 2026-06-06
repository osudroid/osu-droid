package ru.nsu.ccfit.zuev.osu.game.cursor.trail

import android.opengl.GLES20
import org.andengine.engine.camera.Camera
import org.andengine.entity.Entity
import org.andengine.opengl.shader.PositionColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.util.GLState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.hypot

/**
 * A ribbon-style cursor trail that renders a rainbow-colored triangle-strip path.
 *
 * Each trail control-point is added when the cursor moves ≥ 5 units, then fades
 * out over ~0.5 s. The ribbon is drawn with additive blending and per-vertex
 * rainbow colors using [PositionColorShaderProgram].
 */
class FancyCursorTrail : Entity() {

    /** Half-width of the ribbon at its newest (widest) point, in screen pixels. */
    var trailWidth: Float = 8f

    /** Current cursor position; update this every frame before calling [update]. */
    var cursorX: Float = 0f
    var cursorY: Float = 0f

    // Trail state

    private class TrailPiece(
        val x: Float,
        val y: Float,
        val startWidth: Float
    ) {
        var alpha: Float = 1f
        var r: Float = 1f
        var g: Float = 1f
        var b: Float = 1f
        var width: Float = startWidth
    }

    private val pieces = ArrayList<TrailPiece>(256)
    private var lastX = Float.NaN
    private var lastY = Float.NaN
    private var totalTime = 0f

    // Client-side vertex buffer (interleaved: x, y, r, g, b, a)

    private var stagingBuffer: FloatBuffer? = null

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

    override fun draw(pGLState: GLState, pCamera: Camera) {
        val n = pieces.size
        if (n < 2) return

        // Total vertices in the triangle-strip: 2 per segment between control-points
        val vertCount = (n - 1) * 2
        val floatsPerVert = 6          // x, y, r, g, b, a
        val totalFloats = vertCount * floatsPerVert

        // Grow the staging buffer when needed
        var buf = stagingBuffer
        if (buf == null || buf.capacity() < totalFloats) {
            val bb = ByteBuffer.allocateDirect((totalFloats + 12) * 4)
                .order(ByteOrder.nativeOrder())
            buf = bb.asFloatBuffer()
            stagingBuffer = buf
        }

        // Fill vertex data
        buf.position(0)
        for (idx in 1 until n) {
            val prev = pieces[idx - 1]
            val curr = pieces[idx]

            // Perpendicular to the segment direction
            val diffX = curr.x - prev.x
            val diffY = curr.y - prev.y
            var perpX = diffY
            var perpY = -diffX
            val len = hypot(perpX, perpY)
            if (len > 0f) { perpX /= len; perpY /= len }

            // Vertex A – on the 'left' of prev
            buf.put(prev.x - perpX * prev.width)
            buf.put(prev.y - perpY * prev.width)
            buf.put(prev.r); buf.put(prev.g); buf.put(prev.b); buf.put(prev.alpha)

            // Vertex B – on the 'right' of curr
            buf.put(curr.x + perpX * curr.width)
            buf.put(curr.y + perpY * curr.width)
            buf.put(curr.r); buf.put(curr.g); buf.put(curr.b); buf.put(curr.alpha)
        }

        // Bind shader (compiles lazily on first call)
        val shader = PositionColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        // Disable the texture-coord attrib array (PositionColor shader doesn't use it)
        GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)

        // Upload the MVP matrix so the ribbon maps to screen coordinates
        GLES20.glUniformMatrix4fv(
            shader.uniformMVPMatrixLocation, 1, false,
            pGLState.modelViewProjectionGLMatrix, 0
        )

        // Additive blending gives a nice neon glow effect
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)

        // Ensure no VBO is bound so GL uses our client-side buffer
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        val stride = floatsPerVert * 4   // 24 bytes between consecutive vertices

        // Position attribute – 2 floats starting at float-offset 0
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION)
        buf.position(0)
        GLES20.glVertexAttribPointer(
            ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION,
            2, GLES20.GL_FLOAT, false, stride, buf
        )

        // Color attribute – 4 floats starting at float-offset 2 (byte-offset 8)
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        buf.position(2)
        GLES20.glVertexAttribPointer(
            ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION,
            4, GLES20.GL_FLOAT, false, stride, buf
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertCount)

        // Restore state expected by other renderers

        // Restore normal alpha blending
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // Re-enable the texture-coord attrib array (PositionColor shader disables it)
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)
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

