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

    override var applyTheme: ExtendedEntity.(Theme) -> Unit = { theme ->

        background?.apply {
            color = theme.accentColor * 0.25f
            foreground?.color = theme.accentColor * 0.4f
        }

        progressBar.apply {
            color = theme.accentColor * 0.5f
            foreground?.color = theme.accentColor
        }

        thumb.color = theme.accentColor
    }


    /**
     * The minimum allowed value of the slider.
     */
    var min = 0f
        set(min) {
            if (field != min) {
                field = min(min, max)
                value = onProcessValue(value)
            }
        }

    /**
     * The maximum allowed value of the slider.
     */
    var max = 1f
        set(max) {
            if (field != max) {
                field = max(min, max)
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


    private val thumb = Box().apply {
        width = 24f
        height = FillParent
        anchor = Anchor.CenterLeft
        origin = Anchor.Center
        cornerRadius = 12f
        inheritAncestorsColor = false
        depthInfo = DepthInfo.Less
        clearInfo = ClearInfo.ClearDepthBuffer
    }

    private val progressBar = Box().apply {
        width = 24f
        height = FillParent
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        cornerRadius = 12f
        foreground = Box().apply {
            paintStyle = PaintStyle.Outline
            cornerRadius = 12f
            depthInfo = DepthInfo.Default
        }
        depthInfo = DepthInfo.Default
    }


    init {
        width = FillParent
        height = 48f

        background = Box().apply {
            cornerRadius = 12f

            foreground = Box().apply {
                cornerRadius = 12f
                paintStyle = PaintStyle.Outline
            }
        }

        attachChild(thumb)
        attachChild(progressBar)
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
            return (min + step * round((value - min) / step)).coerceIn(min, max)
        }
        return value.coerceIn(min, max)
    }


    private fun updateProgress() {

        val absoluteProgress = (value - min) / (max - min)

        thumb.x = (width * absoluteProgress).coerceAtLeast(thumb.width / 2f).coerceAtMost(width - thumb.width / 2f)

        if (min >= 0f) {
            progressBar.anchor = Anchor.CenterLeft
            progressBar.origin = Anchor.CenterLeft
            progressBar.width = (width * absoluteProgress + thumb.width / 2f).coerceAtLeast(thumb.width).coerceAtMost(width)
            return
        }

        val barCenter = -min / (max - min)

        if (progressBar.anchor.x != barCenter) {
            progressBar.anchor = Vec2(barCenter, 0.5f)
        }

        val partitionWidth = width * if (value < min) barCenter else 1f - barCenter

        progressBar.width = partitionWidth * abs(value / max)

        val innerCenterX = if (value < 0f) 1f else 0f

        if (progressBar.origin.x != innerCenterX) {
            progressBar.origin = Vec2(innerCenterX, 0.5f)
        }

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