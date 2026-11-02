package com.osudroid.game

import com.rian.andengine.timing.FrameTimeInfo
import com.rian.andengine.timing.IClock
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameplayFrameStabilityClockTest {
    private lateinit var reference: FakeReferenceClock
    private lateinit var clock: GameplayFrameStabilityClock

    private var wallNanos = 0L

    /**
     * The "true" BASS position. [FakeReferenceClock.currentTime] usually equals this, except when a test injects a
     * bogus reading.
     */
    private var bassTime = 0f

    @Before
    fun setUp() {
        wallNanos = 1_000_000_000L
        bassTime = 0f
        reference = FakeReferenceClock()
        clock = GameplayFrameStabilityClock(reference) { wallNanos }

        // First consumption.
        clock.processFrame()
    }

    @Test
    fun stallOf800ms_recoversWithinFewFrames() {
        repeat(60) { frame(FRAME) }
        assertTracksBass()

        // A ~0.8 s stall (e.g. GL context recreation): wall clock and BASS both advance 0.8 s in one frame.
        frame(0.8f)

        for (i in 0 until 5) {
            frame(FRAME)
        }

        assertTracksBass("after an 800 ms stall the clock must re-sync within 5 frames")

        var previous = clock.currentTime

        repeat(30) {
            frame(FRAME)

            assertTrue("clock must keep advancing after recovery", clock.currentTime > previous)
            assertTracksBass()
            previous = clock.currentTime
        }
    }

    @Test
    fun persistentBassLead_resyncsWithinBoundedFrames() {
        repeat(60) { frame(FRAME) }

        // BASS (consistently) runs 0.77 s ahead of a normal 16 ms frame, as seen on device
        // (expected 14.195, BASS 14.963). Afterwards it advances normally.
        bassTime += 0.77f
        frame(FRAME)

        var frozenFrames = 0

        repeat(MAX_FROZEN_FRAMES + 1) {
            val before = clock.currentTime
            frame(FRAME)
            if (clock.currentTime == before) frozenFrames++
        }

        assertTrue("clock froze for $frozenFrames frames", frozenFrames <= MAX_FROZEN_FRAMES)
        assertTracksBass("a consistent BASS lead must be re-synced within a bounded number of frames")

        repeat(30) {
            val before = clock.currentTime
            frame(FRAME)
            assertTrue("clock must keep advancing", clock.currentTime > before)
            assertTracksBass()
        }
    }

    @Test
    fun singleBogusBassValue_isIgnored() {
        repeat(60) { frame(FRAME) }

        // One bogus BASS value 2 s off while only 16 ms of real time passed.
        bassTime += FRAME
        wallNanos += (FRAME * 1e9f).toLong()
        reference.currentTime = bassTime + 2f
        var previous = clock.currentTime
        clock.processFrame()

        assertTrue("bogus BASS value must not be applied", clock.currentTime - previous < 0.1f)
        previous = clock.currentTime

        // Subsequent values are normal again.
        repeat(30) { i ->
            frame(FRAME)

            val step = clock.currentTime - previous
            assertTrue("clock must not jump (step $step)", step < 0.1f)
            // The first frame after the glitch may still hold; afterwards the clock must advance every frame.
            if (i == 0) {
                assertTrue("clock must not go back (step $step)", step >= 0f)
            } else {
                assertTrue("clock must advance after the glitch (frame $i, step $step)", step > 0f)
            }
            previous = clock.currentTime
        }

        assertTracksBass("clock must continue tracking BASS after a single glitch")
    }

    @Test
    fun persistentBassLag_neverJumpsBackward() {
        repeat(60) { frame(FRAME) }

        // BASS falls 0.8 s behind the clock and then advances normally. GameScene cannot handle a silent backward
        // jump, so the clock must hold or walk back at most one frame per frame until BASS has caught up.
        bassTime -= 0.8f
        var previous = clock.currentTime

        repeat(60) { i ->
            frame(FRAME)

            val step = clock.currentTime - previous
            assertTrue("clock went back by $step in frame $i", step >= -FRAME - 1e-4f)
            previous = clock.currentTime
        }

        assertTracksBass("the clock must meet BASS again once BASS has caught up")
    }

    @Test
    fun stallDuringLeadIn_resyncStopsAtGameplayStart() {
        clock.gameplayStartTime = 5f
        repeat(240) { frame(FRAME) }
        assertTrue("still in the lead-in (clock ${clock.currentTime})", clock.currentTime < 5f)

        // A 2 s stall that would carry the clock past the start of gameplay.
        frame(2f)

        assertEquals("a re-sync during the lead-in must stop at gameplayStartTime", 5f, clock.currentTime, 1e-4f)

        frame(FRAME)
        frame(FRAME)
        assertTracksBass("after reaching gameplayStartTime the clock re-syncs to BASS")
    }

    @Test
    fun staleBassAfterStallAtHighRate_resyncsViaConsistentReference() {
        reference.rate = 1.5f
        repeat(60) { frameAtRate(FRAME) }
        assertTracksBass(frameTime = RATE_FRAME)

        // A 0.4 s stall (0.6 s of track time at 1.5x) that BASS only reports one frame late: the stall frame still
        // reads the old position and is ignored as a glitch, the next frame's reading is far from the (held) clock but
        // consistent with BASS's own last trusted reading plus real time, so the clock re-syncs at once.
        wallNanos += (0.4f * 1e9f).toLong()
        val staleTime = bassTime
        bassTime += 0.4f * 1.5f
        reference.currentTime = staleTime
        clock.processFrame()

        frameAtRate(FRAME)

        assertTracksBass("a consistent BASS reading must re-sync the clock immediately", RATE_FRAME)

        var previous = clock.currentTime
        repeat(30) {
            frameAtRate(FRAME)
            assertTrue("clock must keep advancing", clock.currentTime > previous)
            assertTracksBass(frameTime = RATE_FRAME)
            previous = clock.currentTime
        }
    }

    @Test
    fun normalPlayback_unchanged() {
        var previous = clock.currentTime

        repeat(300) {
            frame(FRAME)

            assertTrue("clock must advance every frame", clock.currentTime > previous)
            assertTracksBass()
            assertEquals(false, clock.requiresCatchUp)
            previous = clock.currentTime
        }
    }

    private fun frame(elapsed: Float) {
        wallNanos += (elapsed * 1e9f).toLong()
        bassTime += elapsed
        reference.currentTime = bassTime
        clock.processFrame()
    }

    /** Like [frame], but BASS advances by [elapsed] times the reference clock's rate. */
    private fun frameAtRate(elapsed: Float) {
        wallNanos += (elapsed * 1e9f).toLong()
        bassTime += elapsed * reference.rate
        reference.currentTime = bassTime
        clock.processFrame()
    }

    private fun assertTracksBass(message: String = "clock must track BASS within one frame", frameTime: Float = FRAME) {
        val diff = abs(clock.currentTime - bassTime)
        assertTrue("$message (clock ${clock.currentTime}, BASS $bassTime)", diff <= frameTime + 1e-4f)
    }

    private class FakeReferenceClock : IGameplayClock {
        override var currentTime = 0f
        override var rate = 1f
        override var isRunning = true
        override val elapsedFrameTime = 0f
        override val framesPerSecond = 0f
        override val timeInfo = FrameTimeInfo()
        override var userGlobalOffset = 0f
        override var userBeatmapOffset = 0f
        override val totalAppliedOffset = 0f
        override val source: IClock
            get() = this

        override fun processFrame() {}
        override fun reset() {
            currentTime = 0f
        }

        override fun start() {
            isRunning = true
        }

        override fun stop() {
            isRunning = false
        }

        override fun seek(position: Float): Boolean {
            currentTime = position
            return true
        }

        override fun resetSpeedAdjustments() {
            rate = 1f
        }

        override fun changeSource(source: IClock?) {}
    }

    private companion object {
        const val FRAME = 1f / 60

        /** One 60 fps frame of track time at rate 1.5. */
        const val RATE_FRAME = 1.5f / 60

        /**
         * Upper bound of consecutive frozen frames documented in [GameplayFrameStabilityClock].
         */
        const val MAX_FROZEN_FRAMES = 5
    }
}
