package com.reco1l.legacy.graphics

import android.opengl.GLES20
import org.andengine.engine.camera.Camera
import org.andengine.entity.primitive.DrawMode
import org.andengine.entity.primitive.Mesh
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_COLOR
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_POSITION
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributesBuilder

class PathMesh(private val flat: Boolean) : Mesh(0f, 0f, 0, DrawMode.TRIANGLES, PathMeshVBO(flat))
{

    var clearDepth = false


    fun setVertices(vertices: FloatArray)
    {
        // Since sprites are cached and recycled, we need to copy the array to avoid rewrite of the
        // buffer with wrong vertices.
        (mMeshVertexBufferObject as PathMeshVBO).buffer = vertices

        setVertexCountToDraw(vertices.size / if (flat) 3 else 4)
    }


    override fun detachSelf(): Boolean
    {
        // Fixes an issue were the previous buffer is rendered when the new isn't ready yet. This is
        // caused due to the sprites are cached and recycled.
        setVertexCountToDraw(0)

        return super.detachSelf()
    }

    override fun preDraw(gl: GLState, pCamera: Camera)
    {
        gl.disableCulling()

        super.preDraw(gl, pCamera)
    }

    override fun draw(gl: GLState, pCamera: Camera)
    {
        if (clearDepth)
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

        val hadDepthTest = gl.enableDepthTest()

        super.draw(gl, pCamera)

        if (!hadDepthTest)
            gl.disableDepthTest()
    }

    companion object
    {

        // 3D attributes are needed because of the 3D projection and masking.

        val MESH_3D_ATTRIBUTES = VertexBufferObjectAttributesBuilder(2)
            .add(ATTRIBUTE_POSITION_LOCATION, ATTRIBUTE_POSITION, 3, GLES20.GL_FLOAT, false)
            .add(ATTRIBUTE_COLOR_LOCATION, ATTRIBUTE_COLOR, 4, GLES20.GL_UNSIGNED_BYTE, true)
            .build()!!
    }
}
