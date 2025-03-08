package com.reco1l.osu.hud.elements

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.Box
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.Pool
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.updateThread
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import ru.nsu.ccfit.zuev.osu.GlobalManager
import javax.microedition.khronos.opengles.*
import kotlin.math.abs

class HUDHitErrorMeter : HUDElement() {

    private val expiredIndicators = Pool(20) { Indicator(0f, 0f, ColorARGB.White) }
    private val activeIndicators = mutableListOf<Indicator>()

    private val hitWindow = GlobalManager.getInstance().gameScene.hitWindow

    private val greatColor = ColorARGB(70, 180, 220)
    private val okColor = ColorARGB(100, 220, 40)
    private val mehColor = ColorARGB(200, 180, 110)


    // Using a shared box for drawing indicators to reduce memory usage.
    private val indicatorBox = Box().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        setSize(INDICATOR_WIDTH, INDICATOR_HEIGHT - 2f)

        // Used to get anchor working properly.
        parent = this@HUDHitErrorMeter
    }


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
        indicator.alpha = 1f
        indicator.color = when {
            abs(accuracyMs) <= hitWindow.greatWindow -> greatColor
            abs(accuracyMs) <= hitWindow.okWindow -> okColor
            else -> mehColor
        }

        activeIndicators.add(indicator)
    }


    //region Indicator update & draw

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        super.onDrawChildren(gl, camera)
        activeIndicators.fastForEach {
            indicatorBox.x = it.x
            indicatorBox.color = it.color
            indicatorBox.alpha = it.alpha
            indicatorBox.onDraw(gl, camera)
        }
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        activeIndicators.fastForEach(Indicator::update)
        super.onManagedUpdate(pSecondsElapsed)
    }

    //endregion


    private inner class Indicator(var x: Float, var alpha: Float, var color: ColorARGB) {

        fun update() {
            if (alpha > 0f) {
                alpha -= 0.005f
            }

            if (alpha <= 0f) {
                alpha = 0f
                expiredIndicators.free(this)
                updateThread {
                    activeIndicators.remove(this)
                }
            }
        }
    }


    companion object {

        private const val WIDTH = 400f
        private const val BAR_HEIGHT = 10f
        private const val INDICATOR_HEIGHT = BAR_HEIGHT + 14f
        private const val INDICATOR_WIDTH = 4f

    }
}