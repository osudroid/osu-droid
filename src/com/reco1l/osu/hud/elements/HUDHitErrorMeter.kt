package com.reco1l.osu.hud.elements

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.shape.Box
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.framework.Pool
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.updateThread
import ru.nsu.ccfit.zuev.osu.GlobalManager
import kotlin.math.abs

class HUDHitErrorMeter : HUDElement() {


    private val expiredIndicators = Pool<RoundedBox>(20) { Indicator() }

    private val hitWindow = GlobalManager.getInstance().gameScene.hitWindow


    init {
        setSize(WIDTH, INDICATOR_HEIGHT)

        // 50
        attachChild(RoundedBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            cornerRadius = BAR_HEIGHT / 2
            setSize(WIDTH, BAR_HEIGHT)
            setColor(200f / 255f, 180f / 255f, 110f / 255f)
        })

        // 100
        attachChild(Box().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(WIDTH * (hitWindow.okWindow / hitWindow.mehWindow), BAR_HEIGHT)
            setColor(100f / 255f, 220f / 255f, 40f / 255f)
        })

        // 300
        attachChild(Box().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(WIDTH * (hitWindow.greatWindow / hitWindow.mehWindow), BAR_HEIGHT)
            setColor(70f / 255f, 180f / 255f, 220f / 255f)
        })

        // Indicator
        attachChild(RoundedBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            cornerRadius = INDICATOR_WIDTH / 2
            setSize(INDICATOR_WIDTH, INDICATOR_HEIGHT)
        })
    }


    override fun onAccuracyRegister(accuracy: Float) {

        if (abs(accuracy * 1000) > hitWindow.mehWindow) {
            return
        }

        val indicator = expiredIndicators.obtain()

        indicator.x = (WIDTH / 2f) * (accuracy * 1000 / hitWindow.mehWindow)
        indicator.alpha = 0.6f

        if (indicator.parent == null) {
            attachChild(indicator)
        }
    }


    inner class Indicator : RoundedBox() {

        init {
            anchor = Anchor.Center
            origin = Anchor.Center
            cornerRadius = INDICATOR_WIDTH / 2
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