package org.andengine.graphics.gles

import android.opengl.GLES20
import org.andengine.graphics.BufferTarget
import org.andengine.graphics.BufferUsage
import org.andengine.graphics.DeviceCapabilities
import org.andengine.graphics.GpuBuffer
import org.andengine.graphics.GpuFence
import org.andengine.graphics.GpuProgram
import org.andengine.graphics.GpuQuery
import org.andengine.graphics.GraphicsDevice
import org.andengine.graphics.RenderTarget
import org.andengine.graphics.Sampler
import org.andengine.graphics.Texture
import org.andengine.graphics.TextureFilter
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.TextureSource
import org.andengine.graphics.TextureWrap

/**
 * [GraphicsDevice] on top of [GlesBackend]. Creating resources and reading [capabilities] needs a
 * current context (GL thread, after [GlesBackend.onContextCreated]); the instance itself can be
 * obtained at any time.
 */
class GlesGraphicsDevice private constructor() : GraphicsDevice {

    private var capsFor: GlesCapabilities? = null
    private var cachedCapabilities: DeviceCapabilities? = null

    override val capabilities: DeviceCapabilities
        get() {
            val caps = GlesBackend.caps
            val cached = cachedCapabilities
            if (cached != null && capsFor === caps) return cached

            val scratch = IntArray(1)
            GlesBackend.gl.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, scratch, 0)
            val created = DeviceCapabilities(
                maxTextureSize = scratch[0],
                supportsStorageBuffers = caps.hasGl31,
                supportsTimerQuery = caps.hasTimerQuery
            )
            capsFor = caps
            cachedCapabilities = created
            return created
        }

    override fun createTexture(
        width: Int,
        height: Int,
        format: TextureFormat,
        layers: Int,
        filter: TextureFilter,
        wrap: TextureWrap,
        source: TextureSource?,
        label: String
    ): Texture {
        val max = capabilities.maxTextureSize
        require(width in 1..max && height in 1..max) { "Texture '$label' size ${width}x$height outside 1..$max" }
        require(layers >= 1) { "Texture '$label' needs at least one layer, got $layers" }
        return GlesTexture(width, height, format, layers, filter, wrap, source, label)
    }

    override fun createBuffer(target: BufferTarget, usage: BufferUsage, sizeBytes: Int, label: String): GpuBuffer =
        GlesBuffer(target, usage, sizeBytes, label)

    override fun createProgram(
        vertexSource: String,
        fragmentSource: String,
        label: String,
        samplerUnits: Map<String, Int>
    ): GpuProgram = GlesProgram(vertexSource, fragmentSource, label, samplerUnits)

    override fun createSampler(filter: TextureFilter, wrap: TextureWrap, label: String): Sampler =
        GlesSampler(filter, wrap, label)

    override fun createQuery(label: String): GpuQuery = GlesQuery(label)

    override fun createFence(label: String): GpuFence = GlesFence(label)

    override fun createRenderTarget(width: Int, height: Int, depth: Boolean, label: String): RenderTarget {
        val max = capabilities.maxTextureSize
        require(width in 1..max && height in 1..max) { "Render target '$label' size ${width}x$height outside 1..$max" }
        return GlesRenderTarget(width, height, depth, label)
    }

    companion object {
        @JvmStatic
        val instance = GlesGraphicsDevice()
    }
}
