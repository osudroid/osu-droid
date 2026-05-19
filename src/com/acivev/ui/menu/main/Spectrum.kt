package com.acivev.ui.menu.main

import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.buffered.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import kotlin.math.*

/**
 * Audio-reactive spectrum visualizer
 *
 * Quality levels (set via Settings → Graphics → Spectrum visualizer):
 *  - "full" : 3 rounds × 120 bars, FFT every 50 ms  — full visual fidelity.
 *  - "low"  : 1 round  × 120 bars, FFT every 100 ms — ~66 % less geometry, half the FFT reads.
 *  - "off"  : all bars zeroed, VBO flushed once then GPU upload skipped every frame.
 *
 * Every active frame:
 *   1. CPU fills vertex data for activeRounds × 120 bars (degenerate for the rest).
 *   2. VBO uploaded once.
 *   3. Single glDrawArrays renders all bars.
 */
class Spectrum : UIBufferedComponent<Spectrum.SpectrumVBO>() {

    private val frequencyAmplitudes = FloatArray(BAR_COUNT)
    private val temporalAmplitudes = FloatArray(BAR_COUNT)
    private var indexOffset = 0
    private var timeSinceLastAmpUpdate = 0f

    private var barStartX = FloatArray(0)
    private var barStartY = FloatArray(0)
    private var barCosA = FloatArray(0)
    private var barSinA = FloatArray(0)
    private var halfBarH = 0f
    private var maxBarLen = 0f
    private var totalBars = 0

    private var activeRounds = VISUALISER_ROUNDS
    private var updateInterval = TIME_BETWEEN_UPDATES_FULL

    // Set to true for one frame when quality transitions to "off" so the VBO is
    // flushed to degenerate triangles before the dead-zone early-return takes over.
    private var pendingClear = false

    // Tracks whether any bar was above the dead-zone on the previous frame so we
    // can flush the VBO once when the spectrum transitions from visible → silent.
    private var wasVisible = false

    fun configure(logoCX: Float, logoCY: Float, logoRadius: Float) {
        maxBarLen = BAR_LENGTH * logoRadius
        halfBarH  = (logoRadius * 2f *
                sqrt(2.0 * (1.0 - cos(2.0 * PI / BAR_COUNT)))).toFloat() / 4f

        totalBars = BAR_COUNT * VISUALISER_ROUNDS
        barStartX = FloatArray(totalBars)
        barStartY = FloatArray(totalBars)
        barCosA = FloatArray(totalBars)
        barSinA = FloatArray(totalBars)

        for (k in 0 until VISUALISER_ROUNDS) {
            val roundOffset = k * 360.0 / VISUALISER_ROUNDS
            for (i in 0 until BAR_COUNT) {
                val idx = k * BAR_COUNT + i
                val angleRad = Math.toRadians(i.toDouble() / BAR_COUNT * 360.0 + roundOffset)
                val ca = cos(angleRad).toFloat()
                val sa = sin(angleRad).toFloat()
                val startR = logoRadius * BAR_START_INSET

                barStartX[idx] = logoCX + startR * ca
                barStartY[idx] = logoCY + startR * sa
                barCosA[idx] = ca
                barSinA[idx] = sa
            }
        }

        x = 0f
        y = 0f
        width = logoRadius * 2f + MAX_EXTRA * 2f
        height = logoRadius * 2f + MAX_EXTRA * 2f
        blendInfo = BlendInfo.Additive
        alpha = BAR_ALPHA

        requestNewBuffer()
    }

    override fun onCreateBuffer(): SpectrumVBO? {
        if (totalBars <= 0) return null
        return SpectrumVBO(totalBars).also { it.sharingMode = BufferSharingMode.Dynamic }
    }

    override fun onUpdateBuffer() {
        val vbo = buffer ?: return

        // One-shot VBO flush when transitioning to "off": write all degenerate
        // triangles so the GPU stops rendering the last visible frame.
        if (pendingClear) {
            for (total in barStartX.indices) {
                val sx = barStartX[total]; val sy = barStartY[total]
                for (j in 0..5) vbo.putVertex(total * 6 + j, sx, sy)
            }
            vbo.invalidateOnHardware()
            pendingClear = false
            wasVisible = false
            return
        }

        val anyVisible = frequencyAmplitudes.any { it >= AMPLITUDE_DEAD_ZONE }

        // One-shot VBO flush when transitioning from visible → all-below-dead-zone
        // (e.g. music paused/silenced during full/low mode). Without this the GPU
        // keeps rendering the last non-degenerate frame indefinitely.
        if (!anyVisible) {
            if (wasVisible) {
                for (total in barStartX.indices) {
                    val sx = barStartX[total]; val sy = barStartY[total]
                    for (j in 0..5) vbo.putVertex(total * 6 + j, sx, sy)
                }
                vbo.invalidateOnHardware()
                wasVisible = false
            }
            return
        }

        wasVisible = true

        val hh = halfBarH
        val ml = maxBarLen
        val activeBars = BAR_COUNT * activeRounds

        for (total in barStartX.indices) {
            val vBase = total * 6
            val sx = barStartX[total]
            val sy = barStartY[total]

            if (total >= activeBars) {
                // Rounds beyond activeRounds → degenerate (invisible, zero rasterization cost).
                for (j in 0..5) vbo.putVertex(vBase + j, sx, sy)
                continue
            }

            val amp = frequencyAmplitudes[total % BAR_COUNT]
            if (amp < AMPLITUDE_DEAD_ZONE) {
                for (j in 0..5) vbo.putVertex(vBase + j, sx, sy)
            } else {
                val len = amp * ml
                val ca = barCosA[total]
                val sa = barSinA[total]

                val ax = sx - sa * hh
                val ay = sy + ca * hh

                val bx = sx + sa * hh
                val by = sy - ca * hh

                val cx = sx + ca * len + sa * hh
                val cy = sy + sa * len - ca * hh

                val dx = sx + ca * len - sa * hh
                val dy = sy + sa * len + ca * hh

                vbo.putVertex(vBase + 0, ax, ay)
                vbo.putVertex(vBase + 1, bx, by)
                vbo.putVertex(vBase + 2, cx, cy)
                vbo.putVertex(vBase + 3, ax, ay)
                vbo.putVertex(vBase + 4, cx, cy)
                vbo.putVertex(vBase + 5, dx, dy)
            }
        }

        vbo.invalidateOnHardware()
    }

    fun update(dt: Float, kiaiActive: Boolean) {
        when (Config.getSpectrumQuality()) {
            "off" -> {
                if (frequencyAmplitudes.any { it >= AMPLITUDE_DEAD_ZONE }) {
                    // First frame after turning off: zero amplitudes and schedule a VBO flush.
                    frequencyAmplitudes.fill(0f)
                    pendingClear = true
                }
                return
            }
            "low" -> {
                activeRounds = VISUALISER_ROUNDS_LOW
                updateInterval = TIME_BETWEEN_UPDATES_LOW
            }
            else  -> { // "full"
                activeRounds = VISUALISER_ROUNDS
                updateInterval = TIME_BETWEEN_UPDATES_FULL
            }
        }

        // Non-linear decay
        val decayFactor = dt * 1000f * DECAY_PER_MILLISECOND
        for (i in 0 until BAR_COUNT) {
            frequencyAmplitudes[i] -= decayFactor * (frequencyAmplitudes[i] + 0.03f)
            if (frequencyAmplitudes[i] < 0f) frequencyAmplitudes[i] = 0f
        }

        // Throttled FFT read
        timeSinceLastAmpUpdate += dt
        if (timeSinceLastAmpUpdate >= updateInterval) {
            timeSinceLastAmpUpdate -= updateInterval
            updateAmplitudes(kiaiActive)
        }
    }

    fun onBeat(@Suppress("UNUSED_PARAMETER") kiaiActive: Boolean) {
        indexOffset = (indexOffset + BEAT_INDEX_JUMP) % BAR_COUNT
    }

    private fun updateAmplitudes(kiaiActive: Boolean) {
        val fft = GlobalManager.getInstance().songService?.spectrum
        for (i in 0 until BAR_COUNT) temporalAmplitudes[i] = 0f

        if (fft != null) {
            val windowSize = 240
            var leftBound  = 0

            for (i in 0 until BAR_COUNT) {
                var peak = 0f
                var rightBound = 2.0.pow(i * 9.0 / (windowSize - 1)).toInt()

                if (rightBound <= leftBound)
                    rightBound = leftBound + 1

                if (rightBound > 511)
                    rightBound = 511

                while (leftBound < rightBound) {
                    if (leftBound + 1 < fft.size && fft[1 + leftBound] > peak)
                        peak = fft[1 + leftBound]
                    leftBound++
                }
                temporalAmplitudes[i] = (peak * FFT_SCALE).coerceAtMost(1f)
            }
        }

        val kiaiMul = if (kiaiActive) 1f else 0.7f
        for (i in 0 until BAR_COUNT) {
            val target = temporalAmplitudes[(i + indexOffset) % BAR_COUNT] * kiaiMul
            if (target > frequencyAmplitudes[i]) frequencyAmplitudes[i] = target
        }

        indexOffset = (indexOffset + INDEX_CHANGE) % BAR_COUNT
    }

    class SpectrumVBO(barCount: Int) : VertexBuffer(
        drawTopology = GL_TRIANGLES,
        vertexCount = barCount * 6,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_DYNAMIC_DRAW
    )

    companion object {
        const val BAR_COUNT = 120
        const val VISUALISER_ROUNDS = 3
        const val MAX_EXTRA = 300f

        private const val VISUALISER_ROUNDS_LOW = 1
        private const val BAR_START_INSET = 0.88f
        private const val BAR_LENGTH = 2.5f
        private const val BAR_ALPHA = 0.15f
        private const val AMPLITUDE_DEAD_ZONE = 1f / 600f
        private const val DECAY_PER_MILLISECOND = 0.0024f
        private const val TIME_BETWEEN_UPDATES_FULL = 0.05f   // 20 Hz
        private const val TIME_BETWEEN_UPDATES_LOW = 0.10f   // 10 Hz
        private const val FFT_SCALE = 3f
        private const val INDEX_CHANGE = 5
        private const val BEAT_INDEX_JUMP = 10
    }
}