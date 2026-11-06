package com.reco1l.andengine.shape

import org.andengine.graphics.BlendMode
import org.andengine.graphics.ClearBuffers
import org.andengine.graphics.DepthState
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import com.edlplan.framework.utils.*
import com.reco1l.andengine.buffered.UIBufferedComponent
import com.reco1l.andengine.component.*
import org.andengine.engine.camera.*
import org.andengine.graphics.DrawContext

// TODO: This class should be replaced with a more efficient BufferedEntity implementation.
class UITriangleMesh : UIComponent() {


    /**
     * The vertices of the mesh.
     */
    val vertices = FloatArraySlice()


    /**
     * How the mesh uses the depth buffer.
     */
    var depthState = DepthState.Disabled

    /**
     * The buffers cleared before the mesh draws.
     */
    var clearBuffers = ClearBuffers.None

    /**
     * The blend mode of the mesh; null draws with the one of the parent mesh or buffered component.
     */
    var blendMode: BlendMode? = UIBufferedComponent.DEFAULT_BLEND

    init {
        vertices.ary = FloatArray(0)
    }


    /**
     * Allows to set the content size of the mesh explicitly.
     */
    fun setContentSize(width: Float, height: Float) {
        contentWidth = width
        contentHeight = height
    }


    private val resolvedBlendMode: BlendMode
        get() = blendMode ?: when (val parent = parent) {
            is UIBufferedComponent<*> -> parent.blendMode
            is UITriangleMesh -> parent.blendMode
            else -> null
        } ?: UIBufferedComponent.DEFAULT_BLEND

    override fun beginDraw(drawContext: DrawContext) {
        super.beginDraw(drawContext)
        drawContext.clear(clearBuffers)

        // The depth state itself comes from the pipeline of the draw.
        if (depthState.test == null) {
            drawContext.disableDepthTest()
        }
    }

    override fun doDraw(drawContext: DrawContext, pCamera: Camera) {
        super.doDraw(drawContext, pCamera)

        if (vertices.length > 0) {
            MeshRenderer.draw(
                PrimitiveType.Triangles,
                vertices.ary, vertices.offset, vertices.length / 2,
                drawContext.modelViewGLMatrix, drawContext.projectionGLMatrix,
                QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
                resolvedBlendMode, depthState.test, depthState.write
            )
        }

        // The mesh renderer restores the state it found; disable the depth test after drawing so
        // subsequent entities (sprites, circles, etc.) are not accidentally tested against values we wrote.
        if (depthState.test != null) {
            drawContext.disableDepthTest()
        }
    }

}
