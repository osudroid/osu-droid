package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.info.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import org.anddev.andengine.input.touch.*
import kotlin.math.*

data class SliderTheme(
    val backgroundColor: Long = 0xFF222234,
    val progressColor: Long = 0xFFF27272,
    val thumbColor: Long = 0xFFFFFFFF,
    val barHeight: Float = 12f,
    val thumbHeight: Float = 24f,
    val thumbWidth: Float = 32f,
) : ITheme

@Suppress("LeakingThis")
open class Slider(initialValue: Float = 0f) : Control<Float>(initialValue), IWithTheme<SliderTheme> {

    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
        }


    /**
     * The minimum allowed value of the slider.
     */
    var min = 0f
        set(min) {
            if (min > max) {
                throw IllegalArgumentException("min must be less than or equal to max")
            }

            if (field != min) {
                field = min
                value = onProcessValue(value)
            }
        }

    /**
     * The maximum allowed value of the slider.
     */
    var max = 1f
        set(max) {
            if (max < min) {
                throw IllegalArgumentException("max must be greater than or equal to min")
            }

            if (field != max) {
                field = max
                value = onProcessValue(value)
            }
        }

    /**
     * The step size of the slider. If set to 0, the slider will allow any value between min and max.
     */
    var step = 0f
        set(step) {
            if (step < 0f) {
                throw IllegalArgumentException("step must be greater than or equal to 0")
            }
            field = step
        }


    private val backgroundBar = object : RoundedBox() {

        init {
            width = FitParent
            anchor = Anchor.Center
            origin = Anchor.Center
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (event.isActionDown || event.isActionMove) {
                setHierarchyScrollPrevention(true)
                value = (localX / width) * (max - min) + min
            } else {
                setHierarchyScrollPrevention(false)
            }
            return true
        }
    }

    private val progressBar = RoundedBox().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        depthInfo = DepthInfo.Default
    }

    private val thumb = RoundedBox().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.Center
        depthInfo = DepthInfo.Less
        clearInfo = ClearInfo.ClearDepthBuffer
    }


    init {
        width = FitParent

        attachChild(backgroundBar)
        attachChild(thumb)
        attachChild(progressBar)

        onThemeChanged()
    }


    override fun onProcessValue(value: Float): Float {

        if (step > 0f) {
            return (min + step * ceil((value - min) / step)).coerceIn(min, max)
        }

        return value.coerceIn(min, max)
    }

    override fun onThemeChanged() {
        backgroundBar.color = ColorARGB(theme.backgroundColor)
        backgroundBar.height = theme.barHeight
        backgroundBar.cornerRadius = theme.barHeight / 2f

        progressBar.color = ColorARGB(theme.progressColor)
        progressBar.height = theme.barHeight
        progressBar.cornerRadius = theme.barHeight / 2f

        thumb.color = ColorARGB(theme.thumbColor)
        thumb.height = theme.thumbHeight
        thumb.width = theme.thumbWidth
        thumb.cornerRadius = min(theme.thumbHeight, thumb.width) / 2f
        thumb.origin = Anchor.Center
        thumb.foreground = BezelOutline(theme.thumbHeight / 2f)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        updateProgress()
    }

    override fun onValueChanged() {
        super.onValueChanged()
        updateProgress()
    }


    private fun updateProgress() {
        val progressWidth = width * (value - min) / (max - min)

        progressBar.width = progressWidth
        thumb.x = progressWidth.coerceAtMost(width - thumb.width / 2f).coerceAtLeast(thumb.width / 2f)
    }


    companion object {
        val DefaultTheme = SliderTheme()
    }

}