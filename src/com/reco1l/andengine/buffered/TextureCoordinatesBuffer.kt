package com.reco1l.andengine.buffered

import org.andengine.graphics.DrawContext
import org.andengine.graphics.PrimitiveType

/**
 * Texture coordinates of another buffer's vertices, one pair per vertex in the same order.
 */
abstract class TextureCoordinatesBuffer(
    vertexCount: Int,
    vertexSize: Int
) : VertexBuffer(PrimitiveType.Points, vertexCount, vertexSize) {

    // Nothing to draw on their own.
    override fun drawMesh(gl: DrawContext, entity: UIBufferedComponent<*>) = Unit

}
