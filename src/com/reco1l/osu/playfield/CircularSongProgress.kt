package com.reco1l.osu.playfield

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*


class CircularSongProgress : Container() {


    override var autoSizeAxes = Axes.Both


    private val circularProgress: Circle


    init {
        // Reference: https://github.com/ppy/osu/blob/6455c0583b5e607baeca7f584410bc63515aa619/osu.Game/Skinning/LegacySongProgress.cs

        Circle().also { clear ->

            clear.setSize(30f, 30f)
            clear.setAnchor(Anchor.Center)
            clear.setOrigin(Anchor.Center)
            clear.color = ColorARGB.Transparent
            clear.testWithDepthBuffer = true
            clear.clearDepthBufferBeforeDraw = true

            attachChild(clear)
        }

        Circle().also { background ->

            background.setSize(33f, 33f)
            background.setAnchor(Anchor.Center)
            background.setOrigin(Anchor.Center)
            background.color = ColorARGB.White
            background.testWithDepthBuffer = true

            attachChild(background)
        }

        circularProgress = Circle().also { progress ->

            progress.setSize(30f, 30f)
            progress.setAnchor(Anchor.Center)
            progress.setOrigin(Anchor.Center)
            progress.alpha = 0.6f

            attachChild(progress)
        }


        Circle().also { dot ->

            dot.setSize(4f, 4f)
            dot.setAnchor(Anchor.Center)
            dot.setOrigin(Anchor.Center)
            dot.color = ColorARGB.White

            attachChild(dot)
        }

        onMeasureContentSize()

        setAnchor(Anchor.TopRight)
        setOrigin(Anchor.CenterRight)
    }


    fun setProgress(progress: Float, isIntro: Boolean) {

        if (isIntro) {
            circularProgress.setColor(199f / 255f, 1f, 47f / 255f)
            circularProgress.setPortion(-1f + progress)
        } else {
            circularProgress.setColor(1f, 1f, 1f)
            circularProgress.setPortion(progress)
        }

    }

}