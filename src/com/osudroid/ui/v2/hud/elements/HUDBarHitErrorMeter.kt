package com.osudroid.ui.v2.hud.elements

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.UIBox
import com.reco1l.framework.Color4
import com.osudroid.utils.*
import com.reco1l.andengine.component.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import javax.microedition.khronos.opengles.*
import kotlin.math.abs

class HUDBarHitErrorMeter : HUDHitErrorMeter() {

    private val expiredIndicators = SynchronizedPool<Indicator>(20)
    private val activeIndicators = mutableListOf<Indicator>()

    // Using a shared box for drawing indicators to reduce memory usage.
    private val indicatorBox = UIBox().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        setSize(INDICATOR_WIDTH, INDICATOR_HEIGHT - 2f)

        // Used to get anchor working properly.
        parent = this@HUDBarHitErrorMeter
    }


    init {
        setSize(WIDTH, INDICATOR_HEIGHT)

        val mehWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            color = mehColor
            cornerRadius = BAR_HEIGHT / 2
            setSize(WIDTH, BAR_HEIGHT)

            depthInfo = DepthInfo.Default
        }

        val okWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            color = okColor
            setSize(WIDTH * (hitWindow.okWindow / hitWindow.mehWindow).toFloat(), BAR_HEIGHT)

            depthInfo = DepthInfo.Default
        }

        val greatWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(WIDTH * (hitWindow.greatWindow / hitWindow.mehWindow).toFloat(), BAR_HEIGHT)
            color = greatColor

            clearInfo = ClearInfo.ClearDepthBuffer
            depthInfo = DepthInfo.Less
        }

        attachChild(mehWindow)
        attachChild(okWindow, 0)
        attachChild(greatWindow, 0)

        // Indicator
        attachChild(UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(INDICATOR_WIDTH / 2, INDICATOR_HEIGHT)
        })

        mehWindow.alpha = 0.6f
        okWindow.alpha = 0.6f
        greatWindow.alpha = 0.6f
    }

    override fun addResult(accuracy: Float, color: Color4) {
        val accuracyMs = accuracy * 1000

        if (abs(accuracyMs) > hitWindow.mehWindow) {
            return
        }

        val indicator = expiredIndicators.acquire() ?: Indicator(0f, 0f, Color4.White)

        indicator.x = (WIDTH / 2f) * (accuracyMs / hitWindow.mehWindow.toFloat())
        indicator.alpha = 1f
        indicator.color = color

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

    override fun onManagedUpdate(deltaTimeSec: Float) {
        activeIndicators.fastForEach(Indicator::update)
        super.onManagedUpdate(deltaTimeSec)
    }

    //endregion


    private inner class Indicator(var x: Float, var alpha: Float, var color: Color4) : IPoolable {
        override var isRecycled = false

        fun update() {
            if (alpha > 0f) {
                alpha -= 0.005f
            }

            if (alpha <= 0f) {
                alpha = 0f
                expiredIndicators.release(this)
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