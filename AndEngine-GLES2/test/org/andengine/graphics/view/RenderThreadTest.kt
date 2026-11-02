package org.andengine.graphics.view

import android.graphics.SurfaceTexture
import android.view.Surface
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.andengine.opengl.exception.GLESVersionUnsupportedException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RenderThreadTest {

    private val log = CallLog()
    private val backend = FakeSurfaceBackend(log)
    private val renderer = RecordingFrameRenderer(log)
    private val pacing = FramePacing()
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var window: Surface
    private lateinit var thread: RenderThread

    @Before
    fun setUp() {
        surfaceTexture = SurfaceTexture(0)
        window = Surface(surfaceTexture)
        thread = RenderThread(backend, renderer, pacing) { throw AssertionError("vsync pacing is off") }
    }

    @After
    fun tearDown() {
        thread.requestExitAndWait()
        window.release()
        surfaceTexture.release()
    }

    private fun showWindow(width: Int = 800, height: Int = 480) {
        thread.surfaceCreated(window)
        thread.surfaceChanged(width, height)
    }

    private fun runFrame(): List<String> {
        log.take()
        thread.runOneIterationForTest()
        return log.take()
    }

    @Test
    fun threadName_differsFromPlatformRenderThread() {
        assertEquals("EngineRender", thread.name)
    }

    @Test
    fun firstFrame_createsContextAndSurface_thenDraws() {
        showWindow()

        assertTrue(thread.runOneIterationForTest())

        assertEquals(
            listOf(
                "createContext", "createSurface", "makeCurrent", "onContextCreated", "onSurfaceChanged(800,480)",
                "setSwapInterval(1)", "onDrawFrame", "present", "onFrameSwapped"
            ),
            log.take()
        )
    }

    @Test
    fun secondFrame_onlyDrawsAndPresents() {
        showWindow()
        runFrame()

        assertEquals(listOf("makeCurrent", "onDrawFrame", "present", "onFrameSwapped"), runFrame())
    }

    @Test
    fun pauseResume_keepsContext_reportsSizeAgain() {
        showWindow()
        runFrame()

        thread.onPause()
        assertFalse(thread.runOneIterationForTest())
        val paused = log.take()
        assertEquals(listOf("destroySurface"), paused)

        thread.onResume()
        val resumed = runFrame()

        assertFalse("createContext" in resumed)
        assertFalse("onContextCreated" in resumed)
        assertTrue("createSurface" in resumed)
        assertTrue("onSurfaceChanged(800,480)" in resumed)
        assertTrue("setSwapInterval(1)" in resumed)
        assertTrue("onFrameSwapped" in resumed)
    }

    @Test
    fun surfaceDestroyed_waitsUntilSurfaceReleased() {
        backend.presentDelayMillis = 1
        backend.destroySurfaceDelayMillis = 100
        val firstFrame = CountDownLatch(1)
        renderer.onSwapped = Runnable { firstFrame.countDown() }
        showWindow()
        thread.start()
        assertTrue(firstFrame.await(2, TimeUnit.SECONDS))

        val start = System.nanoTime()
        thread.surfaceDestroyed()
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000L

        assertTrue("destroySurface" in log.snapshot())
        assertFalse(backend.hasSurface)
        assertTrue(elapsedMillis < 2000)
    }

    @Test
    fun contextLostOnPresent_recreatesOnce() {
        showWindow()
        runFrame()
        backend.presentResults.add(PresentResult.CONTEXT_LOST)

        val lost = runFrame()
        assertFalse("onFrameSwapped" in lost)
        assertEquals(listOf("makeCurrent", "onDrawFrame", "present", "destroySurface", "destroyContext"), lost)

        val recovered = runFrame()
        assertEquals(1, recovered.count { it == "createContext" })
        assertEquals(1, recovered.count { it == "onContextCreated" })
        assertTrue("onFrameSwapped" in recovered)
    }

    @Test
    fun surfaceLostOnPresent_waitsForNextSurfaceCreated() {
        showWindow()
        runFrame()
        backend.presentResults.add(PresentResult.SURFACE_LOST)

        val lost = runFrame()
        assertFalse("onFrameSwapped" in lost)
        assertEquals("destroySurface", lost.last())
        assertNoWorkUntilSurfaceCreated()

        thread.surfaceCreated(window)
        assertEquals(
            listOf(
                "createSurface", "makeCurrent", "onSurfaceChanged(800,480)", "setSwapInterval(1)",
                "onDrawFrame", "present", "onFrameSwapped"
            ),
            runFrame()
        )
    }

    @Test
    fun newWindowWhileSurfaceStillHeld_recreatesSurfaceForNewWindow() {
        showWindow()
        runFrame()
        val textureB = SurfaceTexture(1)
        val windowB = Surface(textureB)
        try {
            thread.surfaceCreated(windowB)
            thread.surfaceChanged(800, 480)

            assertEquals(
                listOf(
                    "destroySurface", "createSurface", "makeCurrent", "onSurfaceChanged(800,480)",
                    "setSwapInterval(1)", "onDrawFrame", "present", "onFrameSwapped"
                ),
                runFrame()
            )
            assertSame(windowB, backend.lastWindow)

            assertEquals(listOf("makeCurrent", "onDrawFrame", "present", "onFrameSwapped"), runFrame())
        } finally {
            windowB.release()
            textureB.release()
        }
    }

    @Test
    fun sameWindowObjectCreatedAgain_recreatesSurface() {
        showWindow()
        runFrame()

        thread.surfaceCreated(window)

        val frame = runFrame()
        assertEquals(listOf("destroySurface", "createSurface"), frame.take(2))
        assertTrue("onSurfaceChanged(800,480)" in frame)
    }

    @Test
    fun createSurfaceFails_waitsForNextSurfaceCreated() {
        backend.createSurfaceResults.add(false)
        showWindow()

        assertFalse(thread.runOneIterationForTest())
        assertEquals(listOf("createContext", "createSurface"), log.take())
        assertNoWorkUntilSurfaceCreated()

        thread.surfaceCreated(window)
        val frame = runFrame()
        assertEquals(1, frame.count { it == "createSurface" })
        assertTrue("onSurfaceChanged(800,480)" in frame)
        assertTrue("onFrameSwapped" in frame)
    }

    @Test
    fun makeCurrentFails_waitsForNextSurfaceCreated() {
        showWindow()
        runFrame()
        backend.makeCurrentResults.add(false)

        assertFalse(thread.runOneIterationForTest())
        assertEquals(listOf("makeCurrent", "destroySurface"), log.take())
        assertNoWorkUntilSurfaceCreated()

        thread.surfaceCreated(window)
        val frame = runFrame()
        assertFalse("createContext" in frame)
        assertFalse("onContextCreated" in frame)
        assertTrue("createSurface" in frame)
        assertTrue("onSurfaceChanged(800,480)" in frame)
        assertTrue("onFrameSwapped" in frame)
    }

    private fun assertNoWorkUntilSurfaceCreated() {
        repeat(3) {
            assertFalse(thread.runOneIterationForTest())
        }
        thread.surfaceChanged(800, 480)
        assertFalse(thread.runOneIterationForTest())
        assertEquals(emptyList<String>(), log.take())
    }

    @Test
    fun requestRecreateContext_rebuildsContext() {
        showWindow()
        runFrame()

        thread.requestRecreateContext()
        val frame = runFrame()

        assertEquals(listOf("destroySurface", "destroyContext", "createContext"), frame.take(3))
        assertTrue(frame.indexOf("onContextCreated") > frame.indexOf("createContext"))
        assertTrue("onFrameSwapped" in frame)
    }

    @Test
    fun sizeChange_reportsNewSize() {
        showWindow()
        runFrame()

        thread.surfaceChanged(1280, 720)

        assertEquals(
            listOf("makeCurrent", "onSurfaceChanged(1280,720)", "onDrawFrame", "present", "onFrameSwapped"),
            runFrame()
        )
    }

    @Test
    fun zeroSize_doesNotDraw() {
        showWindow(0, 0)
        assertFalse(thread.runOneIterationForTest())
        assertFalse("onDrawFrame" in log.take())

        thread.surfaceChanged(800, 480)
        runFrame()
        thread.surfaceChanged(0, 0)

        assertFalse(thread.runOneIterationForTest())
        assertFalse("onDrawFrame" in log.take())
    }

    @Test
    fun pacingToggles_applyBeforeNextSwap() {
        showWindow()
        runFrame()

        pacing.vsyncEnabled = false
        val unsynced = runFrame()
        assertTrue(unsynced.indexOf("setSwapInterval(0)") in 0 until unsynced.indexOf("present"))

        pacing.presentationTimeEnabled = true
        val timed = runFrame()
        assertTrue(timed.indexOf("setPresentationTime") in 0 until timed.indexOf("present"))

        pacing.presentationTimeEnabled = false
        val cleared = runFrame()
        assertEquals(1, cleared.count { it == "clearPresentationTime" })
        assertTrue(cleared.indexOf("clearPresentationTime") < cleared.indexOf("present"))
        assertFalse("clearPresentationTime" in runFrame())
    }

    @Test
    fun exit_whilePaused_releasesEverythingAndJoins() {
        backend.presentDelayMillis = 1
        val firstFrame = CountDownLatch(1)
        renderer.onSwapped = Runnable { firstFrame.countDown() }
        showWindow()
        thread.start()
        assertTrue(firstFrame.await(2, TimeUnit.SECONDS))

        thread.onPause()
        val paused = log.snapshot()
        assertTrue("destroySurface" in paused)
        assertFalse("destroyContext" in paused)

        thread.requestExitAndWait()

        assertFalse(thread.isAlive)
        assertTrue("destroyContext" in log.snapshot())
        assertFalse(backend.hasSurface)
        assertFalse(backend.hasContext)
    }

    @Test
    fun renderTiming_holdsTheLatestReportedWork() {
        val work = ArrayList<Long>()
        thread.frameWorkListener = object : FrameWorkListener {
            override fun onFrameWork(workNanos: Long) {
                work.add(workNanos)
            }
        }
        RenderTiming.lastWorkNanos = -1L
        showWindow()

        repeat(2) { runFrame() }

        assertEquals(2, work.size)
        assertEquals(work.last(), RenderTiming.lastWorkNanos)
    }

    @Test
    fun frameWork_reportedPerFrame() {
        val work = ArrayList<Long>()
        thread.frameWorkListener = object : FrameWorkListener {
            override fun onFrameWork(workNanos: Long) {
                work.add(workNanos)
            }
        }
        showWindow()

        repeat(3) { runFrame() }
        backend.presentResults.add(PresentResult.CONTEXT_LOST)
        runFrame()

        assertEquals(3, work.size)
        assertTrue(work.all { it >= 0 })
    }

    // Whether the reported value is CPU time is verified on device: on the JVM the CPU time reads as 0 or -1.
    @Test
    fun frameWork_neverIncludesPresentTime() {
        var reported = -1L
        thread.frameWorkListener = object : FrameWorkListener {
            override fun onFrameWork(workNanos: Long) {
                reported = workNanos
            }
        }
        backend.presentDelayMillis = 30
        RenderTiming.lastWorkNanos = -1L
        showWindow()

        runFrame()

        assertTrue("work $reported", reported in 0 until TimeUnit.MILLISECONDS.toNanos(20))
        assertEquals(reported, RenderTiming.lastWorkNanos)
    }

    @Test
    fun choreographerPacing_createsWaiterLazily_andQuitsOnExit() {
        backend.presentDelayMillis = 1
        val source = ImmediateVsyncSource()
        var created = 0
        thread = RenderThread(backend, renderer, pacing) {
            created++
            VsyncWaiter(source, timeoutMillis = 100)
        }
        val firstFrame = CountDownLatch(1)
        renderer.onSwapped = Runnable { firstFrame.countDown() }
        showWindow()
        thread.start()
        assertTrue(firstFrame.await(2, TimeUnit.SECONDS))
        assertEquals(0, created)
        assertEquals(2L, source.requests.count)

        pacing.choreographerPacingEnabled = true
        assertTrue(source.requests.await(2, TimeUnit.SECONDS))
        thread.requestExitAndWait()

        assertFalse(thread.isAlive)
        assertEquals(1, created)
        assertEquals(1, source.quits)
    }

    @Test
    fun surfaceChanged_blocksUntilFrameWithNewSize() {
        backend.presentDelayMillis = 20
        val firstFrame = CountDownLatch(1)
        renderer.onSwapped = Runnable { firstFrame.countDown() }
        showWindow()
        thread.start()
        assertTrue(firstFrame.await(2, TimeUnit.SECONDS))

        val start = System.nanoTime()
        thread.surfaceChanged(1280, 720)
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000L

        assertEquals("1280x720", renderer.presentedSize)
        assertTrue(elapsedMillis < 2000)
    }

    @Test
    fun uiCalls_afterThreadDied_returnPromptly() {
        backend.createContextFailure = IllegalStateException("context")
        val handled = CountDownLatch(1)
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, _ -> handled.countDown() }
        showWindow()
        thread.start()
        assertTrue(handled.await(2, TimeUnit.SECONDS))

        val start = System.nanoTime()
        thread.surfaceChanged(1280, 720)
        thread.onPause()
        thread.onResume()
        thread.surfaceDestroyed()
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000L

        assertTrue(elapsedMillis < 500)
    }

    private class ImmediateVsyncSource : VsyncSource {

        /** Counts down on the second request, which is made after waiting for the first one. */
        val requests = CountDownLatch(2)

        @Volatile
        var quits = 0

        override fun requestNextVsync(callback: VsyncSource.Callback) {
            requests.countDown()
            val now = System.nanoTime()
            callback.onVsync(now, now)
        }

        override fun quit() {
            quits++
        }
    }

    @Test
    fun createContextThrows_propagatesToThread() {
        val failure = GLESVersionUnsupportedException("OpenGL ES 3.0", "OpenGL ES 2.0", "device", "chipset", "7.0")
        backend.createContextFailure = failure
        val caught = AtomicReference<Throwable>()
        val handled = CountDownLatch(1)
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            caught.set(e)
            handled.countDown()
        }
        showWindow()

        thread.start()

        assertTrue(handled.await(2, TimeUnit.SECONDS))
        assertSame(failure, caught.get())
    }
}
