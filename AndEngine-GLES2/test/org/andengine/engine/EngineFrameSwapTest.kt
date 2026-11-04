package org.andengine.engine

import org.andengine.engine.Engine.EngineLock
import org.andengine.engine.camera.Camera
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.andengine.graphics.DrawContext
import org.andengine.graphics.gles.GlesLegacyRestorers
import org.andengine.graphics.gles.installRecordingGL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineFrameSwapTest {

    private class CountingLock : EngineLock(false) {
        var released = 0

        override fun notifyCanUpdate() {
            released++
            super.notifyCanUpdate()
        }

        /** Whether the update thread would currently be allowed to run. */
        fun updateMayRun(): Boolean {
            lock()
            try {
                return !mDrawing.get()
            } finally {
                unlock()
            }
        }

        /** Hands the frame to the draw side, the way a finished update tick does. */
        fun handOverToDraw() {
            lock()
            try {
                notifyCanDraw()
            } finally {
                unlock()
            }
        }
    }

    private lateinit var lock: CountingLock
    private lateinit var engine: Engine

    @Before
    fun setUp() {
        installRecordingGL()
        lock = CountingLock()
        engine = Engine(
            EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f))
                .also { it.setEngineLock(lock) }
        )
    }

    @After
    fun tearDown() {
        GlesLegacyRestorers.clearForTest()
    }

    private fun drawFrame() {
        lock.handOverToDraw()
        lock.released = 0
        engine.onDrawFrame(DrawContext())
    }

    @Test
    fun default_releasesUpdateRightAfterDraw() {
        drawFrame()

        assertTrue(lock.updateMayRun())
        assertEquals(1, lock.released)
    }

    @Test
    fun releaseAfterSwap_holdsUpdateUntilSwapped() {
        engine.setReleaseUpdateAfterSwap(true)

        drawFrame()
        assertFalse(lock.updateMayRun())
        assertEquals(0, lock.released)

        engine.onFrameSwapped()
        assertTrue(lock.updateMayRun())
        assertEquals(1, lock.released)
    }

    @Test
    fun releaseAfterSwap_toggledMidFrame_neverStrandsUpdate() {
        engine.setReleaseUpdateAfterSwap(true)
        drawFrame()
        engine.setReleaseUpdateAfterSwap(false)

        engine.onFrameSwapped()
        engine.onFrameSwapped()

        assertTrue(lock.updateMayRun())
        assertEquals(1, lock.released)
    }

    @Test
    fun frameSwappedWithoutPending_doesNothing() {
        engine.onFrameSwapped()
        assertEquals(0, lock.released)

        drawFrame()
        engine.onFrameSwapped()
        assertEquals(1, lock.released)
    }

    @Test(timeout = 5000)
    fun frameSwappedWithoutPending_doesNotTakeTheLock() {
        drawFrame()
        val held = CountDownLatch(1)
        val done = CountDownLatch(1)
        val holder = Thread {
            lock.lock()
            try {
                held.countDown()
                done.await()
            } finally {
                lock.unlock()
            }
        }
        holder.start()
        try {
            assertTrue(held.await(1, TimeUnit.SECONDS))

            engine.onFrameSwapped()

            assertEquals(1, lock.released)
        } finally {
            done.countDown()
            holder.join()
        }
    }

    @Test
    fun deferredFrameNeverSwapped_nextFrameWithFlagOff_releasesOnce_andLateSwapDoesNothing() {
        engine.setReleaseUpdateAfterSwap(true)
        drawFrame()
        engine.setReleaseUpdateAfterSwap(false)

        drawFrame()
        assertEquals(1, lock.released)

        engine.onFrameSwapped()
        assertEquals(1, lock.released)
    }

    @Test
    fun deferredFrameNeverSwapped_nextDeferredFrame_releasesOnItsSwap() {
        engine.setReleaseUpdateAfterSwap(true)
        drawFrame()
        drawFrame()
        assertEquals(0, lock.released)

        engine.onFrameSwapped()
        assertEquals(1, lock.released)

        engine.onFrameSwapped()
        assertEquals(1, lock.released)
    }

    @Test
    fun flagToggledOnBetweenFrames_defersOnlyTheLaterFrame() {
        drawFrame()
        assertEquals(1, lock.released)

        engine.setReleaseUpdateAfterSwap(true)
        drawFrame()
        assertEquals(0, lock.released)

        engine.onFrameSwapped()
        assertEquals(1, lock.released)
    }
}
