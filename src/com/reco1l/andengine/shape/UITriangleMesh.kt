package com.reco1l.andengine.shape

import android.opengl.GLES32
import com.edlplan.andengine.TriangleRenderer
import com.edlplan.framework.utils.*
import com.reco1l.andengine.buffered.UIBufferedComponent
import com.reco1l.andengine.component.*
import org.andengine.engine.camera.*
import org.andengine.entity.shape.IShape
import org.andengine.opengl.shader.PositionColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.util.*

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


    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)

        // Clearing
        var clearMask = 0

        if (clearInfo.depthBuffer) clearMask = clearMask or GLES32.GL_DEPTH_BUFFER_BIT
        if (clearInfo.colorBuffer) clearMask = clearMask or GLES32.GL_COLOR_BUFFER_BIT
        if (clearInfo.stencilBuffer) clearMask = clearMask or GLES32.GL_STENCIL_BUFFER_BIT

        if (clearMask != 0) {
            GLES32.glClear(clearMask)
        }

        // Depth testing
        if (depthInfo.test) {
            GLES32.glDepthFunc(depthInfo.function)
            GLES32.glDepthMask(depthInfo.mask)

            pGLState.enableDepthTest()
        } else {
            pGLState.disableDepthTest()
        }

        // Blending
        var sourceFactor = IShape.BLENDFUNCTION_SOURCE_DEFAULT
        var destinationFactor = IShape.BLENDFUNCTION_DESTINATION_DEFAULT

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

        pGLState.enableBlend()
        pGLState.blendFunction(sourceFactor, destinationFactor)
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        super.doDraw(pGLState, pCamera)

        if (vertices.length == 0) {
            // Ensure depth test is cleaned up even if there's nothing to render.
            if (depthInfo.test) {
                pGLState.disableDepthTest()
            }
            return
        }

        // Bind PositionColor shader
        val shader = PositionColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        // Upload MVP matrix
        if (shader.uniformMVPMatrixLocation >= 0) {
            GLES32.glUniformMatrix4fv(
                shader.uniformMVPMatrixLocation,
                1, false, pGLState.modelViewProjectionGLMatrix, 0
            )
        }

        // Provide constant color (disable per-vertex color array)
        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GLES32.glVertexAttrib4f(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, drawRed, drawGreen, drawBlue, drawAlpha)

        // Disable texture coordinates
        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)

        // Ensure position attribute array is enabled
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION)


        TriangleRenderer.get().renderTriangles(vertices, pGLState)

        // Restore vertex attribute array state so old AndEngine Sprite rendering is not broken.
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)

        // Disable depth test after drawing so subsequent entities (sprites, circles, etc.)
        // are not accidentally depth-tested against values we wrote.
        if (depthInfo.test) {
            pGLState.disableDepthTest()
        }
    }

}