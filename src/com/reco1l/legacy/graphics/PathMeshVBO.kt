package com.reco1l.legacy.graphics

import android.opengl.GLES20
import com.reco1l.legacy.graphics.PathMesh.Companion.MESH_3D_ATTRIBUTES
import org.andengine.entity.primitive.Mesh
import org.andengine.entity.primitive.vbo.IMeshVertexBufferObject
import org.andengine.opengl.vbo.DrawType.STATIC
import org.andengine.opengl.vbo.VertexBufferObject
import org.andengine.util.color.ColorUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PathMeshVBO :
    VertexBufferObject(null, 0, STATIC, true, MESH_3D_ATTRIBUTES),
    IMeshVertexBufferObject
{

    private var bufferData = FloatArray(0)

    private var floatBuffer = mByteBuffer.asFloatBuffer()


    override fun getHeapMemoryByteSize() = byteCapacity

    override fun getNativeHeapMemoryByteSize() = byteCapacity



    override fun getBufferData() = bufferData

    fun setBufferData(data: FloatArray)
    {
        bufferData = data
        mCapacity = bufferData.size
        setDirtyOnHardware()
    }


    override fun onUpdateVertices(mesh: Mesh) = setDirtyOnHardware()

    override fun onUpdateColor(mesh: Mesh)
    {
        // This will only update the alpha since is expected to the color be specified at every
        // vertex due to the attributes.
        for (i in 0 until bufferData.size / 4)
        {
            val index = i * 4 /*Vertex count*/ + 3 /*Color index*/
            val color = bufferData[index].toRawBits()

            bufferData[index] = ColorUtils.convertRGBAToABGRPackedFloat(
                (color shr 0 and 0xFF) / 255.0f,
                (color shr 8 and 0xFF) / 255.0f,
                (color shr 16 and 0xFF) / 255.0f,
                mesh.alpha
            )
        }

        setDirtyOnHardware()
    }

    override fun onBufferData()
    {
        if (floatBuffer.capacity() < capacity)
        {
            mByteBuffer = ByteBuffer.allocateDirect((capacity + 24) * Float.SIZE_BYTES)
            mByteBuffer.order(ByteOrder.nativeOrder())

            floatBuffer = mByteBuffer.asFloatBuffer()
        }

        floatBuffer.position(0)
        floatBuffer.put(bufferData)

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mByteBuffer.capacity(), mByteBuffer, mUsage)
    }

}