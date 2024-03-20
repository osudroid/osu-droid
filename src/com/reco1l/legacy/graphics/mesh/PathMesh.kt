package com.reco1l.legacy.graphics.mesh

import android.opengl.GLES20
import com.reco1l.legacy.graphics.AlphaOverrideShaderProgram
import org.andengine.engine.camera.Camera
import org.andengine.entity.primitive.DrawMode
import org.andengine.entity.primitive.Mesh
import org.andengine.entity.primitive.vbo.IMeshVertexBufferObject
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_COLOR
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_POSITION
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.VertexBufferObject
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributesBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

    fun setBaseAlpha(value: Float)
    {
        (mMeshVertexBufferObject as PathMeshVBO).baseAlpha = value
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


class PathMeshVBO(private val flat: Boolean) :

    VertexBufferObject(
        VertexBufferObjectManager.GLOBAL, 0, DrawType.STATIC, true,

        if (flat)
            Mesh.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
        else
            PathMesh.MESH_3D_ATTRIBUTES
    ),
    IMeshVertexBufferObject
{


    var buffer = FloatArray(if (flat) 3 else 4)
        set(value)
        {
            field = value
            setDirtyOnHardware()
        }

    var baseAlpha = 1f
        set(value)
        {
            field = value.coerceIn(0f, 1f)
        }


    private var alpha = 0f

    private var floatBuffer = mByteBuffer.asFloatBuffer()



    override fun getBufferData() = buffer

    override fun getHeapMemoryByteSize() = byteCapacity

    override fun getNativeHeapMemoryByteSize() = byteCapacity


    override fun bind(gl: GLState, shader: ShaderProgram)
    {
        super.bind(gl, AlphaOverrideShaderProgram)

        AlphaOverrideShaderProgram.setAlphaUniform(baseAlpha * alpha)
    }

    override fun unbind(gl: GLState, shader: ShaderProgram) = super.unbind(gl, AlphaOverrideShaderProgram)


    override fun onUpdateVertices(mesh: Mesh) = setDirtyOnHardware()

    override fun onUpdateColor(mesh: Mesh)
    {
        // We update the alpha when the data is bound to the hardware buffer so we ensure it is done
        // after the data is updated.
        alpha = mesh.alpha.coerceIn(0f, 1f)
    }

    override fun onBufferData()
    {
        val buffer = buffer
        mCapacity = buffer.size

        if (floatBuffer.capacity() < mCapacity)
        {
            mByteBuffer = ByteBuffer.allocateDirect((mCapacity + (if (flat) 3 else 4) * 6) * Float.SIZE_BYTES)
            mByteBuffer.order(ByteOrder.nativeOrder())

            floatBuffer = mByteBuffer.asFloatBuffer()
        }

        floatBuffer.position(0)
        floatBuffer.put(buffer)

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mByteBuffer.capacity(), mByteBuffer, mUsage)
    }
}
