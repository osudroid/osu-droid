package org.andengine.graphics.gles

import android.opengl.GLES20
import org.andengine.graphics.BlendMode
import org.andengine.graphics.BufferTarget
import org.andengine.graphics.BufferUsage
import org.andengine.graphics.CullMode
import org.andengine.graphics.GpuBuffer
import org.andengine.graphics.GpuResource
import org.andengine.graphics.Pipeline
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.VertexAttribute
import org.andengine.graphics.VertexFormat
import org.andengine.graphics.VertexLayout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlesRenderContextTest {

    private lateinit var gl: RecordingGL
    private val device get() = GlesGraphicsDevice.instance
    private val context get() = GlesRenderContext.instance
    private val created = ArrayList<GpuResource>()

    private val layout = VertexLayout(
        20,
        listOf(
            VertexAttribute(0, VertexFormat.Float2, 0),
            VertexAttribute(1, VertexFormat.Float2, 8),
            VertexAttribute(2, VertexFormat.UByte4Normalized, 16)
        )
    )

    private fun newContext() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
    }

    private fun <T : GpuResource> track(r: T): T {
        created += r
        return r
    }

    private fun pipeline(
        blend: BlendMode? = BlendMode.Alpha,
        primitive: PrimitiveType = PrimitiveType.Triangles,
        layout: VertexLayout = this.layout
    ): Pipeline {
        val program = track(device.createProgram("void main() {}", "void main() {}", "p"))
        return Pipeline(program, layout, blend, null, false, CullMode.None, primitive)
    }

    private fun buffer(target: BufferTarget, label: String): GpuBuffer =
        track(device.createBuffer(target, BufferUsage.Static, 256, label))

    private fun count(prefix: String) = gl.calls.count { it.startsWith(prefix) }

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        newContext()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
    }

    @After
    fun tearDown() {
        if (context.isInPass) context.endPass()
        GlesDebugOptions.simulateEs30 = false
        for (r in created) if (!r.isDisposed) r.dispose()
    }

    @Test
    fun samePipelineTwice_noStateCalls() {
        val p = pipeline()
        val vb = buffer(BufferTarget.Vertex, "vb")
        gl.calls.clear()

        context.beginPass(null, null, false)
        context.setVertexBuffer(vb)
        context.setPipeline(p)
        context.draw(0, 3)
        context.setPipeline(p)
        context.draw(3, 3)

        assertEquals(gl.calls.toString(), 1, count("glUseProgram"))
        assertEquals(gl.calls.toString(), 1, count("glBindVertexArray"))
        assertEquals(2, count("glDrawArrays"))
        assertTrue(gl.calls.contains("glDrawArrays(${GLES20.GL_TRIANGLES},3,3)"))
    }

    @Test
    fun samePipelineNextPass_reappliesStateChangedOutside() {
        val p = pipeline(blend = BlendMode.Alpha)
        val vb = buffer(BufferTarget.Vertex, "vb")
        context.beginPass(null, null, false)
        context.setVertexBuffer(vb)
        context.setPipeline(p)
        context.draw(0, 3)
        context.endPass()

        // Legacy code between passes.
        GlesBackend.state.setBlendEnabled(false)
        GlesBackend.state.blendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)
        gl.calls.clear()

        context.beginPass(null, null, false)
        context.setPipeline(p)
        context.draw(0, 3)

        assertTrue(gl.calls.toString(), gl.calls.contains("glEnable(${GLES20.GL_BLEND})"))
        assertTrue(
            gl.calls.toString(),
            gl.calls.contains("glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})")
        )
        assertTrue(GlesBackend.state.isBlendEnabled)
    }

    @Test
    fun beginPassOnScreen_withoutSurfaceSize_throws() {
        GlesBackend.surfaceWidth = 0
        assertThrows(IllegalStateException::class.java) { context.beginPass(null, null, false) }
    }

    @Test
    fun vertexLayout_attributesReadOnly() {
        @Suppress("UNCHECKED_CAST")
        val list = layout.attributes as MutableList<VertexAttribute>
        assertThrows(UnsupportedOperationException::class.java) { list.add(VertexAttribute(3, VertexFormat.Float1, 0)) }
    }

    @Test
    fun blendNull_disablesBlend() {
        val vb = buffer(BufferTarget.Vertex, "vb")
        context.beginPass(null, null, false)
        context.setVertexBuffer(vb)
        context.setPipeline(pipeline(blend = BlendMode.Alpha))
        context.draw(0, 3)
        gl.calls.clear()

        context.setPipeline(pipeline(blend = null))
        context.draw(0, 3)

        assertTrue(gl.calls.toString(), gl.calls.contains("glDisable(${GLES20.GL_BLEND})"))
        assertEquals(0, count("glBlendFunc"))
    }

    @Test
    fun additive_setsOneOne() {
        val vb = buffer(BufferTarget.Vertex, "vb")
        gl.calls.clear()

        context.beginPass(null, null, false)
        context.setVertexBuffer(vb)
        context.setPipeline(pipeline(blend = BlendMode.Additive))
        context.draw(0, 3)

        assertTrue(gl.calls.toString(), gl.calls.contains("glEnable(${GLES20.GL_BLEND})"))
        assertTrue(
            gl.calls.toString(),
            gl.calls.contains("glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE},${GLES20.GL_ONE})")
        )
    }

    @Test
    fun vao_reusedForSameBuffers() {
        val vb = buffer(BufferTarget.Vertex, "vb")
        val ib = buffer(BufferTarget.Index, "ib")
        val p = pipeline()
        gl.calls.clear()

        context.beginPass(null, null, false)
        context.setPipeline(p)
        context.setVertexBuffer(vb)
        context.setIndexBuffer(ib)
        context.drawIndexed(0, 6)
        context.endPass()
        context.beginPass(null, null, false)
        context.setPipeline(pipeline(blend = BlendMode.Additive))
        context.setVertexBuffer(vb)
        context.setIndexBuffer(ib)
        context.drawIndexed(6, 6)

        assertEquals(gl.calls.toString(), 1, count("glGenVertexArrays"))
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},6,${GLES20.GL_UNSIGNED_SHORT},12)"))
    }

    @Test
    fun vao_setupBindsBuffersAndAttributes() {
        val vb = buffer(BufferTarget.Vertex, "vb") as GlesBuffer
        val ib = buffer(BufferTarget.Index, "ib") as GlesBuffer
        val p = pipeline()
        gl.calls.clear()

        context.beginPass(null, null, false)
        context.setPipeline(p)
        context.setVertexBuffer(vb)
        context.setIndexBuffer(ib)
        context.drawIndexed(0, 6)

        val calls = gl.calls
        assertTrue(calls.toString(), calls.contains("glVertexAttribPointer(0,2,${GLES20.GL_FLOAT},false,20,0)"))
        assertTrue(calls.contains("glVertexAttribPointer(1,2,${GLES20.GL_FLOAT},false,20,8)"))
        assertTrue(calls.contains("glVertexAttribPointer(2,4,${GLES20.GL_UNSIGNED_BYTE},true,20,16)"))
        assertTrue(calls.contains("glEnableVertexAttribArray(2)"))
        assertTrue(calls.contains("glBindBuffer(${GLES20.GL_ELEMENT_ARRAY_BUFFER},${ib.bufferId})"))
        // The array buffer must be bound before the pointers are set.
        val arrayBind = calls.indexOf("glBindBuffer(${GLES20.GL_ARRAY_BUFFER},${vb.bufferId})")
        val firstPointer = calls.indexOfFirst { it.startsWith("glVertexAttribPointer") }
        assertTrue(calls.toString(), arrayBind == -1 || arrayBind < firstPointer)
        assertEquals(vb.bufferId, GlesBackend.state.currentArrayBuffer)
    }

    @Test
    fun vao_differentBuffers_newVao() {
        val p = pipeline()
        val vb1 = buffer(BufferTarget.Vertex, "vb1")
        val vb2 = buffer(BufferTarget.Vertex, "vb2")
        gl.calls.clear()

        context.beginPass(null, null, false)
        context.setPipeline(p)
        context.setVertexBuffer(vb1)
        context.draw(0, 3)
        context.setVertexBuffer(vb2)
        context.draw(0, 3)
        context.setVertexBuffer(vb1)
        context.draw(0, 3)

        assertEquals(2, count("glGenVertexArrays"))
        assertEquals(2, context.vertexArrays.size)
    }

    @Test
    fun vao_rebuiltAfterContextLoss() {
        val p = pipeline()
        val vb = buffer(BufferTarget.Vertex, "vb")
        context.beginPass(null, null, false)
        context.setPipeline(p)
        context.setVertexBuffer(vb)
        context.draw(0, 3)
        context.endPass()

        // New context on the same fake: resources restore, VAOs are dropped.
        gl.calls.clear()
        GlesBackend.onContextCreatedForTest(gl)
        assertEquals(0, context.vertexArrays.size)
        gl.calls.clear()

        context.beginPass(null, null, false)
        context.draw(0, 3)

        assertEquals(gl.calls.toString(), 1, count("glGenVertexArrays"))
        // The pipeline is applied again on the new context.
        assertEquals(1, count("glUseProgram"))
    }

    @Test
    fun vao_droppedWhenBufferDisposed() {
        val p = pipeline()
        val vb1 = buffer(BufferTarget.Vertex, "vb1")
        val vb2 = buffer(BufferTarget.Vertex, "vb2")
        context.beginPass(null, null, false)
        context.setPipeline(p)
        context.setVertexBuffer(vb1)
        context.draw(0, 3)
        assertEquals(1, context.vertexArrays.size)

        vb1.dispose()
        gl.calls.clear()
        context.setVertexBuffer(vb2)
        context.draw(0, 3)

        assertEquals(1, context.vertexArrays.size)
        assertEquals(1, count("glDeleteVertexArrays"))
    }

    @Test
    fun drawWithDisposedVertexBuffer_throws() {
        val vb = buffer(BufferTarget.Vertex, "vb")
        context.beginPass(null, null, false)
        context.setPipeline(pipeline())
        context.setVertexBuffer(vb)
        vb.dispose()

        assertThrows(IllegalStateException::class.java) { context.draw(0, 3) }
    }

    @Test
    fun baseVertexOnEs30_throws() {
        GlesDebugOptions.simulateEs30 = true
        newContext()
        val vb = buffer(BufferTarget.Vertex, "vb")
        val ib = buffer(BufferTarget.Index, "ib")
        context.beginPass(null, null, false)
        context.setPipeline(pipeline())
        context.setVertexBuffer(vb)
        context.setIndexBuffer(ib)

        assertThrows(IllegalStateException::class.java) { context.drawIndexed(0, 6, baseVertex = 4) }
    }

    @Test
    fun baseVertexOnEs32_usesBaseVertexDraw() {
        val vb = buffer(BufferTarget.Vertex, "vb")
        val ib = buffer(BufferTarget.Index, "ib")
        context.beginPass(null, null, false)
        context.setPipeline(pipeline())
        context.setVertexBuffer(vb)
        context.setIndexBuffer(ib)
        gl.calls.clear()

        context.drawIndexed(3, 6, baseVertex = 4)

        assertTrue(
            gl.calls.toString(),
            gl.calls.contains("glDrawElementsInstancedBaseVertex(${GLES20.GL_TRIANGLES},6,${GLES20.GL_UNSIGNED_SHORT},6,1,4)")
        )
    }

    @Test
    fun beginPass_screen_bindsFramebuffer0AndSurfaceViewport() {
        val target = track(device.createRenderTarget(64, 32, false, "rt")) as GlesRenderTarget
        context.beginPass(target, null, false)
        assertEquals(target.framebufferId, GlesBackend.state.currentFramebuffer)
        assertTrue(gl.calls.contains("glViewport(0,0,64,32)"))
        context.endPass()
        gl.calls.clear()

        context.beginPass(null, floatArrayOf(0f, 0f, 0f, 1f), true)

        assertTrue(gl.calls.toString(), gl.calls.contains("glBindFramebuffer(${GLES20.GL_FRAMEBUFFER},0)"))
        assertTrue(gl.calls.contains("glViewport(0,0,800,480)"))
        assertTrue(gl.calls.contains("glDepthMask(true)"))
        assertTrue(gl.calls.contains("glClear(${GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT})"))
    }

    @Test
    fun beginPass_onTargetWhoseRestoreFailed_throws() {
        val target = track(device.createRenderTarget(64, 32, false, "rt")) as GlesRenderTarget
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        gl.framebufferStatus = GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT
        GlesBackend.onContextCreatedForTest(gl)
        assertEquals(0, target.framebufferId)

        val e = assertThrows(IllegalStateException::class.java) { context.beginPass(target, null, false) }

        assertTrue(e.message, e.message!!.contains("'rt'"))
        assertTrue(e.message, e.message!!.contains("restore"))
        assertTrue(!context.isInPass)
    }

    @Test
    fun setTexture_bindsTextureAndSamplerOnUnit() {
        val texture = track(device.createTexture(4, 4, org.andengine.graphics.TextureFormat.RGBA8, label = "t")) as GlesTexture
        val sampler = track(device.createSampler(org.andengine.graphics.TextureFilter.Nearest, org.andengine.graphics.TextureWrap.Repeat, "s")) as GlesSampler
        gl.calls.clear()

        context.setTexture(2, texture, sampler)
        context.setTexture(2, texture, sampler)
        context.setTexture(2, texture, null)

        assertTrue(gl.calls.contains("glActiveTexture(${GLES20.GL_TEXTURE0 + 2})"))
        assertEquals(gl.calls.toString(), 1, count("glBindTexture"))
        assertEquals(1, count("glBindSampler(2,${sampler.samplerId})"))
        assertEquals(1, count("glBindSampler(2,0)"))
    }

    @Test
    fun endPass_resetsActiveUnitAndUnbindsSamplersOfThePass() {
        val texture = track(device.createTexture(4, 4, org.andengine.graphics.TextureFormat.RGBA8, label = "t")) as GlesTexture
        val sampler = track(device.createSampler(org.andengine.graphics.TextureFilter.Nearest, org.andengine.graphics.TextureWrap.Repeat, "s")) as GlesSampler
        context.beginPass(null, null, false)
        context.setTexture(2, texture, sampler)
        gl.calls.clear()

        context.endPass()

        assertTrue(gl.calls.toString(), gl.calls.contains("glActiveTexture(${GLES20.GL_TEXTURE0})"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindSampler(2,0)"))
        assertEquals(0, GlesBackend.state.currentActiveTextureUnit)
    }

    @Test
    fun endPass_withoutTextures_noSamplerCalls() {
        context.beginPass(null, null, false)
        context.endPass()
        context.beginPass(null, null, false)
        gl.calls.clear()

        context.endPass()

        assertEquals(gl.calls.toString(), 0, count("glBindSampler"))
    }

    @Test
    fun setUniformBlock_bindsBufferBase() {
        val ub = buffer(BufferTarget.Uniform, "ub") as GlesBuffer
        gl.calls.clear()

        context.setUniformBlock(1, ub)
        context.setUniformBlock(1, ub)

        assertEquals(1, count("glBindBufferBase(${android.opengl.GLES30.GL_UNIFORM_BUFFER},1,${ub.bufferId})"))
    }

    @Test
    fun scissor_enablesAndClears() {
        context.setScissor(1, 2, 3, 4)
        context.clearScissor()
        assertTrue(gl.calls.contains("glEnable(${GLES20.GL_SCISSOR_TEST})"))
        assertTrue(gl.calls.contains("glScissor(1,2,3,4)"))
        assertTrue(gl.calls.contains("glDisable(${GLES20.GL_SCISSOR_TEST})"))
    }

    @Test
    fun primitiveType_mapsToGlMode() {
        val vb = buffer(BufferTarget.Vertex, "vb")
        context.beginPass(null, null, false)
        context.setVertexBuffer(vb)
        context.setPipeline(pipeline(primitive = PrimitiveType.LineStrip))
        context.draw(0, 4)
        assertTrue(gl.calls.contains("glDrawArrays(${GLES20.GL_LINE_STRIP},0,4)"))
    }

    @Test
    fun beginInlinePass_keepsFramebufferViewportAndScissor() {
        val vb = buffer(BufferTarget.Vertex, "vb")
        val p = pipeline()
        // Legacy code drawing into its own framebuffer with a clip.
        GlesBackend.state.bindFramebuffer(7)
        GlesBackend.state.viewport(0, 0, 128, 64)
        GlesBackend.state.setScissorEnabled(true)
        GlesBackend.state.scissor(1, 2, 3, 4)
        gl.calls.clear()

        context.beginInlinePass()
        context.setVertexBuffer(vb)
        context.setPipeline(p)
        context.draw(0, 3)
        context.endPass()

        assertEquals(gl.calls.toString(), 0, count("glBindFramebuffer"))
        assertEquals(gl.calls.toString(), 0, count("glViewport"))
        assertEquals(gl.calls.toString(), 0, count("glDisable(${GLES20.GL_SCISSOR_TEST})"))
        assertEquals(gl.calls.toString(), 0, count("glClear"))
        assertEquals(1, count("glDrawArrays"))
        assertEquals(7, GlesBackend.state.currentFramebuffer)
        assertTrue(!context.isInPass)
    }

    @Test
    fun beginInlinePass_reappliesPipelineChangedOutside() {
        val vb = buffer(BufferTarget.Vertex, "vb")
        val p = pipeline(blend = BlendMode.Alpha)
        context.beginInlinePass()
        context.setVertexBuffer(vb)
        context.setPipeline(p)
        context.draw(0, 3)
        context.endPass()
        GlesBackend.state.setBlendEnabled(false)
        gl.calls.clear()

        context.beginInlinePass()
        context.setPipeline(p)
        context.draw(0, 3)

        assertTrue(gl.calls.toString(), gl.calls.contains("glEnable(${GLES20.GL_BLEND})"))
    }

    @Test
    fun beginInlinePass_whileInPass_throws() {
        context.beginInlinePass()
        assertThrows(IllegalStateException::class.java) { context.beginInlinePass() }
        assertThrows(IllegalStateException::class.java) { context.beginPass(null, null, false) }
    }

    @Test
    fun setUniformMatrix4_usesPipelineProgram() {
        val p = pipeline()
        val program = p.program as GlesProgram
        val m = FloatArray(16) { it.toFloat() }
        context.setPipeline(p)
        gl.calls.clear()

        context.setUniformMatrix4(3, m)

        assertTrue(gl.calls.toString(), gl.calls.contains("glUseProgram(${program.programId})"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glUniformMatrix4fv(3,1,false,buf,0)"))
        assertTrue(m.contentEquals(gl.uniformMatrices.last()))
    }

    @Test
    fun setUniformMatrix4_negativeLocation_ignored() {
        context.setPipeline(pipeline())
        gl.calls.clear()

        context.setUniformMatrix4(-1, FloatArray(16))

        assertEquals(gl.calls.toString(), 0, count("glUniformMatrix4fv"))
    }

    @Test
    fun setUniformMatrix4_shortMatrix_throws() {
        context.setPipeline(pipeline())
        assertThrows(IllegalArgumentException::class.java) { context.setUniformMatrix4(0, FloatArray(4)) }
    }

    @Test
    fun vertexLayout_equalsByContent() {
        val other = VertexLayout(
            20,
            listOf(
                VertexAttribute(0, VertexFormat.Float2, 0),
                VertexAttribute(1, VertexFormat.Float2, 8),
                VertexAttribute(2, VertexFormat.UByte4Normalized, 16)
            )
        )
        assertEquals(layout, other)
        assertEquals(layout.hashCode(), other.hashCode())
        assertNotEquals(layout, VertexLayout(20, listOf(VertexAttribute(0, VertexFormat.Float3, 0))))
    }
}
