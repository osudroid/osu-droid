package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES32
import org.andengine.graphics.Sampler
import org.andengine.graphics.TextureFilter
import org.andengine.graphics.TextureWrap

/** GL sampler object, recreated after a context loss. GL thread only. */
class GlesSampler(
    val filter: TextureFilter,
    val wrap: TextureWrap,
    override val label: String
) : Sampler, GlesRestorable {

    /** The GL sampler; 0 while there is none. */
    var samplerId = 0
        private set

    override var isDisposed = false
        private set

    override val restorePhase: RestorePhase get() = RestorePhase.Samplers

    init {
        create()
        GlesBackend.registry.register(this)
    }

    private fun create() {
        val gl = GlesBackend.gl
        val ids = IntArray(1)
        gl.glGenSamplers(1, ids, 0)
        val id = ids[0]
        gl.glSamplerParameteri(id, GLES20.GL_TEXTURE_MIN_FILTER, GlesResources.minFilter(filter))
        gl.glSamplerParameteri(id, GLES20.GL_TEXTURE_MAG_FILTER, GlesResources.magFilter(filter))
        gl.glSamplerParameteri(id, GLES20.GL_TEXTURE_WRAP_S, GlesResources.wrap(wrap))
        gl.glSamplerParameteri(id, GLES20.GL_TEXTURE_WRAP_T, GlesResources.wrap(wrap))
        samplerId = id
        GlesResources.label(GLES32.GL_SAMPLER, id, label)
    }

    override fun onContextLost() {
        samplerId = 0
    }

    override fun restore() = create()

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        GlesBackend.registry.unregister(this)
        val id = samplerId
        samplerId = 0
        if (id != 0) {
            GlesBackend.state.onSamplerDeleted(id)
            GlesBackend.gl.glDeleteSamplers(1, intArrayOf(id), 0)
        }
    }

    override fun toString() = "GlesSampler($label, $filter, $wrap)"
}
