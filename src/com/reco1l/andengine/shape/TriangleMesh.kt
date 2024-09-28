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


    /**
     * Whether to clear the depth buffer before drawing.
     */
    var clearDepth = false

    /**
     * Whether to enable depth testing.
     */
    var depthTest = false


    init {
        isCullingEnabled = false
        vertices.ary = FloatArray(0)
    }


    override fun onInitDraw(pGL: GL10) {

        super.onInitDraw(pGL)

        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)

        if (clearDepth) {
            pGL.glClear(GL10.GL_DEPTH_BUFFER_BIT)
        }
    }


    override fun onUpdateVertexBuffer() {

    }


    override fun drawVertices(pGL: GL10, pCamera: Camera) {

        if (vertices.length == 0) {
            return
        }

        val wasDepthTest = GLHelper.isEnableDepthTest()
        GLHelper.setDepthTest(pGL, depthTest)
        TriangleRenderer.get().renderTriangles(vertices, pGL)
        GLHelper.setDepthTest(pGL, wasDepthTest)
    }

}