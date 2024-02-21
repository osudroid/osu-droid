package com.reco1l.legacy.graphics

import android.opengl.GLES20
import org.andengine.entity.primitive.Mesh
import org.andengine.entity.primitive.vbo.IMeshVertexBufferObject
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.VertexBufferObject
import ru.nsu.ccfit.zuev.osu.GlobalManager
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

    var alpha = 0f
        set(value)
        {
            if (field != value)
            {
                field = value
                setDirtyOnHardware()
            }
        }


    private var floatBuffer = mByteBuffer.asFloatBuffer()



    override fun getBufferData() = buffer

    override fun getHeapMemoryByteSize() = byteCapacity

    override fun getNativeHeapMemoryByteSize() = byteCapacity



    override fun onUpdateVertices(mesh: Mesh) = setDirtyOnHardware()

    override fun onUpdateColor(mesh: Mesh)
    {
        // We update the alpha when the data is bound to the hardware buffer so we ensure it is done
        // after the data is updated.
        alpha = mesh.alpha
    }


    private fun repackColor(rawColor: Float, alpha: Int): Float
    {
        val colorBits = rawColor.toRawBits()

        // Source color with alpha applied.
        val packedBits = (alpha shl 24)
            .or(colorBits shr 16 and 0xFF shl 16)
            .or(colorBits shr 8 and 0xFF shl 8)
            .or(colorBits shr 0 and 0xFF shl 0)

        return Float.fromBits(packedBits and -0x1)
    }


    private fun onBufferColor(buffer: FloatArray)
    {
        // This will only update the alpha since is expected to the color be specified at every
        // vertex due to the attributes.
        val alpha = (255f * alpha).toInt()

        var firstColor = Float.NaN
        var firstColorComputed = 0f

        var secondColor = Float.NaN
        var secondColorComputed = 0f

        // Bits operations punish performance specially fromBits() and toRawBits() functions.
        // Here we try to do these operations once, and then reuse the results using the original
        // values as reference.

        for (i in 0 until buffer.size / if (flat) 3 else 4)
        {
            val index = if (flat)
                i * 3 + 2 // 2D vertices
            else
                i * 4 + 3 // 3D vertices

            val packedColor = buffer[index]

            if (packedColor != firstColor && packedColor != secondColor)
            {
                if (firstColor.isNaN())
                {
                    firstColor = packedColor
                    firstColorComputed = repackColor(packedColor, alpha)
                }
                else if (secondColor.isNaN())
                {
                    secondColor = packedColor
                    secondColorComputed = repackColor(packedColor, alpha)
                }
                // Should not be reached since this is intended to be used with two colors only.
                else continue
            }

            buffer[index] = when (packedColor)
            {
                firstColor -> firstColorComputed
                secondColor -> secondColorComputed
                else -> continue
            }
        }
    }


    override fun onBufferData()
    {
        val buffer = buffer
        mCapacity = buffer.size

        onBufferColor(buffer)

        if (floatBuffer.capacity() < mCapacity)
        {
            mByteBuffer = ByteBuffer.allocateDirect((mCapacity + (if (flat) 3 else 4) * 6) * Float.SIZE_BYTES)
            mByteBuffer.order(ByteOrder.nativeOrder())

            floatBuffer = mByteBuffer.asFloatBuffer()
        }

        floatBuffer.position(0)
        floatBuffer.put(buffer)
        floatBuffer.limit(mCapacity)

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mByteBuffer.capacity(), mByteBuffer, mUsage)
    }
}