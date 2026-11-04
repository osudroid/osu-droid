package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import org.andengine.graphics.BufferUsage
import org.andengine.graphics.TextureFilter
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.TextureWrap

/** Mapping of the neutral enums to GL values, plus debug labels. */
object GlesResources {

    fun internalFormat(format: TextureFormat): Int = when (format) {
        TextureFormat.RGBA8 -> GLES30.GL_RGBA8
        TextureFormat.R8 -> GLES30.GL_R8
    }

    fun pixelFormat(format: TextureFormat): Int = when (format) {
        TextureFormat.RGBA8 -> GLES20.GL_RGBA
        TextureFormat.R8 -> GLES30.GL_RED
    }

    fun minFilter(filter: TextureFilter): Int = when (filter) {
        TextureFilter.Nearest -> GLES20.GL_NEAREST
        TextureFilter.Linear -> GLES20.GL_LINEAR
        TextureFilter.LinearMipmapLinear -> GLES20.GL_LINEAR_MIPMAP_LINEAR
    }

    fun magFilter(filter: TextureFilter): Int =
        if (filter == TextureFilter.Nearest) GLES20.GL_NEAREST else GLES20.GL_LINEAR

    fun wrap(wrap: TextureWrap): Int = when (wrap) {
        TextureWrap.Clamp -> GLES20.GL_CLAMP_TO_EDGE
        TextureWrap.Repeat -> GLES20.GL_REPEAT
        TextureWrap.MirroredRepeat -> GLES20.GL_MIRRORED_REPEAT
    }

    fun usage(usage: BufferUsage): Int = when (usage) {
        BufferUsage.Static -> GLES20.GL_STATIC_DRAW
        BufferUsage.Dynamic -> GLES20.GL_DYNAMIC_DRAW
        BufferUsage.Stream -> GLES20.GL_STREAM_DRAW
    }

    /** Labels a GL object for debuggers when debug output is on and the context is ES 3.2. */
    fun label(identifier: Int, name: Int, label: String) {
        if (!GlesDebugOptions.debugOutput || !GlesBackend.caps.hasDebugOutput) return
        GlesBackend.gl32?.glObjectLabel(identifier, name, label.length, label)
    }
}
