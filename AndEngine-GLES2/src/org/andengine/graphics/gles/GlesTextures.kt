package org.andengine.graphics.gles

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteOrder
import org.andengine.opengl.texture.PixelFormat
import org.andengine.opengl.util.GLHelper

/**
 * The GL work of the legacy textures (`org.andengine.opengl.texture`): creating, binding, deleting
 * and uploading `GL_TEXTURE_2D` textures of the current context. Binds go through the state cache.
 * GL thread only.
 */
object GlesTextures {

    /** The unpack alignment GL starts with. */
    const val UNPACK_ALIGNMENT_DEFAULT = 4

    private val ids = IntArray(1)

    /** @return a new texture name. */
    @JvmStatic
    fun generate(): Int {
        GlesBackend.gl.glGenTextures(1, ids, 0)
        return ids[0]
    }

    /** Deletes the texture [id] and forgets its bindings in the state cache. */
    @JvmStatic
    fun delete(id: Int) {
        GlesBackend.state.onTextureDeleted(id)
        ids[0] = id
        GlesBackend.gl.glDeleteTextures(1, ids, 0)
    }

    /** Binds [id] to `GL_TEXTURE_2D` on the active texture unit, skipped if the cache knows it is bound. */
    @JvmStatic
    fun bind(id: Int) = GlesBackend.state.bindTexture(GLES20.GL_TEXTURE_2D, id)

    /**
     * Makes [glTextureUnit] the active texture unit, then binds [id] to `GL_TEXTURE_2D` on it.
     *
     * @param glTextureUnit from `GL_TEXTURE0` to `GL_TEXTURE31`.
     */
    @JvmStatic
    fun bind(id: Int, glTextureUnit: Int) {
        GlesBackend.state.activeTexture(glTextureUnit - GLES20.GL_TEXTURE0)
        bind(id)
    }

    /** Sets the filters and wrap modes of the texture bound to `GL_TEXTURE_2D`. */
    @JvmStatic
    fun parameters(minFilter: Int, magFilter: Int, wrapS: Int, wrapT: Int) {
        val gl = GlesBackend.gl
        gl.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minFilter)
        gl.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, magFilter)
        gl.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrapS)
        gl.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrapT)
    }

    /**
     * Binds [id] to `GL_TEXTURE_EXTERNAL_OES` on the active texture unit and gives it the settings an
     * external texture needs: linear filters, wrap modes clamped to the edge.
     */
    @JvmStatic
    fun setUpExternal(id: Int) {
        val target = GlesLegacyTextureView.TEXTURE_EXTERNAL_OES
        GlesBackend.state.bindTexture(target, id)
        val gl = GlesBackend.gl
        gl.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        gl.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        gl.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        gl.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    /** Sets `GL_UNPACK_ALIGNMENT` for the uploads that follow. */
    @JvmStatic
    fun unpackAlignment(alignment: Int) = GlesBackend.gl.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, alignment)

    /** Allocates mutable storage of [width] x [height] for the bound texture, without pixels. */
    @JvmStatic
    fun allocate(width: Int, height: Int, pixelFormat: PixelFormat) = GlesBackend.gl.glTexImage2D(
        GLES20.GL_TEXTURE_2D, 0, pixelFormat.getGLInternalFormat(), width, height, 0,
        pixelFormat.getGLFormat(), pixelFormat.getGLType(), null
    )

    /**
     * Uploads [bitmap] as the whole bound texture in [pixelFormat]. Does not premultiply the alpha
     * channel, unlike [texImage2DPremultiplied].
     */
    @JvmStatic
    fun texImage2D(bitmap: Bitmap, pixelFormat: PixelFormat) {
        val pixels = GLHelper.getPixels(bitmap, pixelFormat, ByteOrder.BIG_ENDIAN)
        GlesBackend.gl.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, pixelFormat.getGLInternalFormat(), bitmap.width, bitmap.height, 0,
            pixelFormat.getGLFormat(), pixelFormat.getGLType(), pixels
        )
    }

    /**
     * Uploads [bitmap] at ([x], [y]) of the bound texture in [pixelFormat]. Does not premultiply the
     * alpha channel, unlike [texSubImage2DPremultiplied].
     */
    @JvmStatic
    fun texSubImage2D(x: Int, y: Int, bitmap: Bitmap, pixelFormat: PixelFormat) {
        val pixels = GLHelper.getPixels(bitmap, pixelFormat, ByteOrder.BIG_ENDIAN)
        GlesBackend.gl.glTexSubImage2D(
            GLES20.GL_TEXTURE_2D, 0, x, y, bitmap.width, bitmap.height,
            pixelFormat.getGLFormat(), pixelFormat.getGLType(), pixels
        )
    }

    /** Uploads [bitmap] as the whole bound texture, with the alpha channel premultiplied. */
    @JvmStatic
    fun texImage2DPremultiplied(bitmap: Bitmap) = GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

    /** Uploads [bitmap] at ([x], [y]) of the bound texture as [glFormat]/[glType], with the alpha channel premultiplied. */
    @JvmStatic
    fun texSubImage2DPremultiplied(x: Int, y: Int, bitmap: Bitmap, glFormat: Int, glType: Int) =
        GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, x, y, bitmap, glFormat, glType)
}
