package com.osudroid.ui.v2.mainmenu

import com.osudroid.RythimManager
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.UICircle
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.rem
import com.reco1l.toolkt.kotlin.fastForEachIndexed
import org.anddev.andengine.engine.camera.Camera
import java.lang.ref.WeakReference
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

class RippleVisualizer : UIComponent() {

    /**
     * The thickness of each ripple.
     */
    var rippleThickness = 2f // 2 pixels
        set(value) {
            if (field != value) {
                field = value
                ripple.lineWidth = rippleThickness
            }
        }


    private val ripple = UICircle()
    private val referenceToThis = WeakReference(this)

    private var activeRipples = mutableListOf<RippleInfo>()


    init {
        attachChild(ripple.apply {
            color = Colors.White
            anchor = Anchor.Center
            origin = Anchor.Center
            paintStyle = PaintStyle.Outline
            lineWidth = rippleThickness
        })

        RythimManager.addOnBeatChangeListener(referenceToThis) {
            if (RythimManager.isKiai) {
                spawnRipple()
            } else if (RythimManager.beatIndex == 0) {
                spawnRipple()
            }
        }
    }


    private fun spawnRipple() {
        activeRipples.add(RippleInfo(0f, 1f))
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        activeRipples.fastForEachIndexed { index, info ->
            info.update(deltaTimeSec)
        }
        activeRipples.removeAll { it.alpha <= 0f }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        val baseSize = max(width, height)

        activeRipples.fastForEachIndexed { index, info ->
            val size = baseSize + (baseSize / 3) * info.size
            ripple.width = size
            ripple.height = size
            ripple.alpha = info.alpha
            ripple.onDraw(gl, camera)
        }
    }

    fun finalize() {
        RythimManager.removeOnBeatChangeListener(referenceToThis)
    }


    data class RippleInfo(
        var size: Float,
        var alpha: Float
    ) {

        var velocity: Float = 0f


        fun update(deltaTimeSec: Float) {
            val fadeRate = if (RythimManager.isKiai) 0.5f else 0.25f
            alpha -= fadeRate * deltaTimeSec

            // The velocity is proportional to the alpha value
            velocity = max(0f, alpha * (if (RythimManager.isKiai) 3f else 1.5f))

            size += velocity * deltaTimeSec
        }
    }

}