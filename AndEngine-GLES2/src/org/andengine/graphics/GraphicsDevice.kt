package org.andengine.graphics

/** Creates GPU resources. Call on the render thread. */
interface GraphicsDevice {

    fun createTexture(
        width: Int,
        height: Int,
        format: TextureFormat,
        layers: Int = 1,
        filter: TextureFilter = TextureFilter.Linear,
        wrap: TextureWrap = TextureWrap.Clamp,
        source: TextureSource? = null,
        label: String
    ): Texture

    /** @throws IllegalStateException for [BufferTarget.Storage] if the device has no storage buffers. */
    fun createBuffer(target: BufferTarget, usage: BufferUsage, sizeBytes: Int, label: String): GpuBuffer

    /**
     * [samplerUnits] maps sampler uniform names to the texture slot (see [RenderContext.setTexture])
     * each one reads; set once when the program is built (and again after a context loss). Samplers
     * not listed read slot 0.
     *
     * @throws IllegalStateException if the source needs a shading language version the device does
     * not support, or if compiling or linking fails.
     */
    fun createProgram(
        vertexSource: String,
        fragmentSource: String,
        label: String,
        samplerUnits: Map<String, Int> = emptyMap()
    ): GpuProgram

    fun createSampler(filter: TextureFilter, wrap: TextureWrap, label: String): Sampler

    /** @throws IllegalStateException if [DeviceCapabilities.supportsTimerQuery] is false. */
    fun createQuery(label: String): GpuQuery

    fun createFence(label: String): GpuFence

    fun createRenderTarget(width: Int, height: Int, depth: Boolean, label: String): RenderTarget

    val capabilities: DeviceCapabilities
}

class DeviceCapabilities(
    val maxTextureSize: Int,
    val supportsStorageBuffers: Boolean,
    val supportsTimerQuery: Boolean
)
