package com.osudroid.game.replay.analysis

import org.junit.Assert
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

class ReplayAnalysisDataTest {
    @Test
    fun `Single pointer produces one direct tap per DOWN`() {
        val data = ReplayAnalysisData(listOf(
            track(down(0, 10f, 10f), move(16, 20f, 20f), up(32), down(100, 50f, 50f), up(120))
        ))

        Assert.assertEquals(2, data.tapCount)
        Assert.assertArrayEquals(intArrayOf(0, 100), data.tapTimes)
        Assert.assertTrue(data.tapIsDirect.all { it })
        Assert.assertEquals(50f, data.tapXs[1], 0f)
        Assert.assertEquals(50f, data.tapYs[1], 0f)
    }

    @Test
    fun `DOWN while another pointer is held produces an induced tap at its latest position`() {
        val a = track(down(0, 10f, 10f), move(50, 30f, 30f), move(150, 40f, 40f), up(200))
        val b = track(down(100, 300f, 300f), up(120))
        val data = ReplayAnalysisData(listOf(a, b))

        Assert.assertEquals(3, data.tapCount)

        // At t=100: one direct tap at B's position, one induced tap at A's position as of t<=100, i.e. (30, 30).
        val at100 = (0 until data.tapCount).filter { data.tapTimes[it] == 100 }
        val direct = at100.single { data.tapIsDirect[it] }
        val induced = at100.single { !data.tapIsDirect[it] }

        Assert.assertEquals(300f, data.tapXs[direct], 0f)
        Assert.assertEquals(30f, data.tapXs[induced], 0f)
        Assert.assertEquals(30f, data.tapYs[induced], 0f)
    }

    @Test
    fun `Held pointer event exactly at DOWN time is used`() {
        val a = track(down(0, 10f, 10f), move(100, 40f, 40f), up(200))
        val b = track(down(100, 300f, 300f), up(120))
        val data = ReplayAnalysisData(listOf(a, b))

        val induced = (0 until data.tapCount).single { !data.tapIsDirect[it] }

        Assert.assertEquals(100, data.tapTimes[induced])
        Assert.assertEquals(40f, data.tapXs[induced], 0f)
        Assert.assertEquals(40f, data.tapYs[induced], 0f)
    }

    @Test
    fun `Pointer released before DOWN produces no induced tap`() {
        val a = track(down(0, 10f, 10f), up(50))
        val b = track(down(100, 300f, 300f), up(120))
        val data = ReplayAnalysisData(listOf(a, b))

        Assert.assertEquals(2, data.tapCount)
        Assert.assertTrue(data.tapIsDirect.all { it })
    }

    @Test
    fun `Pointer released exactly at DOWN time produces no induced tap`() {
        val a = track(down(0, 10f, 10f), up(100))
        val b = track(down(100, 300f, 300f), up(120))
        val data = ReplayAnalysisData(listOf(a, b))

        Assert.assertEquals(2, data.tapCount)
        Assert.assertTrue(data.tapIsDirect.all { it })
    }

    @Test
    fun `Three held pointers produce two induced taps per DOWN`() {
        val a = track(down(0, 10f, 10f), up(500))
        val b = track(down(10, 20f, 20f), up(500))
        val c = track(down(100, 30f, 30f), up(500))
        val data = ReplayAnalysisData(listOf(a, b, c))

        val at100 = (0 until data.tapCount).filter { data.tapTimes[it] == 100 }

        Assert.assertEquals(1, at100.count { data.tapIsDirect[it] })
        Assert.assertEquals(2, at100.count { !data.tapIsDirect[it] })
        Assert.assertEquals(setOf(10f, 20f), at100.filter { !data.tapIsDirect[it] }.map { data.tapXs[it] }.toSet())
    }

    @Test
    fun `Taps are sorted by time across pointers`() {
        val a = track(down(200, 10f, 10f), up(210))
        val b = track(down(50, 20f, 20f), up(60))
        val data = ReplayAnalysisData(listOf(a, b))

        Assert.assertArrayEquals(intArrayOf(50, 200), data.tapTimes)
        Assert.assertEquals(20f, data.tapXs[0], 0f)
        Assert.assertEquals(10f, data.tapXs[1], 0f)
    }

    @Test
    fun `Empty tracks produce no taps`() {
        val data = ReplayAnalysisData(listOf(track(), track()))

        Assert.assertEquals(0, data.tapCount)
    }

    @Test
    fun `fromMoveArrays copies events up to size`() {
        val moves = Replay.MoveArray(10)
        moves.pushBack(null, 0, 10f, 20f, TouchType.DOWN)
        // size < 2 here, so the collinearity check short-circuits and the move is kept.
        moves.pushBack(null, 16, 30f, 40f, TouchType.MOVE)
        moves.pushBack(32, TouchType.UP)

        val data = ReplayAnalysisData.fromMoveArrays(listOf(moves, Replay.MoveArray(5)))

        Assert.assertEquals(2, data.tracks.size)
        Assert.assertEquals(3, data.tracks[0].size)
        Assert.assertArrayEquals(intArrayOf(0, 16, 32), data.tracks[0].times)
        Assert.assertEquals(30f, data.tracks[0].xs[1], 0f)
        Assert.assertEquals(40f, data.tracks[0].ys[1], 0f)
        Assert.assertEquals(TouchType.UP.id, data.tracks[0].types[2])
        Assert.assertEquals(0, data.tracks[1].size)
    }

    private data class TestTouchEvent(val type: TouchType, val time: Int, val x: Float = 0f, val y: Float = 0f)

    private fun down(time: Int, x: Float, y: Float) = TestTouchEvent(TouchType.DOWN, time, x, y)
    private fun move(time: Int, x: Float, y: Float) = TestTouchEvent(TouchType.MOVE, time, x, y)
    private fun up(time: Int) = TestTouchEvent(TouchType.UP, time)

    private fun track(vararg events: TestTouchEvent) = ReplayAnalysisTrack(
        IntArray(events.size) { events[it].time },
        FloatArray(events.size) { events[it].x },
        FloatArray(events.size) { events[it].y },
        ByteArray(events.size) { events[it].type.id }
    )
}
