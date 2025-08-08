package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*

open class CircularProgressBar : UIContainer() {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
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

    private val trackCircle = circle {
        width = FillParent
        height = FillParent
        paintStyle = PaintStyle.Outline
        lineWidth = 4f
        applyTheme = {
            color = Theme.current.accentColor
            alpha = 0.3f
        }
    }

    private val rotatingCircle = circle {
        relativeSizeAxes = Axes.Both
        width = 0.85f
        height = 0.85f
        anchor = Anchor.Center
        origin = Anchor.Center
        rotationCenter = Anchor.Center
        setPortion(0.1f)
        applyTheme = {
            color = Theme.current.accentColor
            alpha = 0.3f
        }
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