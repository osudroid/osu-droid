package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES32
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.TextureData
import org.andengine.graphics.TextureFormat

/**
 * 8x8 magenta/black checkerboard (RGBA8, nearest, repeat), bound instead of textures whose source
 * failed so a missing texture is visible rather than black or garbage. Created on first use in a
 * context and recreated after a context loss. GL thread only.
 */
object GlesPlaceholderTexture : GlesRestorable {

    const val SIZE = 8

    private var textureId = 0
    private var registered = false

    /** The GL texture id; creates the texture in the current context if needed. */
    val id: Int
        get() {
            if (textureId == 0) create()
            return textureId
        }

    override val restorePhase: RestorePhase get() = RestorePhase.Textures

    /** The checkerboard pixels: one-pixel cells, magenta at (0, 0). */
    fun createData(): TextureData {
        val pixels = ByteBuffer.allocateDirect(SIZE * SIZE * 4).order(ByteOrder.nativeOrder())
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val v: Byte = if ((x + y) % 2 == 0) 0xFF.toByte() else 0
                pixels.put(v).put(0).put(v).put(0xFF.toByte())
            }
        }
        pixels.flip()
        return TextureData(SIZE, SIZE, TextureFormat.RGBA8, pixels)
    }

    private fun create() {
        val gl = GlesBackend.gl
        val ids = IntArray(1)
        gl.glGenTextures(1, ids, 0)
        textureId = ids[0]

        val target = GLES20.GL_TEXTURE_2D
        GlesBackend.state.bindTexture(target, textureId)
        gl.glTexStorage2D(target, 1, GlesResources.internalFormat(TextureFormat.RGBA8), SIZE, SIZE)
        gl.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        gl.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        gl.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        gl.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GlesBackend.state.bindPixelUnpackBuffer(0)
        gl.glTexSubImage2D(target, 0, 0, 0, SIZE, SIZE, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, createData().pixels)
        GlesResources.label(GLES32.GL_TEXTURE, textureId, "placeholder")

        if (!registered) {
            registered = true
            GlesBackend.registry.register(this)
        }
    }

    override fun onContextLost() {
        textureId = 0
    }

    override fun restore(backend: GlesBackend) {
        // A texture restored earlier in the same phase may have created it already.
        if (textureId == 0) create()
    }

    override fun toString() = "GlesPlaceholderTexture"
}
