package com.reco1l.andengine.buffered

import org.andengine.graphics.DrawContext

/**
 * Texture coordinates of another buffer's vertices, one pair per vertex in the same order.
 */
abstract class TextureCoordinatesBuffer(
    vertexCount: Int,
    vertexSize: Int
) : VertexBuffer(0, vertexCount, vertexSize) {

    // Nothing to draw on their own.
    override fun drawMesh(gl: DrawContext, entity: UIBufferedComponent<*>) = Unit

}
