package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.pct

open class CircularProgressBar : UIContainer() {

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
        width = Size.Full
        height = Size.Full
        paintStyle = PaintStyle.Outline
        lineWidth = 4f
        style = {
            color = Theme.current.accentColor
            alpha = 0.3f
        }
    }

    private val rotatingCircle = circle {
        width = 0.85f.pct
        height = 0.85f.pct
        anchor = Anchor.Center
        origin = Anchor.Center
        rotationCenter = Anchor.Center
        setPortion(0.1f)
        style = {
            color = Theme.current.accentColor
            alpha = 0.3f
        }
    }


    init {
        style = {
            trackCircle.color = it.accentColor / trackCircle.alpha
            rotatingCircle.color = it.accentColor
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