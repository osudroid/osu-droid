package org.andengine.graphics.view

import java.util.function.IntSupplier
import java.util.function.LongSupplier
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PerformanceHintsTest {

    private class FakeSession : PerformanceHints.Session {
        val reported = mutableListOf<Long>()
        val targets = mutableListOf<Long>()
        var closed = false
        var throwOnCall = false

        override fun updateTargetWorkDuration(targetNanos: Long) {
            if (throwOnCall) throw IllegalStateException("session failed")
            targets += targetNanos
        }

        override fun reportActualWorkDuration(workNanos: Long) {
            if (throwOnCall) throw IllegalStateException("session failed")
            reported += workNanos
        }

        override fun close() {
            closed = true
        }
    }

    private class FakeFactory : PerformanceHints.SessionFactory {
        val sessions = mutableListOf<FakeSession>()
        val threadIds = mutableListOf<IntArray>()
        val initialTargets = mutableListOf<Long>()
        var requests = 0
        var decline = false
        var throwOnCall = false

        override fun createSession(threadIds: IntArray, targetNanos: Long): PerformanceHints.Session? {
            requests++
            if (decline) {
                return null
            }
            this.threadIds += threadIds
            initialTargets += targetNanos
            return FakeSession().also {
                it.throwOnCall = throwOnCall
                sessions += it
            }
        }
    }

    private val updateThreadId = IntSupplier { UPDATE_TID }
    private val noUpdate = LongSupplier { 0L }

    private fun startedHints(
        factory: FakeFactory,
        updateThreadId: IntSupplier = this.updateThreadId,
        updateNanos: LongSupplier = noUpdate
    ) =
        PerformanceHints(factory, updateThreadId, updateNanos).apply {
            setTargetFrameRate(60f)
            onRenderThreadStarted(RENDER_TID)
        }

    @Test
    @Config(sdk = [30])
    fun belowApi31_doesNothing() {
        val factory = FakeFactory()
        val hints = PerformanceHints(factory, updateThreadId, noUpdate)

        hints.setTargetFrameRate(60f)
        hints.onRenderThreadStarted(RENDER_TID)
        hints.onFrameWork(3_000_000)
        hints.onRenderThreadExited()

        assertTrue(factory.sessions.isEmpty())
    }

    @Test
    @Config(sdk = [31])
    fun api31_createsSessionWithThreadIds_andReportsWork() {
        val factory = FakeFactory()
        val hints = PerformanceHints(factory, updateThreadId, noUpdate)
        hints.setTargetFrameRate(60f)
        hints.onRenderThreadStarted(RENDER_TID)

        hints.onFrameWork(3_000_000)

        assertEquals(1, factory.sessions.size)
        assertArrayEquals(intArrayOf(RENDER_TID, UPDATE_TID), factory.threadIds.single())
        assertEquals(listOf(3_000_000L), factory.sessions.single().reported)
    }

    @Test
    @Config(sdk = [31])
    fun targetFollowsRefreshRate() {
        val factory = FakeFactory()
        val hints = PerformanceHints(factory, updateThreadId, noUpdate)
        hints.setTargetFrameRate(60f)
        hints.onRenderThreadStarted(RENDER_TID)
        hints.onFrameWork(3_000_000)

        assertEquals(16_666_666L, factory.initialTargets.single())

        hints.setTargetFrameRate(120f)
        hints.onFrameWork(3_000_000)

        assertEquals(listOf(8_333_333L), factory.sessions.single().targets)
    }

    @Test
    @Config(sdk = [31])
    fun performanceHints_threadRestart_recreatesSession() {
        val factory = FakeFactory()
        val hints = PerformanceHints(factory, updateThreadId, noUpdate)
        hints.setTargetFrameRate(60f)
        hints.onRenderThreadStarted(RENDER_TID)
        hints.onFrameWork(3_000_000)

        hints.onRenderThreadStarted(RENDER_TID + 1)
        hints.onFrameWork(4_000_000)

        assertEquals(2, factory.sessions.size)
        assertTrue(factory.sessions[0].closed)
        assertArrayEquals(intArrayOf(RENDER_TID + 1, UPDATE_TID), factory.threadIds[1])
        assertEquals(listOf(4_000_000L), factory.sessions[1].reported)
    }

    @Test
    @Config(sdk = [31])
    fun zeroWork_isNotReported() {
        val factory = FakeFactory()
        val hints = PerformanceHints(factory, updateThreadId, noUpdate)
        hints.setTargetFrameRate(60f)
        hints.onRenderThreadStarted(RENDER_TID)
        hints.onFrameWork(3_000_000)

        hints.onFrameWork(0)

        assertEquals(listOf(3_000_000L), factory.sessions.single().reported)
    }

    @Test
    @Config(sdk = [31])
    fun platformCallThrows_dropsSessionAndKeepsRendering() {
        val factory = FakeFactory().apply { throwOnCall = true }
        val hints = startedHints(factory)

        hints.onFrameWork(3_000_000)
        hints.onFrameWork(3_000_000)

        assertEquals(1, factory.requests)
        assertTrue(factory.sessions.single().closed)
    }

    @Test
    @Config(sdk = [31])
    fun declinedSession_isNotRequestedAgain() {
        val factory = FakeFactory().apply { decline = true }
        val hints = startedHints(factory)

        hints.onFrameWork(3_000_000)
        hints.onFrameWork(3_000_000)

        assertEquals(1, factory.requests)
    }

    @Test
    @Config(sdk = [31])
    fun renderThreadExit_closesSession() {
        val factory = FakeFactory()
        val hints = startedHints(factory)
        hints.onFrameWork(3_000_000)

        hints.onRenderThreadExited()

        assertTrue(factory.sessions.single().closed)
    }

    @Test
    @Config(sdk = [31])
    fun unknownUpdateThread_noSessionUntilKnown() {
        val factory = FakeFactory()
        var updateTid = 0
        val hints = startedHints(factory, IntSupplier { updateTid })

        hints.onFrameWork(3_000_000)
        assertEquals(0, factory.requests)

        updateTid = UPDATE_TID
        hints.onFrameWork(3_000_000)

        assertArrayEquals(intArrayOf(RENDER_TID, UPDATE_TID), factory.threadIds.single())
        assertEquals(listOf(3_000_000L), factory.sessions.single().reported)
    }

    @Test
    @Config(sdk = [31])
    fun reportsUpdateTickWhenLongerThanRenderWork() {
        val factory = FakeFactory()
        val hints = startedHints(factory, updateNanos = LongSupplier { 9_000_000L })

        hints.onFrameWork(3_000_000)

        assertEquals(listOf(9_000_000L), factory.sessions.single().reported)
    }

    @Test
    @Config(sdk = [31])
    fun reportsRenderWorkWhenLongerThanUpdateTick() {
        val factory = FakeFactory()
        val hints = startedHints(factory, updateNanos = LongSupplier { 2_000_000L })

        hints.onFrameWork(5_000_000)

        assertEquals(listOf(5_000_000L), factory.sessions.single().reported)
    }

    private companion object {
        const val RENDER_TID = 11
        const val UPDATE_TID = 22
    }
}
