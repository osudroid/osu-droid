package com.reco1l.andengine.buffered

import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesPipeline
import org.andengine.graphics.debug.RenderStats
import android.opengl.GLES32
import com.osudroid.math.toRadians
import org.andengine.opengl.util.GLState
import kotlin.math.*

abstract class VertexBuffer(
    val drawTopology: Int,
    val vertexCount: Int,
    val vertexSize: Int,
    bufferUsage: Int
) : Buffer(vertexCount * vertexSize, bufferUsage) {


    // Write position of the sequential [putVertex], in floats.
    private var cursor = 0


    /**
     * The GL draw mode the vertices are drawn with for [entity]; [drawTopology] unless a buffer
     * depends on the entity's state.
     */
    open fun topology(entity: UIBufferedComponent<*>): Int = drawTopology


    //region Draw pipeline

    override fun beginDraw(gl: GLState) {
        bindAndUpload()
        // Attribute 0 = position (matches standard position attribute location in shaders)
        GlesBackend.state.vertexAttribPointer(0, vertexSize, GLES32.GL_FLOAT, false, 0, 0)
        GlesBackend.state.enableVertexAttribArray(0)
    }

    override fun declarePointers(gl: GLState, entity: UIBufferedComponent<*>) {
        // Pointers are already declared in beginDraw for GLES2
    }

    override fun draw(gl: GLState, entity: UIBufferedComponent<*>) {
        QuadBatcher.flush()
        GLES32.glDrawArrays(topology(entity), 0, vertexCount)
        RenderStats.draw(vertexCount)
    }

    override fun drawMesh(gl: GLState, entity: UIBufferedComponent<*>) {
        check(vertexSize == VERTEX_2D) { "The mesh renderer draws 2D vertices, got $vertexSize components" }
        val primitive = GlesPipeline.primitiveTypeOf(topology(entity)) ?: return
        entity.drawPositions(gl, primitive, data, 0, vertexCount)
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
        cursor = value
    }

    fun putVertex(x: Float, y: Float) {
        data[cursor++] = x
        data[cursor++] = y
    }

    fun putVertex(index: Int, x: Float, y: Float) {
        data[index * vertexSize] = x
        data[index * vertexSize + 1] = y
    }

    //endregion


    companion object {
        const val VERTEX_2D = 2
        const val VERTEX_3D = 3
    }

}


