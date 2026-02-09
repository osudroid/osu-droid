package com.osudroid.ui.v2.mainmenu

import com.edlplan.framework.easing.Easing
import com.osudroid.RythimManager
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.buffered.BufferSharingMode
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.Theme
import com.reco1l.framework.Interpolation
import com.reco1l.toolkt.MathF
import com.reco1l.toolkt.kotlin.fastForEachIndexed
import org.anddev.andengine.engine.camera.Camera
import ru.nsu.ccfit.zuev.osu.GlobalManager
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.min

class RadialVisualizer : UIComponent() {

    /**
     * The thickness of each bar.
     */
    var barThickness = 0.25f.rem
        set(value) {
            if (field != value) {
                field = value
                barBox.width = barThickness
                initializeBars()
            }
        }

    /**
     * The gap between each bar (angular gap in radians for radial mode).
     */
    var gap = 0.15f.srem

    /**
     * The multiplier applied to the FFT magnitude values.
     */
    var magnitudeMultiplier = 2f


    private val barBox = UIBox()
    private val songService = GlobalManager.getInstance().songService

    private var bars = mutableListOf<BarInfo>()
    private var visualizerRadius = 0f
    private var shiftOffset = 0f


    init {
        attachChild(barBox.apply {
            width = barThickness
            color = Colors.White
            anchor = Anchor.Center
            origin = Anchor.BottomCenter
            bufferSharingMode = BufferSharingMode.Dynamic
        })
    }


    private fun initializeBars() {
        visualizerRadius = min(width, height) / 2f
        
        // Circumference = 2 * PI * radius
        val circumference = 2f * MathF.PI * visualizerRadius
        val barCount = (circumference / (barThickness + gap)).toInt()
        
        bars = MutableList(barCount) { i ->
            // Preserve existing bar heights if possible
            if (i < bars.size) bars[i] else BarInfo()
        }
    }


    override fun onSizeChanged() {
        super.onSizeChanged()
        initializeBars()
    }

    override fun onStyle(theme: Theme) {
        super.onStyle(theme)
        initializeBars()
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        val fft = songService.getAudioFFT(bars.size / 6)

        // Synchronize rotation speed with the beat
        val beatLength = RythimManager.beatLength
        val rotationsPerBeat = if (RythimManager.isKiai) 0.25f else 0.1f

        // Calculate rotation speed based on BPM
        // rotationsPerSecond = rotationsPerBeat * beatsPerSecond
        val rotationsPerSecond = if (beatLength > 0) {
            rotationsPerBeat * (1000f / beatLength.toFloat())
        } else {
            0f
        }

        val fftSize = fft?.size ?: 1

        // Accumulate rotation over time (doesn't reset each beat)
        shiftOffset += rotationsPerSecond * fftSize * deltaTimeSec

        // Keep offset in reasonable range
        if (shiftOffset >= fftSize) {
            shiftOffset %= fftSize.toFloat()
        }

        bars.fastForEachIndexed { index, barInfo ->
            val exactIndex = (shiftOffset + index) % fftSize
            val fftIndex1 = exactIndex.toInt()
            val fftIndex2 = (fftIndex1 + 1) % fftSize
            val fraction = exactIndex - fftIndex1

            // Interpolate between two consecutive FFT values for smooth transition
            val value1 = fft?.getOrNull(fftIndex1) ?: 0f
            val value2 = fft?.getOrNull(fftIndex2) ?: 0f
            val interpolatedValue = value1 * (1f - fraction) + value2 * fraction

            barInfo.targetHeight = interpolatedValue * magnitudeMultiplier
            barInfo.update(deltaTimeSec)
        }
        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        val bars = bars

        val baseBarHeight = visualizerRadius

        bars.fastForEachIndexed { index, barInfo ->

            val angle = (index.toFloat() / bars.size) * 2f * PI.toFloat()

            barBox.width = barThickness
            barBox.height = baseBarHeight
            barBox.scaleY = 1f + 2f * barInfo.currentHeight.coerceIn(0f, 1f)
            barBox.rotation = Math.toDegrees(angle.toDouble()).toFloat()

            barBox.onDraw(gl, camera)
        }
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {
        super.onManagedDraw(gl, camera)
    }


    data class BarInfo(
        // Heights are in relative units (0 to 1)
        var targetHeight: Float = 0f,
        var currentHeight: Float = 0f
    ) {
        fun update(deltaTimeSec: Float) {
            currentHeight = Interpolation.floatAt(
                deltaTimeSec.coerceIn(0f, 0.3f),
                currentHeight,
                targetHeight,
                0f,
                0.3f,
                Easing.OutQuint
            )
        }
    }

}