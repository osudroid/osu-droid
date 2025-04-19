package com.reco1l.andengine

import android.opengl.*
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
            intersectedWidth = (maxX - minX)
            intersectedHeight = (maxY - minY)
        }

        val vec4 = vec4Pool.acquire()
            ?.takeUnless { vec -> vec.x != intersectedX || vec.y != intersectedY || vec.z != intersectedWidth || vec.w != intersectedHeight }
            ?: Vec4(intersectedX, intersectedY, intersectedWidth, intersectedHeight)

        GLES10.glScissor(
            intersectedX.toInt(),
            intersectedY.toInt(),
            intersectedWidth.toInt(),
            intersectedHeight.toInt()
        )
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

            GLES10.glScissor(
                current.x.toInt(),
                current.y.toInt(),
                current.z.toInt(),
                current.w.toInt()
            )
        }

        return vec4
    }


    private fun readResolve(): Any = ScissorStack

}
