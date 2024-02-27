package com.reco1l.legacy.graphics

import android.opengl.GLES20
import org.andengine.entity.primitive.Mesh
import org.andengine.entity.primitive.vbo.IMeshVertexBufferObject
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.VertexBufferObject
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PathMeshVBO(private val flat: Boolean) :

    VertexBufferObject(
        GlobalManager.getInstance().engine.vertexBufferObjectManager, 0, DrawType.STATIC, true,

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


    private var alpha = 0f

    private var floatBuffer = mByteBuffer.asFloatBuffer()



    override fun getBufferData() = buffer

    override fun getHeapMemoryByteSize() = byteCapacity

    override fun getNativeHeapMemoryByteSize() = byteCapacity


    override fun bind(gl: GLState, shader: ShaderProgram)
    {
        super.bind(gl, AlphaOverrideShaderProgram)

        AlphaOverrideShaderProgram.setAlphaUniform(alpha)
    }

    override fun unbind(gl: GLState, shader: ShaderProgram) = super.unbind(gl, AlphaOverrideShaderProgram)


    override fun onUpdateVertices(mesh: Mesh) = setDirtyOnHardware()

    override fun onUpdateColor(mesh: Mesh)
    {
        // We update the alpha when the data is bound to the hardware buffer so we ensure it is done
        // after the data is updated.
        alpha = mesh.alpha
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