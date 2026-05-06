package com.reco1l.andengine.buffered

import android.opengl.GLES20
import com.reco1l.toolkt.*
import org.andengine.opengl.util.GLState
import kotlin.math.*

abstract class VertexBuffer(
    val drawTopology: Int,
    val vertexCount: Int,
    val vertexSize: Int,
    bufferUsage: Int
) : Buffer(vertexCount * vertexSize, bufferUsage) {


    //region Draw pipeline

    override fun beginDraw(gl: GLState) {
        bindAndUpload()
        // Attribute 0 = position (matches standard position attribute location in shaders)
        GLES20.glVertexAttribPointer(0, vertexSize, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glEnableVertexAttribArray(0)
    }

    override fun declarePointers(gl: GLState, entity: UIBufferedComponent<*>) {
        // Pointers are already declared in beginDraw for GLES2
    }

    override fun draw(gl: GLState, entity: UIBufferedComponent<*>) {
        GLES20.glDrawArrays(drawTopology, 0, vertexCount)
    }

    //endregion


    //region Elements

    fun addQuad(index: Int, fromX: Float, fromY: Float, toX: Float, toY: Float): Int {
        var i = index
        putVertex(i++, fromX, fromY)
        putVertex(i++, fromX, toY)
        putVertex(i++, toX, fromY)
        putVertex(i++, toX, toY)
        return i
    }

    fun addLine(index: Int, fromX: Float, fromY: Float, toX: Float, toY: Float): Int {
        var i = index
        putVertex(i++, fromX, fromY)
        putVertex(i++, toX, toY)
        return i
    }

    fun addTriangle(index: Int, centerX: Float, centerY: Float, width: Float, height: Float): Int {
        var i = index
        val halfWidth = width / 2f
        val halfHeight = height / 2f
        putVertex(i++, centerX, centerY - halfHeight)
        putVertex(i++, centerX - halfWidth, centerY + halfHeight)
        putVertex(i++, centerX + halfWidth, centerY + halfHeight)
        return i
    }

    fun addArc(
        index: Int,
        centerX: Float,
        centerY: Float,
        startAngle: Float,
        endAngle: Float,
        width: Float,
        height: Float,
        segments: Int
    ): Int {
        var i = index

        val startAngleRadians = (startAngle - 90f).toRadians()
        val deltaAngle = ((endAngle - 90f).toRadians() - startAngleRadians) / max(1, segments - 1)

        for (j in 0 ..< segments) {
            val angle = startAngleRadians + j * deltaAngle
            putVertex(i++, centerX + width * cos(angle), centerY + height * sin(angle))
        }

        return i
    }


    //endregion

    //region Utility functions

    fun setPosition(value: Int) {
        mFloatBuffer.position(value)
    }

    fun putVertex(x: Float, y: Float) {
        mFloatBuffer.put(x)
        mFloatBuffer.put(y)
    }

    fun putVertex(index: Int, x: Float, y: Float) {
        mFloatBuffer.put(index * vertexSize, x)
        mFloatBuffer.put(index * vertexSize + 1, y)
    }

    //endregion


    companion object {
        const val VERTEX_2D = 2
        const val VERTEX_3D = 3
    }

}


