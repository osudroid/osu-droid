package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.info.*
import com.reco1l.framework.*

open class CircularProgressBar : Container() {

    override var applyTheme: ExtendedEntity.(Theme) -> Unit = { theme ->
        trackCircle.color = theme.accentColor.copy(alpha = trackCircle.alpha)
        rotatingCircle.color = theme.accentColor
    }


    /**
     * The progress of the circular progress bar, from 0 to 1. If set to -1, the progress bar will
     * be treated as indeterminate.
     */
    var progress = -1f
        set(value) {
            if (field != value) {
                field = if (value == -1f) value else value.coerceIn(0f, 1f)
                onProgressChange()
            }
        }


    private val maskingCircle = circle {
        anchor = Anchor.Center
        origin = Anchor.Center
        relativeSizeAxes = Axes.Both
        width = 0.9f
        height = 0.9f
        color = ColorARGB.Transparent
        clearInfo = ClearInfo.ClearDepthBuffer
        depthInfo = DepthInfo.Less
    }

    private val trackCircle = circle {
        width = FillParent
        height = FillParent
        color = Theme.current.accentColor
        alpha = 0.3f
        depthInfo = DepthInfo.Default
    }

    private val rotatingCircle = circle {
        width = FillParent
        height = FillParent
        rotationCenter = Anchor.Center
        depthInfo = DepthInfo.Default
        setPortion(0.25f)
        color = Theme.current.accentColor
    }


    protected open fun onProgressChange() {
        if (progress < 0f) {
            rotatingCircle.setPortion(0.25f)
        } else {
            rotatingCircle.setPortion(360f * progress)
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        if (progress < 0f) {
            rotatingCircle.rotation += 360f * deltaTimeSec
        }
        super.onManagedUpdate(deltaTimeSec)
    }

}