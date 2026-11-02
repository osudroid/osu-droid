package org.andengine.graphics

/** An offscreen surface to draw into; its content is in [color]. Content is lost with the context. */
interface RenderTarget : GpuResource {
    val width: Int
    val height: Int
    val color: Texture
}
