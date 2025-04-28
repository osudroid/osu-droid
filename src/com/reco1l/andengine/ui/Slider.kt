package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.info.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import kotlin.math.*

@Suppress("LeakingThis")
open class Slider(initialValue: Float = 0f) : Control<Float>(initialValue) {

    override var onThemeChange: ExtendedEntity.(Theme) -> Unit = { theme ->
        backgroundBar.color = theme.accentColor * 0.25f
        progressBar.color = theme.accentColor * 0.5f
        progressBar.foreground!!.color = theme.accentColor
        thumb.color = theme.accentColor
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
    var step = 0.0f
        set(step) {
            if (step < 0f) {
                throw IllegalArgumentException("step must be greater than or equal to 0")
            }
            field = step
        }

    /**
     * Called when the user starts dragging the slider.
     */
    var onStartDragging: () -> Unit = {}

    /**
     * Called when the user stops dragging the slider.
     */
    var onStopDragging: () -> Unit = {}


    private val backgroundBar = object : Box() {

        init {
            width = FillParent
            anchor = Anchor.Center
            origin = Anchor.Center
            height = 48f
            cornerRadius = 12f
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (event.isActionDown || event.isActionMove) {
                if (event.isActionDown) {
                    setHierarchyScrollPrevention(true)
                    onStartDragging()
                }
                value = (localX / width) * (max - min) + min
            } else {
                setHierarchyScrollPrevention(false)
                onStopDragging()
            }
            return true
        }
    }

    private val progressBar = Box().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        height = 48f
        cornerRadius = 12f

        foreground = Box().apply {
            paintStyle = PaintStyle.Outline
            cornerRadius = 12f
        }
    }

    private val thumb = Box().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterRight
        width = 14f
        height = 48f
        cornerRadius = 12f
    }


    init {
        width = FillParent

        attachChild(backgroundBar)
        attachChild(progressBar)
        attachChild(thumb)
    }


    override fun onSizeChanged() {
        super.onSizeChanged()
        updateProgress()
    }

    override fun onValueChanged() {
        super.onValueChanged()
        updateProgress()
    }

    override fun onProcessValue(value: Float): Float {
        if (step > 0f) {
            return (min + step * ceil((value - min) / step)).coerceIn(min, max)
        }
        return value.coerceIn(min, max)
    }


    private fun updateProgress() {

        val progress = (value - min) / (max - min)
        val progressWidth = width * progress

        thumb.x = progressWidth.coerceAtMost(width - thumb.width).coerceAtLeast(thumb.width)

        // The anchor will determine whether the progress bar should start.
        // The zero will be in the corresponding position of the slider relative to the min and max values.
        // If the min value is 0 or positive then the anchor will be 0 because there's no offset to apply
        // due to non-negative values.
        val anchor = if (min >= 0f) 0f else -min / (max - min)
        val origin = if (value >= 0f) 0f else 1f

        if (progressBar.anchor.x != anchor) {
            progressBar.anchor = Vec2(anchor, 0.5f)
        }

        if (progressBar.origin.x != origin) {
            progressBar.origin = Vec2(origin, 0.5f)
        }

        val leftSideWidth = if (anchor > 0f) width * anchor else 0f
        val leftSideProgressWidth = (leftSideWidth - progressWidth).coerceAtMost(leftSideWidth).coerceAtLeast(0f)

        val rightSideWidth = width - leftSideWidth
        val rightSideProgressWidth = (progressWidth - leftSideWidth).coerceAtMost(rightSideWidth).coerceAtLeast(0f)

        progressBar.width = (if (value >= 0f) rightSideProgressWidth else leftSideProgressWidth).coerceAtLeast(thumb.width)
    }

}