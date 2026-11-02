package com.reco1l.andengine.buffered

import org.andengine.graphics.gles.GlesBackend
import android.opengl.GLES32
import org.andengine.opengl.util.GLState

abstract class TextureCoordinatesBuffer(
    vertexCount: Int,
    vertexSize: Int,
    bufferUsage: Int
) : VertexBuffer(0, vertexCount, vertexSize, bufferUsage) {

    override fun beginDraw(gl: GLState) {
        bindAndUpload()
        // Attribute 3 = texture coordinates (ATTRIBUTE_TEXTURECOORDINATES_LOCATION = 3 in ShaderProgramConstants)
        GlesBackend.state.vertexAttribPointer(3, vertexSize, GLES32.GL_FLOAT, false, 0, 0)
        GlesBackend.state.enableVertexAttribArray(3)
    }

    override fun declarePointers(gl: GLState, entity: UIBufferedComponent<*>) {
        // Handled in beginDraw
    }

    override fun draw(gl: GLState, entity: UIBufferedComponent<*>) = Unit

}