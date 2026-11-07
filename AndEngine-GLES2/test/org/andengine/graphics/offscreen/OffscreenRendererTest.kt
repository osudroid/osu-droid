package org.andengine.graphics.offscreen

import org.andengine.engine.camera.Camera
import org.andengine.graphics.DrawContext
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesRenderTarget
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.endOpenPass
import org.andengine.graphics.gles.installRecordingGL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OffscreenRendererTest {

    private lateinit var gl: RecordingGL
    private val drawContext = DrawContext()
    private val camera = Camera(0f, 0f, 800f, 480f)

    @Before
    fun setUp() {
        gl = installRecordingGL()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        GlesBackend.state.bindFramebuffer(0)
        GlesBackend.state.viewport(0, 0, 800, 480)
        drawContext.resetModelViewGLMatrixStack()
        drawContext.resetProjectionGLMatrixStack()
    }

    @After
    fun tearDown() {
        endOpenPass()
        RenderTargetPool.clear()
    }

    @Test
    fun pool_roundsSizesUpAndReusesReturnedTargets() {
        val lease = RenderTargetPool.acquire(300, 100)
        assertEquals(2 * RenderTargetPool.SIZE_STEP, lease.target.width)
        assertEquals(RenderTargetPool.SIZE_STEP, lease.target.height)

        RenderTargetPool.release(lease)
        assertFalse(lease.isValid)

        val again = RenderTargetPool.acquire(200, 200)
        assertSame(lease.target, again.target)
        assertTrue(again.isValid)

        // Too small for this one.
        val larger = RenderTargetPool.acquire(600, 100)
        assertNotSame(lease.target, larger.target)
    }

    @Test
    fun pool_takesBackLeasesUnusedForTooLong() {
        val used = RenderTargetPool.acquire(10, 10)
        val unused = RenderTargetPool.acquire(10, 10)

        repeat(RenderTargetPool.UNUSED_FRAMES + 1) {
            RenderTargetPool.onFrame()
            used.markUsed()
        }

        assertTrue(used.isValid)
        assertFalse(unused.isValid)
        // The taken back target is lent again.
        assertSame(unused.target, RenderTargetPool.acquire(10, 10).target)
    }

    @Test
    fun runPending_runsARequestedJobOnce() {
        var runs = 0
        val job = OffscreenRenderer.Job { _, _ -> runs++ }

        OffscreenRenderer.request(job)
        OffscreenRenderer.request(job)
        OffscreenRenderer.runPending(drawContext, camera)
        OffscreenRenderer.runPending(drawContext, camera)

        assertEquals(1, runs)
    }

    @Test
    fun draw_rendersIntoTheTargetArea() {
        val target = RenderTargetPool.acquire(64, 32).target as GlesRenderTarget
        var inTarget = false

        OffscreenRenderer.request { dc, _ ->
            OffscreenRenderer.draw(target, 50, 20, 0f, 0f, 50f, 20f, dc) {
                inTarget = GlesBackend.state.currentFramebuffer == target.framebufferId && GlesRenderContext.instance.isInPass
            }
        }
        OffscreenRenderer.runPending(drawContext, camera)

        assertTrue(inTarget)
        assertTrue(gl.calls.toString(), gl.calls.contains("glViewport(0,0,50,20)"))
        assertFalse(GlesRenderContext.instance.isInPass)
    }

    @Test
    fun runPending_restoresFramebufferAndViewport() {
        val target = RenderTargetPool.acquire(64, 32).target

        OffscreenRenderer.request { dc, _ -> OffscreenRenderer.draw(target, 50, 20, 0f, 0f, 50f, 20f, dc) {} }
        OffscreenRenderer.runPending(drawContext, camera)

        assertEquals(0, GlesBackend.state.currentFramebuffer)
        assertEquals(800, GlesBackend.state.viewportWidth)
        assertEquals(480, GlesBackend.state.viewportHeight)
    }

    @Test
    fun runPending_keepsTheJobsAfterOneThatThrew() {
        var ran = false
        OffscreenRenderer.request { _, _ -> throw IllegalStateException("boom") }
        OffscreenRenderer.request { _, _ -> ran = true }

        assertThrows(IllegalStateException::class.java) { OffscreenRenderer.runPending(drawContext, camera) }
        assertFalse(ran)

        OffscreenRenderer.runPending(drawContext, camera)
        assertTrue(ran)
    }

    @Test
    fun cancel_dropsARequestedJob() {
        var ran = false
        val job = OffscreenRenderer.Job { _, _ -> ran = true }

        OffscreenRenderer.request(job)
        OffscreenRenderer.cancel(job)
        OffscreenRenderer.runPending(drawContext, camera)

        assertFalse(ran)
    }
}
