package com.osudroid.game.replay.analysis

import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

class ReplayAnalysisGeometryTest {
    private val a = track(down(0, 0f, 0f), move(100, 10f, 0f), move(200, 20f, 0f), up(300))

    @Test
    fun `Window includes events exactly at both bounds`() {
        val g = geometry(a)
        g.build(200f, 100, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true) // Window [100, 200].

        Assert.assertEquals(2 * DOT, g.frameDots.length)
        Assert.assertEquals(1 * SEGMENT, g.path.length)
        Assert.assertEquals(0, g.directTaps.length)
    }

    @Test
    fun `Window excludes events outside bounds`() {
        val g = geometry(a)
        g.build(150f, 40, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true) // Window [110, 150].

        assertAllEmpty(g)
    }

    @Test
    fun `Nothing is visible before the first event`() {
        val g = geometry(a)
        g.build(-500f, 800, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        assertAllEmpty(g)
    }

    @Test
    fun `Nothing is visible after the last window`() {
        val g = geometry(a)
        g.build(2000f, 800, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        assertAllEmpty(g)
    }

    @Test
    fun `Segment with one endpoint outside the window is not drawn`() {
        val g = geometry(a)
        g.build(200f, 150, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true) // Window [50, 200].

        Assert.assertEquals(1 * SEGMENT, g.path.length)
    }

    @Test
    fun `Path does not bridge UP`() {
        val g = geometry(
            track(
                down(0, 0f, 0f),
                move(50, 10f, 0f),
                up(100),
                down(150, 100f, 0f),
                move(200, 110f, 0f),
                up(250)
            )
        )

        g.build(250f, 1000, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        Assert.assertEquals(2 * SEGMENT, g.path.length)
        Assert.assertEquals(4 * DOT, g.frameDots.length)
        Assert.assertEquals(2 * DIRECT, g.directTaps.length)
    }

    @Test
    fun `Zero-length path segments are skipped`() {
        val g = geometry(track(down(0, 5f, 5f), move(50, 5f, 5f), move(100, 15f, 5f)))
        g.build(100f, 1000, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        Assert.assertEquals(1 * SEGMENT, g.path.length)

        for (i in 0 until g.path.length) {
            Assert.assertTrue(g.path.ary[i].isFinite())
        }
    }

    @Test
    fun `Tap without movement draws a dot and a tap but no path`() {
        val g = geometry(track(down(0, 5f, 5f), up(10)))
        g.build(10f, 800, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        Assert.assertEquals(0, g.path.length)
        Assert.assertEquals(DOT, g.frameDots.length)
        Assert.assertEquals(DIRECT, g.directTaps.length)
    }

    @Test
    fun `Induced taps go to the induced layer`() {
        val a2 = track(down(0, 0f, 0f), move(50, 10f, 0f), up(200))
        val b = track(down(100, 50f, 50f), up(120))
        val g = geometry(a2, b)

        g.build(150f, 1000, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        Assert.assertEquals(2 * DIRECT, g.directTaps.length)
        Assert.assertEquals(1 * INDUCED, g.inducedTaps.length)
    }

    @Test
    fun `Disabled layers have zero length`() {
        val g = geometry(a)
        g.build(200f, 1000, showCursorPath = false, showFrameMarkers = false, showTapMarkers = false)

        assertAllEmpty(g)

        g.build(200f, 1000, showCursorPath = true, showFrameMarkers = false, showTapMarkers = false)

        Assert.assertTrue(g.path.length > 0)
        Assert.assertEquals(0, g.frameDots.length)
        Assert.assertEquals(0, g.inducedTaps.length)
        Assert.assertEquals(0, g.directTaps.length)
    }

    @Test
    fun `Frame dot vertices are scaled`() {
        val x = 100f
        val y = 100f

        val g = ReplayAnalysisGeometry(ReplayAnalysisData(listOf(track(down(0, x, y), up(10)))), scale = 2f)
        g.build(0f, 800, showCursorPath = false, showFrameMarkers = true, showTapMarkers = false)

        // Radius = 5 / 2 * 2 = 5 screen px.
        val distances = (0 until g.frameDots.length step 2).map {
            hypot(g.frameDots.ary[it] - x, g.frameDots.ary[it + 1] - y)
        }

        Assert.assertTrue(distances.all { it <= 5f + 1e-4f })
        Assert.assertEquals(5f, distances.max(), 1e-4f)
    }

    @Test
    fun `Path quad spans the segment with the scaled half-width`() {
        val g = ReplayAnalysisGeometry(
            ReplayAnalysisData(listOf(track(down(0, 0f, 0f), move(100, 10f, 0f), up(200)))),
            scale = 2f
        )
        g.build(100f, 1000, showCursorPath = true, showFrameMarkers = false, showTapMarkers = false)

        // Half-width = PATH_WIDTH / 2 * scale = 4 / 2 * 2 = 4.
        Assert.assertEquals(12, g.path.length)

        val vertices = (0 until g.path.length step 2).map { g.path.ary[it] to g.path.ary[it + 1] }

        for ((x, y) in vertices) {
            Assert.assertTrue("x=$x", abs(x) < 1e-4f || abs(x - 10f) < 1e-4f)
            Assert.assertTrue("y=$y", abs(y - -4f) < 1e-4f || abs(y - 4f) < 1e-4f)
        }

        for ((cx, cy) in listOf(0f to -4f, 0f to 4f, 10f to -4f, 10f to 4f)) {
            Assert.assertTrue(
                "Missing corner ($cx, $cy)",
                vertices.any { abs(it.first - cx) < 1e-4f && abs(it.second - cy) < 1e-4f }
            )
        }
    }

    @Test
    fun `Tap ring vertices lie on the scaled inner and outer radii`() {
        val ringA = track(down(0, 0f, 0f), up(200))
        val ringB = track(down(100, 50f, 50f), up(120))
        val g = ReplayAnalysisGeometry(ReplayAnalysisData(listOf(ringA, ringB)), scale = 2f)
        g.build(150f, 1000, showCursorPath = false, showFrameMarkers = false, showTapMarkers = true)

        val inducedTaps = g.inducedTaps
        Assert.assertEquals(240, inducedTaps.length)

        // Outer radius = TAP_RING_DIAMETER / 2 * scale = 32 / 2 * 2 = 32; inner radius = outer - TAP_RING_THICKNESS * scale = 32 - 3.2 * 2 = 25.6.
        val distances = (0 until inducedTaps.length step 2).map {
            hypot(inducedTaps.ary[it], inducedTaps.ary[it + 1])
        }

        Assert.assertTrue(distances.all { abs(it - 32f) < 1e-3f || abs(it - 25.6f) < 1e-3f })
        Assert.assertEquals(25.6f, distances.min(), 1e-3f)
        Assert.assertEquals(32f, distances.max(), 1e-3f)
    }

    @Test
    fun `Rebuilding at an earlier time matches a fresh build`() {
        val b = track(down(100, 50f, 50f), up(120))
        val g1 = geometry(a, b)

        g1.build(200f, 1000, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)
        g1.build(100f, 1000, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)
        g1.build(200f, 1000, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        val g2 = geometry(a, b)
        g2.build(200f, 1000, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        for ((x, y) in listOf(
            g1.path to g2.path,
            g1.frameDots to g2.frameDots,
            g1.inducedTaps to g2.inducedTaps,
            g1.directTaps to g2.directTaps
        )) {
            Assert.assertEquals(y.length, x.length)
            Assert.assertArrayEquals(y.ary.copyOf(y.length), x.ary.copyOf(x.length), 0f)
        }
    }

    @Test
    fun `Buffers are reused when output does not grow`() {
        val g = geometry(a)
        g.build(300f, 1000, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        val ary = g.frameDots.ary
        g.build(200f, 100, showCursorPath = true, showFrameMarkers = true, showTapMarkers = true)

        Assert.assertSame(ary, g.frameDots.ary)
    }

    private fun assertAllEmpty(g: ReplayAnalysisGeometry) {
        Assert.assertEquals(0, g.path.length)
        Assert.assertEquals(0, g.frameDots.length)
        Assert.assertEquals(0, g.inducedTaps.length)
        Assert.assertEquals(0, g.directTaps.length)
    }

    private fun geometry(vararg tracks: ReplayAnalysisTrack) =
        ReplayAnalysisGeometry(ReplayAnalysisData(tracks.toList()), 1f)

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

    private companion object {
        const val SEGMENT = 12
        const val DOT = 48
        const val INDUCED = 240
        const val DIRECT = 288
    }
}
