package com.reco1l.andengine.sprite

import com.reco1l.framework.Interpolation
import com.reco1l.framework.math.Vec2
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.opengles.GL10
import org.anddev.andengine.opengl.texture.region.TextureRegion

/**
 * A [UISprite] that supports gradual fade to a certain direction.
 */
class UIFadeSprite @JvmOverloads constructor(textureRegion: TextureRegion? = null) : UISprite(textureRegion) {
    /**
     * Direction the fade travels. The start alpha is at the tail of the vector, the end alpha is at the head.
     */
    var fadeDirection = leftToRight
        set(value) {
            if (field != value) {
                field = value
                rebuildColorBuffer()
            }
        }

    /**
     * Alpha at the beginning of the fade (tail of the direction vector).
     */
    var fadeAlphaStart = 1f
        set(value) {
            if (field != value) {
                field = value
                rebuildColorBuffer()
            }
        }

    /**
     * Alpha at the end of the fade (head of the direction vector).
     */
    var fadeAlphaEnd = 0f
        set(value) {
            if (field != value) {
                field = value
                rebuildColorBuffer()
            }
        }

    /**
     * RGB color multiplied into the texture.
     */
    var fadeColor = Triple(1f, 1f, 1f)
        set(value) {
            if (field != value) {
                field = value
                rebuildColorBuffer()
            }
        }

    private val colorBuffer = ByteBuffer
        .allocateDirect(4 * 4 * Float.SIZE_BYTES)
        .apply { order(ByteOrder.nativeOrder()) }
        .asFloatBuffer()

    init {
        rebuildColorBuffer()
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)

        gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer)
    }

    override fun onDrawBuffer(gl: GL10) {
        super.onDrawBuffer(gl)

        gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
        gl.glColor4f(1f, 1f, 1f, 1f)
    }

    private fun rebuildColorBuffer() {
        val dir = fadeDirection
        val (r, g, b) = fadeColor

        // Project each vertex onto the direction vector
        val dots = VERTEX_UV.map { (u, v) -> u * dir.x + v * dir.y }
        val minDot = dots.min()
        val maxDot = dots.max()

        dots.forEachIndexed { i, dot ->
            val alpha =
                if (minDot == maxDot) fadeAlphaStart
                else Interpolation.floatAt(dot, fadeAlphaStart, fadeAlphaEnd, minDot, maxDot)

            val base = i * 4

            colorBuffer.put(base, r)
            colorBuffer.put(base + 1, g)
            colorBuffer.put(base + 2, b)
            colorBuffer.put(base + 3, alpha)
        }

        colorBuffer.position(0)
    }

    companion object {
        // Normalized (u, v) positions matching RectangleVertexBuffer order:
        // 0: top-left, 1: bottom-left, 2: top-right, 3: bottom-right
        private val VERTEX_UV = arrayOf(
            0f to 0f,
            0f to 1f,
            1f to 0f,
            1f to 1f,
        )

        val leftToRight = Vec2(1f, 0f)
        val rightToLeft = Vec2(-1f, 0f)
        val topToBottom = Vec2(0f, 1f)
        val bottomToTop = Vec2(0f, -1f)
        val topLeftToBottomRight = Vec2(1f, 1f)
    }
}