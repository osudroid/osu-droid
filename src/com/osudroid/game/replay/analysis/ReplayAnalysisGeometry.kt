package com.osudroid.game.replay.analysis

import com.edlplan.framework.utils.FloatArraySlice
import com.osudroid.utils.firstIndexAtOrAfter
import com.osudroid.utils.lastIndexAtOrBefore
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

/**
 * Builds the triangle geometry of the replay analysis overlay, which consists of the recent cursor path, input
 * frame dots, and tap rings.
 *
 * Every layer is a list of vertices (2 floats each) drawn as `GL_TRIANGLES`, without indexing. The layers are
 * rebuilt into reused buffers on every [build], so no allocation happens in the steady state.
 *
 * @param data The [ReplayAnalysisData] to build geometry from. Positions are in screen coordinates.
 * @param scale The scale applied to sizes. This is used to convert osu! pixels to screen pixels.
 */
class ReplayAnalysisGeometry(private val data: ReplayAnalysisData, private val scale: Float) {
    /**
     * The cursor path layer. The slice's [FloatArraySlice.offset] is always 0.
     */
    val path = createEmptyFloatArraySlice()

    /**
     * The input frame dot layer. The slice's [FloatArraySlice.offset] is always 0.
     */
    val frameDots = createEmptyFloatArraySlice()

    /**
     * The induced tap ring layer. The slice's [FloatArraySlice.offset] is always 0.
     */
    val inducedTaps = createEmptyFloatArraySlice()

    /**
     * The direct tap layer (ring and center dot). The slice's [FloatArraySlice.offset] is always 0.
     */
    val directTaps = createEmptyFloatArraySlice()

    /**
     * Rebuilds all layers for the window `[nowMs - displayLengthMs, nowMs]`, with both bounds inclusive.
     *
     * @param nowMs The current time, in milliseconds.
     * @param displayLengthMs The length of the window, in milliseconds.
     * @param showCursorPath Whether to build the [path] layer.
     * @param showFrameMarkers Whether to build the [frameDots] layer.
     * @param showTapMarkers Whether to build the [inducedTaps] and [directTaps] layers.
     */
    fun build(
        nowMs: Float,
        displayLengthMs: Int,
        showCursorPath: Boolean,
        showFrameMarkers: Boolean,
        showTapMarkers: Boolean
    ) {
        path.length = 0
        frameDots.length = 0
        inducedTaps.length = 0
        directTaps.length = 0

        val startMs = ceil(nowMs - displayLengthMs).toInt()
        val endMs = floor(nowMs).toInt()

        if (showCursorPath || showFrameMarkers) {
            for (i in data.tracks.indices) {
                val track = data.tracks[i]
                val from = track.times.firstIndexAtOrAfter(startMs)
                val to = track.times.lastIndexAtOrBefore(endMs)

                if (showCursorPath) {
                    track.buildPath(from, to)
                }

                if (showFrameMarkers) {
                    track.buildFrameDots(from, to)
                }
            }
        }

        if (showTapMarkers) {
            val from = data.tapTimes.firstIndexAtOrAfter(startMs)
            val to = data.tapTimes.lastIndexAtOrBefore(endMs)

            for (k in from..to) {
                val x = data.tapXs[k]
                val y = data.tapYs[k]

                if (data.tapIsDirect[k]) {
                    directTaps.ensureCapacity(directTaps.length + DIRECT_TAP_FLOATS)
                    directTaps.addRing(x, y)
                    directTaps.addFan(x, y, TAP_DOT_DIAMETER / 2 * scale, TAP_DOT_COS, TAP_DOT_SIN)
                } else {
                    inducedTaps.ensureCapacity(inducedTaps.length + INDUCED_TAP_FLOATS)
                    inducedTaps.addRing(x, y)
                }
            }
        }
    }

    private fun ReplayAnalysisTrack.buildPath(from: Int, to: Int) {
        val halfWidth = PATH_WIDTH * scale / 2

        for (k in from until to) {
            if (types[k] == TouchType.UP.id || types[k + 1] == TouchType.UP.id) {
                continue
            }

            val x0 = xs[k]
            val y0 = ys[k]
            val x1 = xs[k + 1]
            val y1 = ys[k + 1]
            val dx = x1 - x0
            val dy = y1 - y0
            val length = sqrt(dx * dx + dy * dy)

            // Skips zero-length segments (no direction) and avoids NaN normals.
            if (length <= 0f || !length.isFinite()) {
                continue
            }

            val nx = -dy / length * halfWidth
            val ny = dx / length * halfWidth

            path.ensureCapacity(path.length + PATH_SEGMENT_FLOATS)
            path.addTriangle(x0 + nx, y0 + ny, x0 - nx, y0 - ny, x1 + nx, y1 + ny)
            path.addTriangle(x1 + nx, y1 + ny, x0 - nx, y0 - ny, x1 - nx, y1 - ny)
        }
    }

    private fun ReplayAnalysisTrack.buildFrameDots(from: Int, to: Int) {
        val radius = FRAME_DOT_DIAMETER / 2 * scale

        for (k in from..to) {
            if (types[k] == TouchType.UP.id) {
                continue
            }

            frameDots.ensureCapacity(frameDots.length + FRAME_DOT_FLOATS)
            frameDots.addFan(xs[k], ys[k], radius, FRAME_DOT_COS, FRAME_DOT_SIN)
        }
    }

    private fun FloatArraySlice.addRing(cx: Float, cy: Float) {
        val outer = TAP_RING_DIAMETER / 2 * scale
        val inner = outer - TAP_RING_THICKNESS * scale

        for (k in 0 until TAP_RING_SEGMENTS) {
            val c0 = TAP_RING_COS[k]
            val s0 = TAP_RING_SIN[k]
            val c1 = TAP_RING_COS[k + 1]
            val s1 = TAP_RING_SIN[k + 1]

            addTriangle(
                cx + outer * c0, cy + outer * s0,
                cx + inner * c0, cy + inner * s0,
                cx + outer * c1, cy + outer * s1
            )

            addTriangle(
                cx + outer * c1, cy + outer * s1,
                cx + inner * c0, cy + inner * s0,
                cx + inner * c1, cy + inner * s1
            )
        }
    }

    private fun FloatArraySlice.addFan(
        cx: Float,
        cy: Float,
        radius: Float,
        cos: FloatArray,
        sin: FloatArray
    ) {
        for (k in 0 until cos.size - 1) {
            addTriangle(
                cx, cy,
                cx + radius * cos[k], cy + radius * sin[k],
                cx + radius * cos[k + 1], cy + radius * sin[k + 1]
            )
        }
    }

    private fun FloatArraySlice.addTriangle(
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ) {
        ary[length++] = x0
        ary[length++] = y0
        ary[length++] = x1
        ary[length++] = y1
        ary[length++] = x2
        ary[length++] = y2
    }

    private fun FloatArraySlice.ensureCapacity(needed: Int) {
        if (ary.size < needed) {
            ary = ary.copyOf(max(needed, ary.size * 3 / 2))
        }
    }

    private fun createEmptyFloatArraySlice() = FloatArraySlice().apply {
        ary = FloatArray(0)
        offset = 0
        length = 0
    }

    private companion object {
        // Sizes, in osu! pixels.
        const val PATH_WIDTH = 2f
        const val FRAME_DOT_DIAMETER = 2.5f
        const val FRAME_DOT_SEGMENTS = 8
        const val TAP_RING_DIAMETER = 16f
        const val TAP_RING_THICKNESS = 1.6f
        const val TAP_RING_SEGMENTS = 20
        const val TAP_DOT_DIAMETER = 2f
        const val TAP_DOT_SEGMENTS = 8

        // Floats per primitive: 2 per vertex, 3 vertices per triangle.
        const val PATH_SEGMENT_FLOATS = 2 * 6
        const val FRAME_DOT_FLOATS = FRAME_DOT_SEGMENTS * 6
        const val INDUCED_TAP_FLOATS = TAP_RING_SEGMENTS * 2 * 6
        const val DIRECT_TAP_FLOATS = INDUCED_TAP_FLOATS + TAP_DOT_SEGMENTS * 6

        // Unit circle tables of length `segments + 1`; the last entry closes the circle.
        val FRAME_DOT_COS = circleTable(FRAME_DOT_SEGMENTS, ::cos)
        val FRAME_DOT_SIN = circleTable(FRAME_DOT_SEGMENTS, ::sin)
        val TAP_RING_COS = circleTable(TAP_RING_SEGMENTS, ::cos)
        val TAP_RING_SIN = circleTable(TAP_RING_SEGMENTS, ::sin)
        val TAP_DOT_COS = circleTable(TAP_DOT_SEGMENTS, ::cos)
        val TAP_DOT_SIN = circleTable(TAP_DOT_SEGMENTS, ::sin)

        fun circleTable(segments: Int, function: (Double) -> Double) =
            FloatArray(segments + 1) { function(2 * PI * it / segments).toFloat() }
    }
}
