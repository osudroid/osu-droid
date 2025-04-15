package com.reco1l.andengine.buffered

import com.reco1l.toolkt.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11
import kotlin.math.*

abstract class VertexBuffer(
    val drawTopology: Int,
    val vertexCount: Int,
    val vertexSize: Int,
    bufferUsage: Int
) : Buffer(vertexCount * vertexSize, bufferUsage) {


    //region Draw pipeline

    override fun beginDraw(gl: GL10) {
        GLHelper.enableVertexArray(gl)
    }

    override fun declarePointers(gl: GL10, entity: BufferedEntity<*>) {

        if (GLHelper.EXTENSIONS_VERTEXBUFFEROBJECTS) {
            selectOnHardware(gl as GL11)
            GLHelper.vertexZeroPointer(gl)
        } else {
            GLHelper.vertexPointer(gl, mFloatBuffer)
        }
    }

    override fun draw(gl: GL10, entity: BufferedEntity<*>) {
        gl.glDrawArrays(drawTopology, 0, vertexCount)
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

    fun addTriangle(index: Int, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Int {
        var i = index
        putVertex(i++, x1, y1)
        putVertex(i++, x2, y2)
        putVertex(i++, x3, y3)
        return i
    }

    fun addArc(
        index: Int,
        centerX: Float,
        centerY: Float,
        startAngle: Float,
        endAngle: Float,
        width: Float,
        segments: Int
    ): Int {
        var i = index

        val startAngleRadians = startAngle.toRadians()
        val deltaAngle = (endAngle.toRadians() - startAngleRadians) / segments

        for (j in 0 ..< segments) {
            val angle = startAngleRadians + j * deltaAngle
            putVertex(i++, centerX + width * cos(angle), centerY + width * sin(angle))
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


