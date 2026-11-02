package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import org.andengine.graphics.debug.RenderStats

/**
 * The single owner of GL state. Every setter forwards to [gl] only when the value really changes
 * and counts the matching [RenderStats] entry. Unknown state is `-1` (or `NaN` for floats), so
 * after [invalidate] the next call always reaches GL. GL thread only.
 *
 * Drawing that is queued instead of issued at once (the quad batch) hooks into
 * [beforeExternalChange], so it is drawn before anything changes the state it depends on; the
 * scissor has its own hook, [beforeScissorChange], since not all queued work depends on it.
 */
class GlesStateCache(private val gl: GL30) {

    /**
     * Runs once right before a state change that really reaches GL (a skipped duplicate does not
     * count; the scissor has [beforeScissorChange] instead), before a clear, and before an object is forgotten as deleted; it is cleared before
     * it runs, so it is never re-entered. Used to draw queued work first: the callee must leave
     * every value this cache tracks as it found it (see [saveDrawState]), because the change that
     * triggered it goes on with values read before. Null while nothing waits.
     */
    @JvmField
    var beforeExternalChange: Runnable? = null

    /**
     * Like [beforeExternalChange], but runs only before a change of the scissor test or box (and
     * [beforeExternalChange] does not): set only while queued work depends on the scissor.
     */
    @JvmField
    var beforeScissorChange: Runnable? = null

    private val scratch = IntArray(1)
    private val unitCount: Int

    private var program = UNKNOWN
    private var arrayBuffer = UNKNOWN
    private var elementBuffer = UNKNOWN
    private var uniformBuffer = UNKNOWN
    private var pixelUnpackBuffer = UNKNOWN
    private var pixelPackBuffer = UNKNOWN
    private var storageBuffer = UNKNOWN
    private var vertexArray = UNKNOWN
    private var framebuffer = UNKNOWN
    private var renderbuffer = UNKNOWN

    private var activeUnit = UNKNOWN
    // Layout: unit * TEXTURE_TARGETS + targetIndex.
    private val textures: IntArray
    private val samplers: IntArray
    private val uniformBindings = IntArray(UNIFORM_BINDINGS)

    private var blendEnabled = UNKNOWN
    private var blendSrcRgb = UNKNOWN
    private var blendDstRgb = UNKNOWN
    private var blendSrcAlpha = UNKNOWN
    private var blendDstAlpha = UNKNOWN

    private var depthTest = UNKNOWN
    private var depthFunc = UNKNOWN
    private var depthMask = UNKNOWN
    private var clearDepth = Float.NaN
    private var cullFace = UNKNOWN
    private var cullMode = UNKNOWN
    private var frontFace = UNKNOWN
    private var dither = UNKNOWN
    private var lineWidth = Float.NaN

    private var scissor = UNKNOWN
    private var scissorX = UNKNOWN
    private var scissorY = UNKNOWN
    private var scissorW = UNKNOWN
    private var scissorH = UNKNOWN

    var viewportX = UNKNOWN
        private set
    var viewportY = UNKNOWN
        private set
    var viewportWidth = UNKNOWN
        private set
    var viewportHeight = UNKNOWN
        private set

    // Per attribute bit: known (we set it) and enabled.
    private var attribKnown = 0
    private var attribEnabled = 0

    private var clearR = Float.NaN
    private var clearG = Float.NaN
    private var clearB = Float.NaN
    private var clearA = Float.NaN

    val currentProgram: Int get() = program
    val currentArrayBuffer: Int get() = arrayBuffer
    val currentElementBuffer: Int get() = elementBuffer
    val currentVertexArray: Int get() = vertexArray
    val currentFramebuffer: Int get() = framebuffer

    /** Zero-based active texture unit, `-1` when unknown. */
    val currentActiveTextureUnit: Int get() = activeUnit

    val isBlendEnabled: Boolean get() = blendEnabled == 1
    val isDepthTestEnabled: Boolean get() = depthTest == 1
    val isScissorEnabled: Boolean get() = scissor == 1
    val isCullFaceEnabled: Boolean get() = cullFace == 1
    val isDitherEnabled: Boolean get() = dither == 1

    val currentBlendSrcRgb: Int get() = blendSrcRgb
    val currentBlendDstRgb: Int get() = blendDstRgb
    val currentBlendSrcAlpha: Int get() = blendSrcAlpha
    val currentBlendDstAlpha: Int get() = blendDstAlpha

    init {
        gl.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, scratch, 0)
        unitCount = scratch[0].coerceIn(MIN_UNITS, MAX_UNITS)
        textures = IntArray(unitCount * TEXTURE_TARGETS)
        samplers = IntArray(unitCount)
        invalidate()
    }

    /**
     * Runs [beforeExternalChange] now, if set: for callers about to do something the queued work
     * must come before, but that does not go through a setter of this cache.
     */
    fun runBeforeExternalChange() {
        val hook = beforeExternalChange ?: return
        beforeExternalChange = null
        hook.run()
    }

    private fun runBeforeScissorChange() {
        val hook = beforeScissorChange ?: return
        beforeScissorChange = null
        hook.run()
    }

    /** Marks everything unknown; the next call of every setter goes to GL. */
    fun invalidate() {
        program = UNKNOWN
        arrayBuffer = UNKNOWN
        elementBuffer = UNKNOWN
        uniformBuffer = UNKNOWN
        pixelUnpackBuffer = UNKNOWN
        pixelPackBuffer = UNKNOWN
        storageBuffer = UNKNOWN
        vertexArray = UNKNOWN
        framebuffer = UNKNOWN
        renderbuffer = UNKNOWN
        activeUnit = UNKNOWN
        textures.fill(UNKNOWN)
        samplers.fill(UNKNOWN)
        uniformBindings.fill(UNKNOWN)
        blendEnabled = UNKNOWN
        blendSrcRgb = UNKNOWN
        blendDstRgb = UNKNOWN
        blendSrcAlpha = UNKNOWN
        blendDstAlpha = UNKNOWN
        depthTest = UNKNOWN
        depthFunc = UNKNOWN
        depthMask = UNKNOWN
        clearDepth = Float.NaN
        cullFace = UNKNOWN
        cullMode = UNKNOWN
        frontFace = UNKNOWN
        dither = UNKNOWN
        lineWidth = Float.NaN
        scissor = UNKNOWN
        scissorX = UNKNOWN
        scissorY = UNKNOWN
        scissorW = UNKNOWN
        scissorH = UNKNOWN
        viewportX = UNKNOWN
        viewportY = UNKNOWN
        viewportWidth = UNKNOWN
        viewportHeight = UNKNOWN
        attribKnown = 0
        attribEnabled = 0
        clearR = Float.NaN
        clearG = Float.NaN
        clearB = Float.NaN
        clearA = Float.NaN
    }

    fun useProgram(id: Int) {
        if (program == id) return
        runBeforeExternalChange()
        program = id
        gl.glUseProgram(id)
        RenderStats.programSwitch()
    }

    fun bindArrayBuffer(id: Int) {
        if (arrayBuffer == id) return
        runBeforeExternalChange()
        arrayBuffer = id
        gl.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        RenderStats.bufferBind()
    }

    fun bindElementBuffer(id: Int) {
        if (elementBuffer == id) return
        runBeforeExternalChange()
        elementBuffer = id
        gl.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, id)
        RenderStats.bufferBind()
    }

    fun bindUniformBuffer(id: Int) {
        if (uniformBuffer == id) return
        uniformBuffer = id
        gl.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, id)
        RenderStats.bufferBind()
    }

    fun bindPixelUnpackBuffer(id: Int) {
        if (pixelUnpackBuffer == id) return
        pixelUnpackBuffer = id
        gl.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, id)
        RenderStats.bufferBind()
    }

    fun bindPixelPackBuffer(id: Int) {
        if (pixelPackBuffer == id) return
        pixelPackBuffer = id
        gl.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, id)
        RenderStats.bufferBind()
    }

    /**
     * Binds [id] to the indexed uniform block [index] (`glBindBufferBase`), which also makes it the
     * generic `GL_UNIFORM_BUFFER` binding. Indices 0 until 16 are tracked; others pass through.
     */
    fun bindUniformBufferBase(index: Int, id: Int) {
        if (index in 0 until UNIFORM_BINDINGS) {
            if (uniformBindings[index] == id) return
            uniformBindings[index] = id
        }
        uniformBuffer = id
        gl.glBindBufferBase(GLES30.GL_UNIFORM_BUFFER, index, id)
        RenderStats.bufferBind()
    }

    /** Needs ES 3.1. */
    fun bindShaderStorageBuffer(id: Int) {
        if (storageBuffer == id) return
        storageBuffer = id
        gl.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, id)
        RenderStats.bufferBind()
    }

    fun bindVertexArray(id: Int) {
        if (vertexArray == id) return
        runBeforeExternalChange()
        vertexArray = id
        // The element buffer binding is part of the VAO state.
        elementBuffer = UNKNOWN
        // Attribute enable state is per VAO as well.
        attribKnown = 0
        attribEnabled = 0
        gl.glBindVertexArray(id)
    }

    fun bindFramebuffer(id: Int) {
        if (framebuffer == id) return
        runBeforeExternalChange()
        framebuffer = id
        gl.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, id)
        RenderStats.framebufferBind()
    }

    fun bindRenderbuffer(id: Int) {
        if (renderbuffer == id) return
        renderbuffer = id
        gl.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, id)
    }

    /** @param unit zero-based texture unit. */
    fun activeTexture(unit: Int) {
        if (activeUnit == unit) return
        runBeforeExternalChange()
        activeUnit = unit
        gl.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
    }

    fun bindTexture(target: Int, id: Int) {
        val targetIndex = textureTargetIndex(target)
        if (targetIndex < 0) {
            runBeforeExternalChange()
            gl.glBindTexture(target, id)
            RenderStats.textureBind()
            return
        }
        if (activeUnit == UNKNOWN) activeTexture(0)
        val unit = activeUnit
        if (unit >= unitCount) {
            runBeforeExternalChange()
            gl.glBindTexture(target, id)
            RenderStats.textureBind()
            return
        }
        val slot = unit * TEXTURE_TARGETS + targetIndex
        if (textures[slot] == id) return
        runBeforeExternalChange()
        textures[slot] = id
        gl.glBindTexture(target, id)
        RenderStats.textureBind()
    }

    /**
     * Records a texture binding made by someone else behind the cache's back, such as
     * `SurfaceTexture.updateTexImage()` binding its OES texture on the active unit, so the next
     * [bindTexture] is not wrongly skipped. Applies to the active unit; when that unit is unknown,
     * [target] is forgotten on every unit instead.
     */
    fun noteTextureBound(target: Int, id: Int) {
        val targetIndex = textureTargetIndex(target)
        if (targetIndex < 0) return
        val unit = activeUnit
        if (unit == UNKNOWN) {
            var slot = targetIndex
            while (slot < textures.size) {
                textures[slot] = UNKNOWN
                slot += TEXTURE_TARGETS
            }
            return
        }
        if (unit < unitCount) textures[unit * TEXTURE_TARGETS + targetIndex] = id
    }

    private fun textureTargetIndex(target: Int): Int = when (target) {
        GLES20.GL_TEXTURE_2D -> 0
        GLES30.GL_TEXTURE_2D_ARRAY -> 1
        TEXTURE_EXTERNAL_OES -> 2
        else -> -1
    }

    /** @param unit zero-based texture unit; 0 unbinds, so the texture's own parameters apply. */
    fun bindSampler(unit: Int, id: Int) {
        val tracked = unit in 0 until unitCount
        if (tracked && samplers[unit] == id) return
        runBeforeExternalChange()
        if (tracked) samplers[unit] = id
        gl.glBindSampler(unit, id)
    }

    /** Forgets [id] on every unit, since GL unbinds it when the sampler is deleted. */
    fun onSamplerDeleted(id: Int) {
        runBeforeExternalChange()
        for (i in samplers.indices) {
            if (samplers[i] == id) samplers[i] = UNKNOWN
        }
    }

    /** Forgets [id] as the current buffer so a reused id is bound again. */
    fun onBufferDeleted(id: Int) {
        runBeforeExternalChange()
        for (i in uniformBindings.indices) {
            if (uniformBindings[i] == id) uniformBindings[i] = UNKNOWN
        }
        if (arrayBuffer == id) arrayBuffer = UNKNOWN
        if (elementBuffer == id) elementBuffer = UNKNOWN
        if (uniformBuffer == id) uniformBuffer = UNKNOWN
        if (pixelUnpackBuffer == id) pixelUnpackBuffer = UNKNOWN
        if (pixelPackBuffer == id) pixelPackBuffer = UNKNOWN
        if (storageBuffer == id) storageBuffer = UNKNOWN
    }

    fun onProgramDeleted(id: Int) {
        runBeforeExternalChange()
        if (program == id) program = UNKNOWN
    }

    fun onFramebufferDeleted(id: Int) {
        runBeforeExternalChange()
        if (framebuffer == id) framebuffer = UNKNOWN
    }

    fun onVertexArrayDeleted(id: Int) {
        runBeforeExternalChange()
        if (vertexArray == id) {
            // GL falls back to VAO 0, so the VAO-scoped state is gone as well.
            vertexArray = UNKNOWN
            elementBuffer = UNKNOWN
            attribKnown = 0
            attribEnabled = 0
        }
    }

    fun onRenderbufferDeleted(id: Int) {
        if (renderbuffer == id) renderbuffer = UNKNOWN
    }

    /** Forgets [id] on every unit and target, since GL unbinds it when the texture is deleted. */
    fun onTextureDeleted(id: Int) {
        runBeforeExternalChange()
        for (i in textures.indices) {
            if (textures[i] == id) textures[i] = UNKNOWN
        }
    }

    fun setBlendEnabled(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        if (blendEnabled == value) return
        runBeforeExternalChange()
        blendEnabled = value
        setCap(GLES20.GL_BLEND, enabled)
    }

    fun blendFunc(src: Int, dst: Int) = blendFuncSeparate(src, dst, src, dst)

    fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {
        if (blendSrcRgb == srcRgb && blendDstRgb == dstRgb && blendSrcAlpha == srcAlpha && blendDstAlpha == dstAlpha) return
        runBeforeExternalChange()
        blendSrcRgb = srcRgb
        blendDstRgb = dstRgb
        blendSrcAlpha = srcAlpha
        blendDstAlpha = dstAlpha
        if (srcRgb == srcAlpha && dstRgb == dstAlpha) {
            gl.glBlendFunc(srcRgb, dstRgb)
        } else {
            gl.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
        }
        RenderStats.blendChange()
    }

    fun setDepthTestEnabled(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        if (depthTest == value) return
        runBeforeExternalChange()
        depthTest = value
        setCap(GLES20.GL_DEPTH_TEST, enabled)
    }

    fun depthFunc(func: Int) {
        if (depthFunc == func) return
        runBeforeExternalChange()
        depthFunc = func
        gl.glDepthFunc(func)
    }

    fun depthMask(flag: Boolean) {
        val value = if (flag) 1 else 0
        if (depthMask == value) return
        runBeforeExternalChange()
        depthMask = value
        gl.glDepthMask(flag)
    }

    fun clearDepth(depth: Float) {
        if (clearDepth == depth) return
        clearDepth = depth
        gl.glClearDepthf(depth)
    }

    fun setCullFaceEnabled(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        if (cullFace == value) return
        runBeforeExternalChange()
        cullFace = value
        setCap(GLES20.GL_CULL_FACE, enabled)
    }

    fun cullFace(mode: Int) {
        if (cullMode == mode) return
        runBeforeExternalChange()
        cullMode = mode
        gl.glCullFace(mode)
    }

    fun frontFace(mode: Int) {
        if (frontFace == mode) return
        runBeforeExternalChange()
        frontFace = mode
        gl.glFrontFace(mode)
    }

    fun setDitherEnabled(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        if (dither == value) return
        runBeforeExternalChange()
        dither = value
        setCap(GLES20.GL_DITHER, enabled)
    }

    fun lineWidth(width: Float) {
        if (lineWidth == width) return
        lineWidth = width
        gl.glLineWidth(width)
    }

    fun setScissorEnabled(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        if (scissor == value) return
        runBeforeScissorChange()
        scissor = value
        setCap(GLES20.GL_SCISSOR_TEST, enabled)
    }

    fun scissor(x: Int, y: Int, width: Int, height: Int) {
        if (scissorX == x && scissorY == y && scissorW == width && scissorH == height) return
        runBeforeScissorChange()
        scissorX = x
        scissorY = y
        scissorW = width
        scissorH = height
        gl.glScissor(x, y, width, height)
    }

    fun viewport(x: Int, y: Int, width: Int, height: Int) {
        if (viewportX == x && viewportY == y && viewportWidth == width && viewportHeight == height) return
        runBeforeExternalChange()
        viewportX = x
        viewportY = y
        viewportWidth = width
        viewportHeight = height
        gl.glViewport(x, y, width, height)
    }

    /** Attribute indices 0 until 32 are tracked; others pass through uncached. */
    fun enableVertexAttribArray(index: Int) {
        if (index !in 0 until 32) {
            runBeforeExternalChange()
            gl.glEnableVertexAttribArray(index)
            return
        }
        val bit = 1 shl index
        if (attribKnown and bit != 0 && attribEnabled and bit != 0) return
        runBeforeExternalChange()
        attribKnown = attribKnown or bit
        attribEnabled = attribEnabled or bit
        gl.glEnableVertexAttribArray(index)
    }

    fun disableVertexAttribArray(index: Int) {
        if (index !in 0 until 32) {
            runBeforeExternalChange()
            gl.glDisableVertexAttribArray(index)
            return
        }
        val bit = 1 shl index
        if (attribKnown and bit != 0 && attribEnabled and bit == 0) return
        runBeforeExternalChange()
        attribKnown = attribKnown or bit
        attribEnabled = attribEnabled and bit.inv()
        gl.glDisableVertexAttribArray(index)
    }

    // The next three are pass-through so the call stays in the backend.

    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        runBeforeExternalChange()
        gl.glVertexAttribPointer(index, size, type, normalized, stride, offset)
    }

    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, buffer: java.nio.Buffer) {
        runBeforeExternalChange()
        gl.glVertexAttribPointer(index, size, type, normalized, stride, buffer)
    }

    fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {
        runBeforeExternalChange()
        gl.glVertexAttrib4f(index, x, y, z, w)
    }

    fun clearColor(r: Float, g: Float, b: Float, a: Float) {
        if (clearR == r && clearG == g && clearB == b && clearA == a) return
        clearR = r
        clearG = g
        clearB = b
        clearA = a
        gl.glClearColor(r, g, b, a)
    }

    fun clear(mask: Int) {
        runBeforeExternalChange()
        gl.glClear(mask)
    }

    /**
     * Copies the state a draw of the render context may change (program, VAO with its element
     * buffer, array buffer, active unit, the 2D texture and the sampler of unit 0, the array
     * texture and the sampler of unit 1, blend, depth, cull and the scissor) into [out], which
     * needs [DRAW_STATE_SIZE] values. See [restoreDrawState].
     */
    fun saveDrawState(out: IntArray) {
        out[S_PROGRAM] = program
        out[S_VERTEX_ARRAY] = vertexArray
        out[S_ELEMENT_BUFFER] = elementBuffer
        out[S_ARRAY_BUFFER] = arrayBuffer
        out[S_ACTIVE_UNIT] = activeUnit
        out[S_TEXTURE_0] = textures[0]
        out[S_SAMPLER_0] = samplers[0]
        out[S_TEXTURE_ARRAY_1] = textures[TEXTURE_ARRAY_1]
        out[S_SAMPLER_1] = samplers[1]
        out[S_BLEND] = blendEnabled
        out[S_BLEND_SRC_RGB] = blendSrcRgb
        out[S_BLEND_DST_RGB] = blendDstRgb
        out[S_BLEND_SRC_ALPHA] = blendSrcAlpha
        out[S_BLEND_DST_ALPHA] = blendDstAlpha
        out[S_DEPTH_TEST] = depthTest
        out[S_DEPTH_FUNC] = depthFunc
        out[S_DEPTH_MASK] = depthMask
        out[S_CULL] = cullFace
        out[S_CULL_MODE] = cullMode
        saveScissorState(out, S_SCISSOR)
    }

    /**
     * Copies the scissor test and box into [out] from [offset] on ([SCISSOR_STATE_SIZE] values).
     * See [restoreScissorState].
     */
    fun saveScissorState(out: IntArray, offset: Int) {
        out[offset] = scissor
        out[offset + 1] = scissorX
        out[offset + 2] = scissorY
        out[offset + 3] = scissorW
        out[offset + 4] = scissorH
    }

    /** Sets the known values [saveScissorState] wrote into [saved] from [offset] on again. */
    fun restoreScissorState(saved: IntArray, offset: Int) {
        // The box is set as a whole, so either all four are known or none; a width is never -1.
        if (saved[offset + 3] != UNKNOWN) scissor(saved[offset + 1], saved[offset + 2], saved[offset + 3], saved[offset + 4])
        val enabled = saved[offset]
        if (enabled != UNKNOWN) setScissorEnabled(enabled == 1)
    }

    /**
     * Sets the values [saveDrawState] wrote into [saved] again, those that were known, so code
     * that set them earlier can still rely on them. Duplicates are skipped as usual.
     */
    fun restoreDrawState(saved: IntArray) {
        // The element buffer belongs to the VAO, so the VAO comes first.
        val vao = saved[S_VERTEX_ARRAY]
        if (vao != UNKNOWN) bindVertexArray(vao)
        val elements = saved[S_ELEMENT_BUFFER]
        if (elements != UNKNOWN && vertexArray == vao) bindElementBuffer(elements)
        val array = saved[S_ARRAY_BUFFER]
        if (array != UNKNOWN) bindArrayBuffer(array)
        val prog = saved[S_PROGRAM]
        if (prog != UNKNOWN) useProgram(prog)

        val blend = saved[S_BLEND]
        if (blend != UNKNOWN) setBlendEnabled(blend == 1)
        val srcRgb = saved[S_BLEND_SRC_RGB]
        val dstRgb = saved[S_BLEND_DST_RGB]
        val srcAlpha = saved[S_BLEND_SRC_ALPHA]
        val dstAlpha = saved[S_BLEND_DST_ALPHA]
        // All four are set together, so either all are known or none.
        if (srcRgb != UNKNOWN) blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
        val depth = saved[S_DEPTH_TEST]
        if (depth != UNKNOWN) setDepthTestEnabled(depth == 1)
        val func = saved[S_DEPTH_FUNC]
        if (func != UNKNOWN) depthFunc(func)
        val mask = saved[S_DEPTH_MASK]
        if (mask != UNKNOWN) depthMask(mask == 1)
        val cull = saved[S_CULL]
        if (cull != UNKNOWN) setCullFaceEnabled(cull == 1)
        val mode = saved[S_CULL_MODE]
        if (mode != UNKNOWN) cullFace(mode)
        restoreScissorState(saved, S_SCISSOR)

        // Unit 1 first, so unit 0 is active afterwards unless another unit was.
        val array1 = saved[S_TEXTURE_ARRAY_1]
        if (array1 != UNKNOWN && textures[TEXTURE_ARRAY_1] != array1) {
            activeTexture(1)
            bindTexture(GLES30.GL_TEXTURE_2D_ARRAY, array1)
        }
        val sampler1 = saved[S_SAMPLER_1]
        if (sampler1 != UNKNOWN) bindSampler(1, sampler1)

        val texture = saved[S_TEXTURE_0]
        val sampler = saved[S_SAMPLER_0]
        if (texture != UNKNOWN || sampler != UNKNOWN) {
            activeTexture(0)
            if (texture != UNKNOWN) bindTexture(GLES20.GL_TEXTURE_2D, texture)
            if (sampler != UNKNOWN) bindSampler(0, sampler)
        }
        val unit = saved[S_ACTIVE_UNIT]
        if (unit != UNKNOWN) activeTexture(unit)
    }

    private fun setCap(cap: Int, enabled: Boolean) {
        if (enabled) gl.glEnable(cap) else gl.glDisable(cap)
    }

    companion object {
        private const val UNKNOWN = -1
        private const val TEXTURE_TARGETS = 3
        private const val MIN_UNITS = 8
        private const val MAX_UNITS = 32
        private const val TEXTURE_EXTERNAL_OES = 0x8D65
        private const val UNIFORM_BINDINGS = 16

        private const val S_PROGRAM = 0
        private const val S_VERTEX_ARRAY = 1
        private const val S_ELEMENT_BUFFER = 2
        private const val S_ARRAY_BUFFER = 3
        private const val S_ACTIVE_UNIT = 4
        private const val S_TEXTURE_0 = 5
        private const val S_SAMPLER_0 = 6
        private const val S_BLEND = 7
        private const val S_BLEND_SRC_RGB = 8
        private const val S_BLEND_DST_RGB = 9
        private const val S_BLEND_SRC_ALPHA = 10
        private const val S_BLEND_DST_ALPHA = 11
        private const val S_DEPTH_TEST = 12
        private const val S_DEPTH_FUNC = 13
        private const val S_DEPTH_MASK = 14
        private const val S_CULL = 15
        private const val S_CULL_MODE = 16
        private const val S_TEXTURE_ARRAY_1 = 17
        private const val S_SAMPLER_1 = 18
        private const val S_SCISSOR = 19

        /** Index of unit 1's 2D array binding in the texture bindings. */
        private const val TEXTURE_ARRAY_1 = TEXTURE_TARGETS + 1

        /** Number of values [saveScissorState] writes. */
        const val SCISSOR_STATE_SIZE = 5

        /** Number of values [saveDrawState] writes. */
        const val DRAW_STATE_SIZE = S_SCISSOR + SCISSOR_STATE_SIZE
    }
}
