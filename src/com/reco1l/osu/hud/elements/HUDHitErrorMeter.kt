package com.reco1l.osu.hud.elements

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.Box
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.Pool
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.updateThread
import ru.nsu.ccfit.zuev.osu.GlobalManager
import kotlin.math.abs

class HUDHitErrorMeter : HUDElement() {


    private val expiredIndicators = Pool<Box>(20) { Indicator() }

    private val hitWindow = GlobalManager.getInstance().gameScene.hitWindow

    private val greatColor = ColorARGB(70, 180, 220)

    private val okColor = ColorARGB(100, 220, 40)

    private val mehColor = ColorARGB(200, 180, 110)


    init {
        setSize(WIDTH, INDICATOR_HEIGHT)

        val mehWindow = RoundedBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            color = mehColor
            cornerRadius = BAR_HEIGHT / 2
            setSize(WIDTH, BAR_HEIGHT)

            depthInfo = DepthInfo.Default
        }

        val okWindow = Box().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            color = okColor
            setSize(WIDTH * (hitWindow.okWindow / hitWindow.mehWindow), BAR_HEIGHT)

            depthInfo = DepthInfo.Default
        }

        val greatWindow = Box().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(WIDTH * (hitWindow.greatWindow / hitWindow.mehWindow), BAR_HEIGHT)
            color = greatColor

            depthInfo = DepthInfo.Clear
        }

        attachChild(mehWindow)
        attachChild(okWindow, 0)
        attachChild(greatWindow, 0)

        // Indicator
        attachChild(Box().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(INDICATOR_WIDTH / 2, INDICATOR_HEIGHT)
        })

        mehWindow.alpha = 0.6f
        okWindow.alpha = 0.6f
        greatWindow.alpha = 0.6f
    }


    override fun onAccuracyRegister(accuracy: Float) {

        val accuracyMs = accuracy * 1000

        if (abs(accuracyMs) > hitWindow.mehWindow) {
            return
        }

        val indicator = expiredIndicators.obtain()

        indicator.x = (WIDTH / 2f) * (accuracyMs / hitWindow.mehWindow)
        indicator.alpha = 0.6f

        indicator.color = when {
            abs(accuracyMs) <= hitWindow.greatWindow -> greatColor
            abs(accuracyMs) <= hitWindow.okWindow -> okColor
            else -> mehColor
        }

        if (indicator.parent == null) {
            attachChild(indicator)
        }
    }


    private inner class Indicator : Box() {

        init {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(INDICATOR_WIDTH, INDICATOR_HEIGHT - 2f)
        }

        override fun onManagedUpdate(pSecondsElapsed: Float) {

            if (alpha > 0f) {
                alpha -= 0.002f
            }

            if (alpha <= 0f) {
                alpha = 0f

                updateThread {
                    detachSelf()
                    expiredIndicators.free(this)
                }
            }
        }
    }


    companion object {

        private const val WIDTH = 400f
        private const val BAR_HEIGHT = 12f
        private const val INDICATOR_HEIGHT = BAR_HEIGHT + 14f
        private const val INDICATOR_WIDTH = 4f

    }
}