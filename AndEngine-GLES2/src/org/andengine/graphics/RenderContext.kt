package org.andengine.graphics

/**
 * Records the draws of a frame, pass by pass. Bindings ([setPipeline], [setTexture], buffers,
 * scissor, viewport) stay in effect until changed, also across passes, except the samplers that
 * [endPass] unbinds. Draws are only allowed
 * between [beginPass] or [beginInlinePass] and [endPass]. The interface never returns backend state, so a backend may
 * execute immediately or record commands. Call on the render thread.
 */
interface RenderContext {

    /**
     * Starts drawing into [target], or the screen when it is `null`, with the viewport covering
     * the whole target and scissoring off. Clears the color to [clearColor] (r, g, b, a) when it
     * is not `null`, and the depth to 1 when [clearDepth] is set.
     */
    fun beginPass(target: RenderTarget?, clearColor: FloatArray?, clearDepth: Boolean)

    /**
     * Starts a pass that draws into whatever framebuffer, viewport and scissor are current, as set
     * by drawing code outside the render context, and clears nothing. For draws interleaved with
     * such code. Ends with [endPass] like any pass.
     */
    fun beginInlinePass()

    /**
     * Ends the current pass and leaves the GL as legacy drawing code expects it: no vertex array
     * bound, texture unit 0 active, and no sampler bound on any slot [setTexture] used since the
     * last [endPass]. Textures stay bound, but a sampler must be passed to [setTexture] again in
     * the next pass.
     */
    fun endPass()

    fun setPipeline(p: Pipeline)

    /** Binds [texture] to [slot]; [sampler] overrides the texture's own filter/wrap, `null` uses them. */
    fun setTexture(slot: Int, texture: Texture, sampler: Sampler?)

    /** A [BufferTarget.Vertex] buffer laid out as the pipeline's [VertexLayout]. */
    fun setVertexBuffer(buffer: GpuBuffer)

    /** A [BufferTarget.Index] buffer of 16-bit unsigned indices, used by [drawIndexed]. */
    fun setIndexBuffer(buffer: GpuBuffer)

    /**
     * Sets the `mat4` uniform at [location] of the current pipeline's program to the first 16
     * values of [matrix] (column-major). The value belongs to the program and stays until set
     * again or the context is lost. A [location] of -1 is ignored.
     *
     * @throws IllegalStateException without a pipeline.
     */
    fun setUniformMatrix4(location: Int, matrix: FloatArray)

    /** Binds a [BufferTarget.Uniform] buffer to the uniform block [binding]. */
    fun setUniformBlock(binding: Int, buffer: GpuBuffer)

    /** Restricts drawing to the rectangle, in pixels from the bottom left of the target. */
    fun setScissor(x: Int, y: Int, width: Int, height: Int)

    fun clearScissor()

    fun setViewport(x: Int, y: Int, width: Int, height: Int)

    fun draw(firstVertex: Int, vertexCount: Int)

    /**
     * Draws [indexCount] indices starting at [firstIndex], each offset by [baseVertex].
     *
     * @throws IllegalStateException if [baseVertex] is not 0 and the device cannot offset indices
     * (OpenGL ES below 3.2).
     */
    fun drawIndexed(firstIndex: Int, indexCount: Int, baseVertex: Int = 0)
}
