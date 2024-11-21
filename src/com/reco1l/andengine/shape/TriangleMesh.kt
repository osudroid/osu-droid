package com.reco1l.andengine.shape

import com.edlplan.andengine.TriangleRenderer
import com.edlplan.framework.utils.*
import com.reco1l.andengine.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*

class TriangleMesh : ExtendedEntity(vertexBuffer = null) {


    /**
     * The vertices of the mesh.
     */
    val vertices = FloatArraySlice()


    init {
        isCullingEnabled = false
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


    override fun onInitDraw(pGL: GL10) {

        super.onInitDraw(pGL)

        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
    }


    override fun onUpdateVertexBuffer() {

    }


    override fun drawVertices(pGL: GL10, pCamera: Camera) {

        if (vertices.length == 0) {
            return
        }

        TriangleRenderer.get().renderTriangles(vertices, pGL)
    }

}