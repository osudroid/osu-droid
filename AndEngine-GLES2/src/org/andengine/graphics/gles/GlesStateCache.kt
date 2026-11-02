package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import org.andengine.graphics.debug.RenderStats

/**
 * The single owner of GL state. Every setter forwards to [gl] only when the value really changes
 * and counts the matching [RenderStats] entry. Unknown state is `-1` (or `NaN` for floats), so
 * after [invalidate] the next call always reaches GL. GL thread only.
 */
class GlesStateCache(private val gl: GL30) {

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
        program = id
        gl.glUseProgram(id)
        RenderStats.programSwitch()
    }

    fun bindArrayBuffer(id: Int) {
        if (arrayBuffer == id) return
        arrayBuffer = id
        gl.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        RenderStats.bufferBind()
    }

    fun bindElementBuffer(id: Int) {
        if (elementBuffer == id) return
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
        activeUnit = unit
        gl.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
    }

    fun bindTexture(target: Int, id: Int) {
        val targetIndex = textureTargetIndex(target)
        if (targetIndex < 0) {
            gl.glBindTexture(target, id)
            RenderStats.textureBind()
            return
        }
        if (activeUnit == UNKNOWN) activeTexture(0)
        val unit = activeUnit
        if (unit >= unitCount) {
            gl.glBindTexture(target, id)
            RenderStats.textureBind()
            return
        }
        val slot = unit * TEXTURE_TARGETS + targetIndex
        if (textures[slot] == id) return
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
        if (unit in 0 until unitCount) {
            if (samplers[unit] == id) return
            samplers[unit] = id
        }
        gl.glBindSampler(unit, id)
    }

    /** Forgets [id] on every unit, since GL unbinds it when the sampler is deleted. */
    fun onSamplerDeleted(id: Int) {
        for (i in samplers.indices) {
            if (samplers[i] == id) samplers[i] = UNKNOWN
        }
    }

    /** Forgets [id] as the current buffer so a reused id is bound again. */
    fun onBufferDeleted(id: Int) {
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
        if (program == id) program = UNKNOWN
    }

    fun onFramebufferDeleted(id: Int) {
        if (framebuffer == id) framebuffer = UNKNOWN
    }

    fun onVertexArrayDeleted(id: Int) {
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
        for (i in textures.indices) {
            if (textures[i] == id) textures[i] = UNKNOWN
        }
    }

    fun setBlendEnabled(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        if (blendEnabled == value) return
        blendEnabled = value
        setCap(GLES20.GL_BLEND, enabled)
    }

    fun blendFunc(src: Int, dst: Int) = blendFuncSeparate(src, dst, src, dst)

    fun blendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {
        if (blendSrcRgb == srcRgb && blendDstRgb == dstRgb && blendSrcAlpha == srcAlpha && blendDstAlpha == dstAlpha) return
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
        depthTest = value
        setCap(GLES20.GL_DEPTH_TEST, enabled)
    }

    fun depthFunc(func: Int) {
        if (depthFunc == func) return
        depthFunc = func
        gl.glDepthFunc(func)
    }

    fun depthMask(flag: Boolean) {
        val value = if (flag) 1 else 0
        if (depthMask == value) return
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
        cullFace = value
        setCap(GLES20.GL_CULL_FACE, enabled)
    }

    fun cullFace(mode: Int) {
        if (cullMode == mode) return
        cullMode = mode
        gl.glCullFace(mode)
    }

    fun frontFace(mode: Int) {
        if (frontFace == mode) return
        frontFace = mode
        gl.glFrontFace(mode)
    }

    fun setDitherEnabled(enabled: Boolean) {
        val value = if (enabled) 1 else 0
        if (dither == value) return
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
        scissor = value
        setCap(GLES20.GL_SCISSOR_TEST, enabled)
    }

    fun scissor(x: Int, y: Int, width: Int, height: Int) {
        if (scissorX == x && scissorY == y && scissorW == width && scissorH == height) return
        scissorX = x
        scissorY = y
        scissorW = width
        scissorH = height
        gl.glScissor(x, y, width, height)
    }

    fun viewport(x: Int, y: Int, width: Int, height: Int) {
        if (viewportX == x && viewportY == y && viewportWidth == width && viewportHeight == height) return
        viewportX = x
        viewportY = y
        viewportWidth = width
        viewportHeight = height
        gl.glViewport(x, y, width, height)
    }

    /** Attribute indices 0 until 32 are tracked; others pass through uncached. */
    fun enableVertexAttribArray(index: Int) {
        if (index !in 0 until 32) {
            gl.glEnableVertexAttribArray(index)
            return
        }
        val bit = 1 shl index
        if (attribKnown and bit != 0 && attribEnabled and bit != 0) return
        attribKnown = attribKnown or bit
        attribEnabled = attribEnabled or bit
        gl.glEnableVertexAttribArray(index)
    }

    fun disableVertexAttribArray(index: Int) {
        if (index !in 0 until 32) {
            gl.glDisableVertexAttribArray(index)
            return
        }
        val bit = 1 shl index
        if (attribKnown and bit != 0 && attribEnabled and bit == 0) return
        attribKnown = attribKnown or bit
        attribEnabled = attribEnabled and bit.inv()
        gl.glDisableVertexAttribArray(index)
    }

    // The next three are pass-through so the call stays in the backend.

    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        gl.glVertexAttribPointer(index, size, type, normalized, stride, offset)
    }

    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, buffer: java.nio.Buffer) {
        gl.glVertexAttribPointer(index, size, type, normalized, stride, buffer)
    }

    fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float) {
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
        gl.glClear(mask)
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
    }
}
