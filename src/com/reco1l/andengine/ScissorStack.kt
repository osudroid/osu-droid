package com.reco1l.andengine

import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import androidx.core.util.Pools.*
import com.reco1l.framework.math.*
import java.util.Stack
import kotlin.math.*

object ScissorStack : Stack<Vec4>() {

    private val vec4Pool = SimplePool<Vec4>(32)


    fun pushScissor(x: Float, y: Float, width: Float, height: Float) {

        val intersectedX: Float
        val intersectedY: Float
        val intersectedWidth: Float
        val intersectedHeight: Float

        if (empty()) {
            intersectedX = x
            intersectedY = y
            intersectedWidth = width
            intersectedHeight = height
        } else {
            val current = peek()

            val minX = max(current.x, x)
            val minY = max(current.y, y)
            val maxX = min(current.x + current.z, x + width)
            val maxY = min(current.y + current.w, y + height)

            intersectedX = minX
            intersectedY = minY
            // Disjoint rectangles intersect in nothing; GL rejects a negative size.
            intersectedWidth = max(0f, maxX - minX)
            intersectedHeight = max(0f, maxY - minY)
        }

        val vec4 = vec4Pool.acquire()
            ?.takeUnless { vec -> vec.x != intersectedX || vec.y != intersectedY || vec.z != intersectedWidth || vec.w != intersectedHeight }
            ?: Vec4(intersectedX, intersectedY, intersectedWidth, intersectedHeight)

        applyClip(intersectedX.toInt(), intersectedY.toInt(), intersectedWidth.toInt(), intersectedHeight.toInt())
        super.push(vec4)
    }

    override fun pop(): Vec4? {

        if (empty()) {
            return null
        }

        val vec4 = super.pop()
        if (vec4 != null) {
            vec4Pool.release(vec4)
        }

        if (!empty()) {
            val current = peek()

            applyClip(current.x.toInt(), current.y.toInt(), current.z.toInt(), current.w.toInt())
        } else {
            QuadBatcher.clearClip()
        }

        return vec4
    }


    /** Sets the GL scissor for direct draws and reports the same rectangle to the quad batch. */
    private fun applyClip(x: Int, y: Int, width: Int, height: Int) {
        GlesBackend.state.scissor(x, y, width, height)
        QuadBatcher.setClip(x, y, width, height)
    }


    private fun readResolve(): Any = ScissorStack

}
