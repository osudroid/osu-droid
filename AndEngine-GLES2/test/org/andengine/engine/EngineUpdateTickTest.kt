package org.andengine.engine

import org.andengine.engine.Engine.EngineLock
import org.andengine.engine.camera.Camera
import org.andengine.engine.handler.IUpdateHandler
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.andengine.graphics.DrawContext
import org.andengine.graphics.gles.GlesLegacyRestorers
import org.andengine.graphics.gles.installRecordingGL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineUpdateTickTest {
    private val lock = HandOverLock()
    private lateinit var engine: FakeCpuClockEngine

    @Before
    fun setUp() {
        installRecordingGL()
        engine = FakeCpuClockEngine(
            EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f))
                .also { it.setEngineLock(lock) }
        )
    }

    @After
    fun tearDown() {
        GlesLegacyRestorers.clearForTest()
    }

    private fun onUpdate(action: () -> Unit) {
        engine.registerUpdateHandler(object : IUpdateHandler {
            override fun onUpdate(pSecondsElapsed: Float) = action()

            override fun reset() {}
        })
    }

    private fun awaitUpdateHandedOver() {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!lock.drawMayRun()) {
            check(System.nanoTime() < deadline) { "The update tick did not hand its frame over" }
            Thread.sleep(1)
        }
    }

    @Test(timeout = 5000)
    fun frameUpdateWork_isTheCpuTimeOfTheTick_withoutSleeps() {
        assertEquals(0L, engine.frameUpdateWorkNanos)

        onUpdate {
            engine.cpuNanos += 3_000_000L
            // Mimics ThrottledFrameClock: blocks, but does no work.
            Thread.sleep(SLEEP_MILLIS)
        }
        engine.start()

        // The tick hands the frame to the draw side and then waits for it, so it runs on its own thread.
        val tick = Thread { engine.onTickUpdate() }
        tick.start()
        engine.onDrawFrame(DrawContext())
        tick.join()

        assertEquals(3_000_000L, engine.frameUpdateWorkNanos)
    }

    @Test(timeout = 5000)
    fun frameUpdateWork_staysWithItsFrame_whileTheNextTickRuns() {
        var ticks = 0

        onUpdate {
            ticks++
            engine.cpuNanos += if (ticks == 1) 3_000_000L else 7_000_000L
        }

        engine.start()

        val updates = Thread {
            engine.onTickUpdate()
            engine.onTickUpdate()
        }

        updates.start()

        // Draws the first frame, which lets the second tick run while this frame is still being presented.
        engine.onDrawFrame(DrawContext())
        awaitUpdateHandedOver()
        assertEquals(3_000_000L, engine.frameUpdateWorkNanos)

        engine.onDrawFrame(DrawContext())
        updates.join()
        assertEquals(7_000_000L, engine.frameUpdateWorkNanos)
    }

    @Test(timeout = 5000)
    fun frameUpdateWork_isZero_forFramesDrawnWhileNotRunning() {
        onUpdate { engine.cpuNanos += 3_000_000L }

        engine.start()

        val tick = Thread { engine.onTickUpdate() }
        tick.start()

        engine.onDrawFrame(DrawContext())
        tick.join()
        engine.stop()

        val idleTick = Thread { engine.onTickUpdate() }
        idleTick.start()

        engine.onDrawFrame(DrawContext())
        idleTick.join()

        assertEquals(0L, engine.frameUpdateWorkNanos)
    }

    /**
     * An [Engine] whose thread CPU time only advances when a test says so.
     */
    private class FakeCpuClockEngine(options: EngineOptions) : Engine(options) {
        @Volatile
        var cpuNanos = 0L

        override fun currentThreadCpuTimeNanos() = cpuNanos
    }

    private class HandOverLock : EngineLock(false) {
        /** Whether an update tick has handed its frame to the draw side. */
        fun drawMayRun(): Boolean {
            lock()
            try {
                return mDrawing.get()
            } finally {
                unlock()
            }
        }
    }

    private companion object {
        const val SLEEP_MILLIS = 5L
    }
}
