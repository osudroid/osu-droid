package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.info.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*

data class CircularProgressBarTheme(
    val trackColor: ColorARGB = ColorARGB(0x4DFFFFFF),
    val progressColor: ColorARGB = ColorARGB(0xFFFFFFFF),
) : ITheme

@Suppress("JoinDeclarationAndAssignment")
open class CircularProgressBar : Container(), IWithTheme<CircularProgressBarTheme> {

    override var theme = DefaultTheme
        set(value) {
            if (field != value) {
                field = value
                onThemeChanged()
            }
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


    private val rotatingCircle: Circle
    private val maskingCircle: Circle
    private val trackCircle: Circle


    init {
        rotatingCircle = circle {
            width = FillParent
            height = FillParent
            rotationCenter = Anchor.Center
            depthInfo = DepthInfo.Default
            setPortion(0.25f)
        }

        trackCircle = circle {
            width = FillParent
            height = FillParent
            alpha = 0.3f
            depthInfo = DepthInfo.Default
        }

        maskingCircle = circle {
            anchor = Anchor.Center
            origin = Anchor.Center
            relativeSizeAxes = Axes.Both
            width = 0.9f
            height = 0.9f
            color = ColorARGB.Transparent
            clearInfo = ClearInfo.ClearDepthBuffer
            depthInfo = DepthInfo.Less
        }
    }


    open fun onProgressChange() {
        if (progress < 0f) {
            rotatingCircle.setPortion(0.25f)
        } else {
            rotatingCircle.setPortion(360f * progress)
        }
    }


    override fun onThemeChanged() {
        trackCircle.color = theme.trackColor
        rotatingCircle.color = theme.progressColor
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        if (progress < 0f) {
            rotatingCircle.rotation += 360f * deltaTimeSec
        }
        super.onManagedUpdate(deltaTimeSec)
    }


    companion object {
        val DefaultTheme = CircularProgressBarTheme()
    }
}