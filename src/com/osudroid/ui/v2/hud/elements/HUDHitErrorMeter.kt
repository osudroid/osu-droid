package com.osudroid.ui.v2.hud.elements

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.UIBox
import com.reco1l.framework.Color4
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.utils.*
import com.reco1l.toolkt.kotlin.*
import org.andengine.engine.camera.*
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.GlobalManager
import kotlin.math.abs

class HUDHitErrorMeter : HUDElement() {

    private val expiredIndicators = SynchronizedPool<Indicator>(20)
    private val activeIndicators = mutableListOf<Indicator>()

    private val hitWindow = GlobalManager.getInstance().gameScene.hitWindow

    private val greatColor = Color4(70, 180, 220)
    private val okColor = Color4(100, 220, 40)
    private val mehColor = Color4(200, 180, 110)


    // Using a shared box for drawing indicators to reduce memory usage.
    private val indicatorBox = UIBox().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        setSize(INDICATOR_WIDTH, INDICATOR_HEIGHT - 2f)

        // Used to get anchor working properly.
        parent = this@HUDHitErrorMeter
    }


    init {
        setSize(WIDTH, INDICATOR_HEIGHT)

        val mehWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            color = mehColor
            cornerRadius = BAR_HEIGHT / 2
            setSize(WIDTH, BAR_HEIGHT)
        }

        val okWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            color = okColor
            setSize(WIDTH * (hitWindow.okWindow / hitWindow.mehWindow).toFloat(), BAR_HEIGHT)
        }

        val greatWindow = UIBox().apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            setSize(WIDTH * (hitWindow.greatWindow / hitWindow.mehWindow).toFloat(), BAR_HEIGHT)
            color = greatColor
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


    override fun onAccuracyRegister(accuracy: Float) {

        val accuracyMs = accuracy * 1000

        if (abs(accuracyMs) > hitWindow.mehWindow) {
            return
        }

        val indicator = expiredIndicators.acquire() ?: Indicator(0f, 0f, Color4.White)

        indicator.x = (WIDTH / 2f) * (accuracyMs / hitWindow.mehWindow.toFloat())
        indicator.alpha = 1f
        indicator.color = when {
            abs(accuracyMs) <= hitWindow.greatWindow -> greatColor
            abs(accuracyMs) <= hitWindow.okWindow -> okColor
            else -> mehColor
        }

        activeIndicators.add(indicator)
    }


    //region Indicator update & draw

    override fun onManagedDraw(pGLState: GLState, pCamera: Camera) {
        super.onManagedDraw(pGLState, pCamera)

        if (activeIndicators.isEmpty()) return

        // After super.onManagedDraw() the HUDHitErrorMeter matrix has been popped, so we
        // are now in the parent (GameplayHUD) coordinate space.  We must re-push our own
        // translation so that indicatorBox.absoluteX/Y (which are relative to this element)
        // are mapped back to the correct screen position.
        pGLState.pushModelViewGLMatrix()
        pGLState.translateModelViewGLMatrixf(absoluteX, absoluteY, 0f)

        activeIndicators.fastForEach {
            indicatorBox.x = it.x
            indicatorBox.color = it.color
            indicatorBox.alpha = it.alpha
            indicatorBox.onDraw(pGLState, pCamera)
        }

        pGLState.popModelViewGLMatrix()
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