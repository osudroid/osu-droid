package com.reco1l.andengine.shape

import com.edlplan.andengine.TriangleRenderer
import com.edlplan.framework.utils.*
import com.reco1l.andengine.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*

// TODO: This class should be replaced with a more efficient BufferedEntity implementation.
class TriangleMesh : ExtendedEntity() {


    /**
     * The vertices of the mesh.
     */
    val vertices = FloatArraySlice()


    init {
        vertices.ary = FloatArray(0)
    }


    /**
     * Allows to set the content size of the mesh explicitly.
     */
    fun setContentSize(width: Float, height: Float) {
        contentWidth = width
        contentHeight = height
        onContentSizeMeasured()
    }


    override fun beginDraw(pGL: GL10) {

        super.beginDraw(pGL)

        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)

        if (vertices.length != 0) {
            TriangleRenderer.get().renderTriangles(vertices, gl)
        }
    }

}