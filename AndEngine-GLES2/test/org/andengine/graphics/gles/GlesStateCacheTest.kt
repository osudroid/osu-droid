package org.andengine.graphics.gles

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import org.andengine.graphics.ClearBuffers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GlesStateCacheTest {

    private lateinit var gl: RecordingGL
    private lateinit var cache: GlesStateCache

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        cache = GlesStateCache(gl)
        gl.calls.clear()
    }

    private fun count(call: String) = gl.calls.count { it == call }

    @Test
    fun bindTexture_sameIdTwice_oneCall() {
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 5)
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 5)
        assertEquals(1, count("glBindTexture(${GLES20.GL_TEXTURE_2D},5)"))
    }

    @Test
    fun bindTexture_targetsTrackedSeparately() {
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 5)
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 5)
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 5)
        assertEquals(1, count("glBindTexture(${GLES20.GL_TEXTURE_2D},5)"))
        assertEquals(1, count("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},5)"))
        assertEquals(2, gl.calls.count { it.startsWith("glBindTexture") })
    }

    @Test
    fun bindTexture_perUnit() {
        cache.activeTexture(0)
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 5)
        cache.activeTexture(1)
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 5)
        assertEquals(2, count("glBindTexture(${GLES20.GL_TEXTURE_2D},5)"))
        assertEquals(2, gl.calls.count { it.startsWith("glActiveTexture") })
    }

    @Test
    fun externalRebind_forcesCorrectNextBind() {
        cache.activeTexture(0)
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 7)
        cache.noteTextureBound(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 8)
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 7)
        assertEquals(2, count("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},7)"))
    }

    @Test
    fun externalRebind_sameIdAsNoted_skipsBind() {
        cache.activeTexture(0)
        cache.noteTextureBound(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 8)
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 8)
        assertEquals(0, count("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},8)"))
    }

    @Test
    fun externalRebind_onlyAffectsActiveUnit() {
        cache.activeTexture(1)
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 7)
        cache.activeTexture(0)
        cache.noteTextureBound(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 8)
        cache.activeTexture(1)
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 7)
        assertEquals(1, count("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},7)"))
    }

    @Test
    fun externalRebind_unknownUnit_nextBindReachesGl() {
        cache.invalidate()
        cache.noteTextureBound(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 7)
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 7)
        assertEquals(1, count("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},7)"))
    }

    @Test
    fun invalidate_forcesNextCall() {
        cache.useProgram(3)
        cache.invalidate()
        cache.useProgram(3)
        assertEquals(2, count("glUseProgram(3)"))
    }

    @Test
    fun blendFunc_equalsSeparate() {
        cache.blendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        cache.blendFuncSeparate(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        assertEquals(1, gl.calls.count { it.startsWith("glBlendFunc") })
    }

    @Test
    fun bindVertexArray_forgetsElementBuffer() {
        cache.bindElementBuffer(4)
        cache.bindVertexArray(2)
        cache.bindElementBuffer(4)
        assertEquals(2, count("glBindBuffer(${GLES20.GL_ELEMENT_ARRAY_BUFFER},4)"))
    }

    @Test
    fun vertexAttribArray_mask() {
        cache.enableVertexAttribArray(0)
        cache.enableVertexAttribArray(0)
        cache.disableVertexAttribArray(0)
        cache.disableVertexAttribArray(0)
        assertEquals(1, count("glEnableVertexAttribArray(0)"))
        assertEquals(1, count("glDisableVertexAttribArray(0)"))
    }

    @Test
    fun onTextureDeleted_forgetsBinding() {
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 7)
        cache.onTextureDeleted(7)
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 7)
        assertEquals(2, count("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
    }

    @Test
    fun enableFlags_dedupAndGetters() {
        cache.setBlendEnabled(true)
        cache.setBlendEnabled(true)
        assertEquals(1, count("glEnable(${GLES20.GL_BLEND})"))
        assertTrue(cache.isBlendEnabled)
        cache.setBlendEnabled(false)
        assertEquals(1, count("glDisable(${GLES20.GL_BLEND})"))
    }

    @Test
    fun bindFramebuffer_dedup() {
        cache.bindFramebuffer(2)
        cache.bindFramebuffer(2)
        assertEquals(1, count("glBindFramebuffer(${GLES30.GL_FRAMEBUFFER},2)"))
    }

    @Test
    fun bindVertexArray_forgetsAttribEnables() {
        cache.enableVertexAttribArray(0)
        cache.bindVertexArray(2)
        cache.enableVertexAttribArray(0)
        assertEquals(2, count("glEnableVertexAttribArray(0)"))
    }

    @Test
    fun onBufferDeleted_forgetsBinding() {
        cache.bindArrayBuffer(3)
        cache.onBufferDeleted(3)
        cache.bindArrayBuffer(3)
        assertEquals(2, count("glBindBuffer(${GLES20.GL_ARRAY_BUFFER},3)"))
    }

    @Test
    fun onProgramDeleted_forgetsBinding() {
        cache.useProgram(3)
        cache.onProgramDeleted(3)
        cache.useProgram(3)
        assertEquals(2, count("glUseProgram(3)"))
    }

    @Test
    fun onFramebufferDeleted_forgetsBinding() {
        cache.bindFramebuffer(3)
        cache.onFramebufferDeleted(3)
        cache.bindFramebuffer(3)
        assertEquals(2, gl.calls.count { it.startsWith("glBindFramebuffer") })
    }

    @Test
    fun onVertexArrayDeleted_forgetsBinding() {
        cache.bindVertexArray(3)
        cache.onVertexArrayDeleted(3)
        cache.bindVertexArray(3)
        assertEquals(2, count("glBindVertexArray(3)"))
    }

    @Test
    fun vertexAttribArray_outOfRange_passesThrough() {
        cache.enableVertexAttribArray(40)
        cache.enableVertexAttribArray(40)
        assertEquals(2, count("glEnableVertexAttribArray(40)"))
    }

    @Test
    fun onVertexArrayDeleted_forgetsVaoScopedState() {
        cache.bindVertexArray(3)
        cache.bindElementBuffer(4)
        cache.onVertexArrayDeleted(3)
        cache.bindElementBuffer(4)
        assertEquals(2, count("glBindBuffer(${GLES20.GL_ELEMENT_ARRAY_BUFFER},4)"))
    }

    @Test
    fun onRenderbufferDeleted_forgetsBinding() {
        cache.bindRenderbuffer(3)
        cache.onRenderbufferDeleted(3)
        cache.bindRenderbuffer(3)
        assertEquals(2, gl.calls.count { it.startsWith("glBindRenderbuffer") })
    }

    @Test
    fun bindSampler_cachedPerUnit_forgottenOnDelete() {
        cache.bindSampler(1, 4)
        cache.bindSampler(1, 4)
        cache.bindSampler(2, 4)
        assertEquals(2, gl.calls.count { it.startsWith("glBindSampler") })
        cache.onSamplerDeleted(4)
        cache.bindSampler(1, 4)
        assertEquals(3, gl.calls.count { it.startsWith("glBindSampler") })
    }

    @Test
    fun bindUniformBufferBase_cachedPerIndex_forgottenOnBufferDelete() {
        cache.bindUniformBufferBase(0, 6)
        cache.bindUniformBufferBase(0, 6)
        cache.bindUniformBuffer(6)
        assertEquals(1, gl.calls.count { it.startsWith("glBindBufferBase") })
        // glBindBufferBase also set the generic binding.
        assertEquals(0, gl.calls.count { it.startsWith("glBindBuffer(") })
        cache.onBufferDeleted(6)
        cache.bindUniformBufferBase(0, 6)
        assertEquals(2, gl.calls.count { it.startsWith("glBindBufferBase") })
    }

    @Test
    fun beforeExternalChange_runsOnceBeforeARealChange() {
        cache.useProgram(3)
        gl.calls.clear()
        val seen = ArrayList<String>()
        cache.beforeExternalChange = Runnable { seen += "hook after ${gl.calls.size} calls" }

        cache.useProgram(3)
        assertEquals("a skipped duplicate does not count", 0, seen.size)

        cache.useProgram(4)
        cache.useProgram(5)
        assertEquals(listOf("hook after 0 calls"), seen)
        assertNull(cache.beforeExternalChange)
    }

    @Test
    fun beforeExternalChange_runsBeforeDeleteAndClear() {
        var runs = 0
        val hook = Runnable { runs++ }
        cache.beforeExternalChange = hook
        cache.onTextureDeleted(9)
        cache.beforeExternalChange = hook
        cache.clear(GLES20.GL_COLOR_BUFFER_BIT)
        assertEquals(2, runs)
    }

    @Test
    fun beforeExternalChange_isNotReentered() {
        var runs = 0
        cache.beforeExternalChange = Runnable {
            runs++
            cache.useProgram(8)
        }
        cache.useProgram(7)
        assertEquals(1, runs)
        assertEquals(listOf("glUseProgram(8)", "glUseProgram(7)"), gl.calls)
    }

    @Test
    fun drawState_restoresKnownValues() {
        cache.useProgram(3)
        cache.bindArrayBuffer(4)
        cache.activeTexture(2)
        cache.activeTexture(0)
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 5)
        cache.activeTexture(2)
        cache.setBlendEnabled(true)
        cache.blendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)
        val saved = IntArray(GlesStateCache.DRAW_STATE_SIZE)
        cache.saveDrawState(saved)

        cache.useProgram(30)
        cache.bindArrayBuffer(40)
        cache.activeTexture(0)
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 50)
        cache.setBlendEnabled(false)
        cache.blendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        gl.calls.clear()

        cache.restoreDrawState(saved)

        assertEquals(3, cache.currentProgram)
        assertEquals(4, cache.currentArrayBuffer)
        assertEquals(2, cache.currentActiveTextureUnit)
        assertTrue(cache.isBlendEnabled)
        assertEquals(GLES20.GL_ONE, cache.currentBlendSrcRgb)
        assertEquals(GLES20.GL_ZERO, cache.currentBlendDstRgb)
        assertEquals(1, count("glBindTexture(${GLES20.GL_TEXTURE_2D},5)"))
        // Unknown values (VAO, depth, cull, sampler) are left alone.
        assertEquals(0, gl.calls.count { it.startsWith("glBindVertexArray") || it.startsWith("glBindSampler") })
    }

    @Test
    fun drawState_restoresKnownExternalTextureOfUnit0() {
        cache.activeTexture(0)
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 7)
        val saved = IntArray(GlesStateCache.DRAW_STATE_SIZE)
        cache.saveDrawState(saved)

        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 9)
        cache.activeTexture(3)
        gl.calls.clear()

        cache.restoreDrawState(saved)

        assertEquals(gl.calls.toString(), 1, count("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},7)"))
        // The external binding is set on unit 0, which stays active as it was saved.
        val bind = gl.calls.indexOf("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},7)")
        val unit0 = gl.calls.indexOf("glActiveTexture(${GLES20.GL_TEXTURE0})")
        assertTrue(gl.calls.toString(), unit0 in 0 until bind)
        assertEquals(0, cache.currentActiveTextureUnit)

        // Restoring again with the binding already in place changes nothing.
        gl.calls.clear()
        cache.restoreDrawState(saved)
        assertEquals(gl.calls.toString(), 0, gl.calls.count { it.startsWith("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},") })
    }

    @Test
    fun drawState_leavesUnknownExternalTextureOfUnit0() {
        // Unit 0's external binding was never set through the cache: unknown when saved.
        cache.activeTexture(0)
        cache.bindTexture(GLES20.GL_TEXTURE_2D, 5)
        val saved = IntArray(GlesStateCache.DRAW_STATE_SIZE)
        cache.saveDrawState(saved)

        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 9)
        gl.calls.clear()

        cache.restoreDrawState(saved)

        // The unknown binding is not guessed: the external texture the draw bound stays.
        assertEquals(gl.calls.toString(), 0, gl.calls.count { it.startsWith("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},") })
        cache.bindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 9)
        assertEquals(gl.calls.toString(), 0, gl.calls.count { it.startsWith("glBindTexture(${GLES11Ext.GL_TEXTURE_EXTERNAL_OES},") })
    }

    @Test
    fun clearBuffers_turnsTheDepthMaskOnFirst() {
        cache.depthMask(false)
        gl.calls.clear()

        cache.clear(ClearBuffers.All)

        val bits = GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_STENCIL_BUFFER_BIT
        assertEquals(listOf("glDepthMask(true)", "glClear($bits)"), gl.calls)
    }

    @Test
    fun clearBuffers_colorOnlyKeepsTheDepthMask() {
        cache.depthMask(false)
        gl.calls.clear()

        cache.clear(ClearBuffers.Color)

        assertEquals(listOf("glClear(${GLES20.GL_COLOR_BUFFER_BIT})"), gl.calls)
    }

    @Test
    fun clearBuffers_noneClearsNothing() {
        cache.clear(ClearBuffers.None)

        assertTrue(gl.calls.isEmpty())
    }
}
