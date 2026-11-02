package org.andengine.graphics.gles

import android.opengl.GLES20
import org.andengine.graphics.BufferTarget
import org.andengine.graphics.GpuBuffer
import org.andengine.graphics.Pipeline
import org.andengine.graphics.RenderContext
import org.andengine.graphics.RenderTarget
import org.andengine.graphics.Sampler
import org.andengine.graphics.Texture
import org.andengine.graphics.debug.RenderStats

/**
 * [RenderContext] that executes immediately on the current GL context. All state goes through
 * [GlesBackend.state]; pipeline state and the VAO are applied lazily at the next draw, and only
 * when the pipeline, a buffer or the GL context changed, or a new pass began (a new pass re-applies
 * only the pipeline). [endPass] unbinds the VAO so code outside the render context cannot change
 * the cached VAOs, and resets texture unit 0 and the samplers it bound, which legacy code assumes.
 * GL thread only.
 */
class GlesRenderContext private constructor() : RenderContext {

    val vertexArrays = GlesVertexArrayCache()

    var isInPass = false
        private set

    private var pipeline: Pipeline? = null
    private var pipelineDirty = true
    private var primitiveMode = GLES20.GL_TRIANGLES

    private var vertexBuffer: GlesBuffer? = null
    private var indexBuffer: GlesBuffer? = null
    private var vaoDirty = true
    private var vaoId = 0

    /** Highest texture slot [setTexture] touched since the last [endPass], or -1. */
    private var highestTextureSlot = -1

    /** The state cache the pipeline and VAO were applied to; a new one means a new context. */
    private var appliedState: GlesStateCache? = null

    init {
        GlesBackend.registry.register(vertexArrays)
    }

    override fun beginPass(target: RenderTarget?, clearColor: FloatArray?, clearDepth: Boolean) {
        check(!isInPass) { "beginPass while a pass is active" }
        val state = GlesBackend.state
        // Queued drawing belongs to what came before this pass.
        state.runBeforeExternalChange()
        syncContext(state)

        // Code outside the render context may have changed program/blend/depth/cull between
        // passes; the state cache dedups, so re-applying at the next draw is cheap.
        pipelineDirty = true

        if (target == null) {
            check(GlesBackend.surfaceWidth > 0 && GlesBackend.surfaceHeight > 0) {
                "beginPass on the screen before the surface size is known " +
                    "(${GlesBackend.surfaceWidth}x${GlesBackend.surfaceHeight}); EngineRenderer.onSurfaceChanged sets it"
            }
            state.bindFramebuffer(0)
            state.viewport(0, 0, GlesBackend.surfaceWidth, GlesBackend.surfaceHeight)
        } else {
            val t = target as GlesRenderTarget
            check(!t.isDisposed) { "Render target '${t.label}' is disposed" }
            // 0 would silently draw to the screen.
            check(t.framebufferId != 0) {
                "Render target '${t.label}' has no framebuffer: its restore after the last context loss failed"
            }
            state.bindFramebuffer(t.framebufferId)
            state.viewport(0, 0, t.width, t.height)
        }
        state.setScissorEnabled(false)

        var mask = 0
        if (clearColor != null) {
            require(clearColor.size >= 4) { "Clear color needs 4 components, got ${clearColor.size}" }
            state.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
            mask = mask or GLES20.GL_COLOR_BUFFER_BIT
        }
        if (clearDepth) {
            // glClear honours the depth mask; the pipeline sets its own again at the next draw.
            state.depthMask(true)
            state.clearDepth(1f)
            mask = mask or GLES20.GL_DEPTH_BUFFER_BIT
        }
        if (mask != 0) state.clear(mask)

        isInPass = true
    }

    override fun beginInlinePass() {
        check(!isInPass) { "beginInlinePass while a pass is active" }
        syncContext(GlesBackend.state)
        // Same as beginPass: state set outside may differ from the applied pipeline.
        pipelineDirty = true
        isInPass = true
    }

    override fun endPass() {
        check(isInPass) { "endPass without beginPass" }
        val state = GlesBackend.state
        // Queued drawing of this pass goes to its target before the pass ends.
        state.runBeforeExternalChange()
        isInPass = false
        state.bindVertexArray(0)
        // Legacy code binds textures on unit 0 without setting it, and expects the texture's own
        // filter/wrap, so leave no sampler behind on the slots this pass used.
        val highest = highestTextureSlot
        highestTextureSlot = -1
        for (slot in 0..highest) state.bindSampler(slot, 0)
        state.activeTexture(0)
    }

    /**
     * Applies the pipeline again at the next draw, for code that changed program, blend, depth or
     * cull state behind the render context's back during a pass.
     */
    fun invalidatePipeline() {
        pipelineDirty = true
    }

    override fun setPipeline(p: Pipeline) {
        val current = pipeline
        if (p === current) return
        if (current == null || current.layout != p.layout) vaoDirty = true
        pipeline = p
        pipelineDirty = true
    }

    override fun setTexture(slot: Int, texture: Texture, sampler: Sampler?) {
        require(slot >= 0) { "Texture slot must not be negative, got $slot" }
        check(!texture.isDisposed) { "Texture '${texture.label}' is disposed" }
        val glTarget: Int
        val textureId: Int
        when (texture) {
            is GlesTexture -> {
                glTarget = texture.glTarget
                textureId = texture.textureId
            }
            is GlesLegacyTextureView -> {
                glTarget = texture.glTarget
                textureId = texture.textureId
            }
            else -> throw IllegalArgumentException("Texture '${texture.label}' is not a GLES texture")
        }
        val s = sampler as GlesSampler?
        if (s != null) check(!s.isDisposed) { "Sampler '${s.label}' is disposed" }

        val state = GlesBackend.state
        state.activeTexture(slot)
        state.bindTexture(glTarget, textureId)
        state.bindSampler(slot, s?.samplerId ?: 0)
        if (slot > highestTextureSlot) highestTextureSlot = slot
    }

    override fun setVertexBuffer(buffer: GpuBuffer) {
        require(buffer.target == BufferTarget.Vertex) { "'${buffer.label}' is not a vertex buffer" }
        val b = buffer as GlesBuffer
        if (b !== vertexBuffer) {
            vertexBuffer = b
            vaoDirty = true
        }
    }

    override fun setIndexBuffer(buffer: GpuBuffer) {
        require(buffer.target == BufferTarget.Index) { "'${buffer.label}' is not an index buffer" }
        val b = buffer as GlesBuffer
        if (b !== indexBuffer) {
            indexBuffer = b
            vaoDirty = true
        }
    }

    override fun setUniformMatrix4(location: Int, matrix: FloatArray) {
        require(matrix.size >= 16) { "A mat4 needs 16 values, got ${matrix.size}" }
        val p = pipeline ?: throw IllegalStateException("setUniformMatrix4 without a pipeline")
        if (location < 0) return
        val program = p.program as GlesProgram
        check(!program.isDisposed) { "Pipeline uses the disposed program '${program.label}'" }
        // Uniform calls go to the program in use; the draw applies the same program again (deduped).
        program.use()
        GlesBackend.gl.glUniformMatrix4fv(location, 1, false, matrix, 0)
    }

    override fun setUniformFloat(location: Int, components: Int, values: FloatArray, offset: Int) {
        require(components in 1..4) { "A float uniform has 1 to 4 components, got $components" }
        require(offset >= 0 && offset + components <= values.size) {
            "$components values from $offset exceed ${values.size} values"
        }
        val p = pipeline ?: throw IllegalStateException("setUniformFloat without a pipeline")
        if (location < 0) return
        val program = p.program as GlesProgram
        check(!program.isDisposed) { "Pipeline uses the disposed program '${program.label}'" }
        program.use()
        val gl = GlesBackend.gl
        when (components) {
            1 -> gl.glUniform1f(location, values[offset])
            2 -> gl.glUniform2f(location, values[offset], values[offset + 1])
            3 -> gl.glUniform3f(location, values[offset], values[offset + 1], values[offset + 2])
            else -> gl.glUniform4f(location, values[offset], values[offset + 1], values[offset + 2], values[offset + 3])
        }
    }

    override fun setUniformBlock(binding: Int, buffer: GpuBuffer) {
        require(binding >= 0) { "Uniform block binding must not be negative, got $binding" }
        require(buffer.target == BufferTarget.Uniform) { "'${buffer.label}' is not a uniform buffer" }
        val b = buffer as GlesBuffer
        check(!b.isDisposed) { "Buffer '${b.label}' is disposed" }
        GlesBackend.state.bindUniformBufferBase(binding, b.bufferId)
    }

    override fun setScissor(x: Int, y: Int, width: Int, height: Int) {
        val state = GlesBackend.state
        state.setScissorEnabled(true)
        state.scissor(x, y, width, height)
    }

    override fun clearScissor() {
        GlesBackend.state.setScissorEnabled(false)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        GlesBackend.state.viewport(x, y, width, height)
    }

    override fun draw(firstVertex: Int, vertexCount: Int) {
        prepareDraw()
        GlesBackend.gl.glDrawArrays(primitiveMode, firstVertex, vertexCount)
        RenderStats.draw(vertexCount)
    }

    override fun drawIndexed(firstIndex: Int, indexCount: Int, baseVertex: Int) {
        val ib = indexBuffer ?: throw IllegalStateException("drawIndexed without an index buffer")
        check(!ib.isDisposed) { "Index buffer '${ib.label}' is disposed" }
        check(baseVertex == 0 || GlesBackend.caps.hasGl32) {
            "drawIndexed with baseVertex $baseVertex needs OpenGL ES 3.2, the context is ${GlesBackend.caps.version}"
        }
        prepareDraw()

        val offset = firstIndex * INDEX_SIZE_BYTES
        if (baseVertex == 0) {
            GlesBackend.gl.glDrawElements(primitiveMode, indexCount, GLES20.GL_UNSIGNED_SHORT, offset)
        } else {
            GlesBackend.gl32!!.glDrawElementsInstancedBaseVertex(
                primitiveMode, indexCount, GLES20.GL_UNSIGNED_SHORT, offset, 1, baseVertex
            )
        }
        RenderStats.draw(indexCount)
    }

    private fun prepareDraw() {
        check(isInPass) { "Draw outside of a pass" }
        val p = pipeline ?: throw IllegalStateException("Draw without a pipeline")
        val vb = vertexBuffer ?: throw IllegalStateException("Draw without a vertex buffer")
        check(!vb.isDisposed) { "Vertex buffer '${vb.label}' is disposed" }

        val state = GlesBackend.state
        syncContext(state)
        if (pipelineDirty) {
            GlesPipeline.apply(p, state)
            primitiveMode = GlesPipeline.primitiveMode(p.primitive)
            pipelineDirty = false
        }
        if (vaoDirty) {
            val ib = indexBuffer
            // A disposed index buffer only matters to drawIndexed, which checked it already.
            vaoId = vertexArrays.obtain(p.layout, vb, if (ib != null && ib.isDisposed) null else ib)
            vaoDirty = false
        }
        // Cheap when unchanged; also rebinds after something bound VAO 0 (e.g. an index upload).
        state.bindVertexArray(vaoId)
    }

    /** After a new context everything the GL had is gone: apply pipeline and VAO again. */
    private fun syncContext(state: GlesStateCache) {
        if (state === appliedState) return
        appliedState = state
        pipelineDirty = true
        vaoDirty = true
    }

    companion object {
        private const val INDEX_SIZE_BYTES = 2

        @JvmStatic
        val instance = GlesRenderContext()
    }
}
