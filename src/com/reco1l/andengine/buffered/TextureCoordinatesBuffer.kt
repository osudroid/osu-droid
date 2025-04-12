package com.reco1l.andengine.buffered

import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*

abstract class TextureCoordinatesBuffer(
    vertexCount: Int,
    vertexSize: Int,
    bufferUsage: Int
) : VertexBuffer(0, vertexCount, vertexSize, bufferUsage) {

    override fun beginDraw(gl: GL10) {
        GLHelper.enableTextures(gl)
        GLHelper.enableTexCoordArray(gl)
    }

    override fun declarePointers(gl: GL10, entity: BufferedEntity<*>) {
        if (GLHelper.EXTENSIONS_VERTEXBUFFEROBJECTS) {
            selectOnHardware(gl as GL11)
            GLHelper.texCoordZeroPointer(gl)
        } else {
            GLHelper.texCoordPointer(gl, mFloatBuffer)
        }
    }

    override fun draw(gl: GL10, entity: BufferedEntity<*>) = Unit

}