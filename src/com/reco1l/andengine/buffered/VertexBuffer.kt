package com.reco1l.andengine.buffered

import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

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


