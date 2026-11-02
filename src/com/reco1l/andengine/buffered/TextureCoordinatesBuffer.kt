package com.reco1l.andengine.buffered

import org.andengine.opengl.util.GLState

/**
 * Texture coordinates of another buffer's vertices, one pair per vertex in the same order.
 */
abstract class TextureCoordinatesBuffer(
    vertexCount: Int,
    vertexSize: Int
) : VertexBuffer(0, vertexCount, vertexSize) {

    // Texture coordinates of another buffer's vertices: nothing to draw on their own.
    override fun drawMesh(gl: GLState, entity: UIBufferedComponent<*>) = Unit

}
