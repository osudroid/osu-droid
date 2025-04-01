package com.reco1l.andengine.shape

import com.edlplan.andengine.TriangleRenderer
import com.edlplan.framework.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.info.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*

// TODO: This class should be replaced with a more efficient BufferedEntity implementation.
class TriangleMesh : ExtendedEntity() {


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


    init {
        vertices.ary = FloatArray(0)
    }


    /**
     * Allows to set the content size of the mesh explicitly.
     */
    fun setContentSize(width: Float, height: Float) {
        contentWidth = width
        contentHeight = height
        invalidate(InvalidationFlag.ContentSize)
    }


    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)

        // Clearing
        var clearMask = 0

        if (clearInfo.depthBuffer) clearMask = clearMask or GL10.GL_DEPTH_BUFFER_BIT
        if (clearInfo.colorBuffer) clearMask = clearMask or GL10.GL_COLOR_BUFFER_BIT
        if (clearInfo.stencilBuffer) clearMask = clearMask or GL10.GL_STENCIL_BUFFER_BIT

        if (clearMask != 0) {
            gl.glClear(clearMask)
        }

        // Depth testing
        if (depthInfo.test) {
            gl.glDepthFunc(depthInfo.function)
            gl.glDepthMask(depthInfo.mask)

            GLHelper.enableDepthTest(gl)
        } else {
            GLHelper.disableDepthTest(gl)
        }
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)

        if (vertices.length != 0) {
            TriangleRenderer.get().renderTriangles(vertices, gl)
        }
    }

}