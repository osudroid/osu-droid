package com.reco1l.andengine.shape

import org.andengine.graphics.BlendMode
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesPipeline
import android.opengl.GLES32
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
     * The depth information of the entity.
     */
    var depthInfo = DepthInfo.None

    /**
     * The clear information of the entity.
     */
    var clearInfo = ClearInfo.None

    /**
     * The blend information of the entity.
     */
    var blendInfo = BlendInfo.Mixture


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


    private var cachedSource = -1
    private var cachedDestination = -1
    private var cachedBlend = BlendMode.Alpha

    override fun beginDraw(pGLState: DrawContext) {
        super.beginDraw(pGLState)

        // Clearing
        var clearMask = 0

        if (clearInfo.depthBuffer) clearMask = clearMask or GLES32.GL_DEPTH_BUFFER_BIT
        if (clearInfo.colorBuffer) clearMask = clearMask or GLES32.GL_COLOR_BUFFER_BIT
        if (clearInfo.stencilBuffer) clearMask = clearMask or GLES32.GL_STENCIL_BUFFER_BIT

        if (clearMask != 0) {
            // Pending quads belong before the clear; and a clear ignores the depth buffer while the
            // depth mask is off, which the mesh renderer's state restore can leave behind.
            QuadBatcher.flush()
            if (clearInfo.depthBuffer) GlesBackend.state.depthMask(true)
            GlesBackend.state.clear(clearMask)
        }

        // The depth state itself comes from the pipeline of the draw.
        if (!depthInfo.test) {
            pGLState.disableDepthTest()
        }
    }

    override fun doDraw(pGLState: DrawContext, pCamera: Camera) {
        super.doDraw(pGLState, pCamera)

        if (vertices.length == 0) {
            // Ensure depth test is cleaned up even if there's nothing to render.
            if (depthInfo.test) {
                pGLState.disableDepthTest()
            }
            return
        }

        // Blending
        var sourceFactor = GLES32.GL_SRC_ALPHA
        var destinationFactor = GLES32.GL_ONE_MINUS_SRC_ALPHA

        if (blendInfo == BlendInfo.Inherit) {
            val parent = parent

            if (parent is UIBufferedComponent<*>) {
                sourceFactor = parent.blendInfo.sourceFactor
                destinationFactor = parent.blendInfo.destinationFactor
            } else if (parent is UITriangleMesh) {
                sourceFactor = parent.blendInfo.sourceFactor
                destinationFactor = parent.blendInfo.destinationFactor
            }
        } else {
            sourceFactor = blendInfo.sourceFactor
            destinationFactor = blendInfo.destinationFactor
        }

        if (sourceFactor != cachedSource || destinationFactor != cachedDestination) {
            cachedSource = sourceFactor
            cachedDestination = destinationFactor
            // Factors without an engine equivalent draw as straight alpha.
            cachedBlend = GlesPipeline.blendModeOf(sourceFactor, destinationFactor) ?: BlendMode.Alpha
        }

        val depthTest = if (depthInfo.test) GlesPipeline.compareFunctionOf(depthInfo.function) else null

        MeshRenderer.draw(
            PrimitiveType.Triangles,
            vertices.ary, vertices.offset, vertices.length / 2,
            pGLState.modelViewGLMatrix, pGLState.projectionGLMatrix,
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
            cachedBlend, depthTest, depthInfo.mask
        )

        // The mesh renderer restores the state it found; disable the depth test after drawing so
        // subsequent entities (sprites, circles, etc.) are not accidentally tested against values we wrote.
        if (depthInfo.test) {
            pGLState.disableDepthTest()
        }
    }

}
