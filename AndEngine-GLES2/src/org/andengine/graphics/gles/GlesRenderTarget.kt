package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import org.andengine.graphics.RenderTarget
import org.andengine.graphics.TextureFilter
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.TextureWrap

/**
 * Framebuffer with an RGBA8 [color] texture and an optional 24-bit depth renderbuffer. After a
 * context loss the framebuffer is rebuilt (its texture is restored earlier, empty). The previous
 * framebuffer binding is kept. GL thread only.
 */
class GlesRenderTarget(
    override val width: Int,
    override val height: Int,
    val hasDepth: Boolean,
    override val label: String
) : RenderTarget, GlesRestorable {

    override val color = GlesTexture(
        width, height, TextureFormat.RGBA8, 1, TextureFilter.Linear, TextureWrap.Clamp, null, "$label.color"
    )

    /** The GL framebuffer; 0 while there is none. */
    var framebufferId = 0
        private set

    private var depthRenderbufferId = 0

    override var isDisposed = false
        private set

    override val restorePhase: RestorePhase get() = RestorePhase.Framebuffers

    init {
        try {
            create()
        } catch (e: IllegalStateException) {
            color.dispose()
            throw e
        }
        GlesBackend.registry.register(this)
    }

    private fun create() {
        val gl = GlesBackend.gl
        val state = GlesBackend.state
        val previous = state.currentFramebuffer.coerceAtLeast(0)

        val ids = IntArray(1)
        gl.glGenFramebuffers(1, ids, 0)
        framebufferId = ids[0]
        state.bindFramebuffer(framebufferId)
        gl.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, color.ownTextureId, 0)

        if (hasDepth) {
            gl.glGenRenderbuffers(1, ids, 0)
            depthRenderbufferId = ids[0]
            state.bindRenderbuffer(depthRenderbufferId)
            gl.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, width, height)
            gl.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRenderbufferId)
        }

        val status = gl.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        state.bindFramebuffer(previous)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            deleteGlObjects()
            throw IllegalStateException("Render target '$label' is incomplete: 0x${Integer.toHexString(status)}")
        }
        GlesResources.label(GLES20.GL_FRAMEBUFFER, framebufferId, label)
    }

    private fun deleteGlObjects() {
        val gl = GlesBackend.gl
        val state = GlesBackend.state
        if (framebufferId != 0) {
            state.onFramebufferDeleted(framebufferId)
            gl.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
            framebufferId = 0
        }
        if (depthRenderbufferId != 0) {
            state.onRenderbufferDeleted(depthRenderbufferId)
            gl.glDeleteRenderbuffers(1, intArrayOf(depthRenderbufferId), 0)
            depthRenderbufferId = 0
        }
    }

    override fun onContextLost() {
        framebufferId = 0
        depthRenderbufferId = 0
    }

    override fun restore() = create()

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        GlesBackend.registry.unregister(this)
        deleteGlObjects()
        color.dispose()
    }

    override fun toString() = "GlesRenderTarget($label, ${width}x$height, depth=$hasDepth)"
}
