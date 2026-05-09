package com.osudroid.ui.v2.hud.elements

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.shape.UIBox
import com.reco1l.framework.Color4
import com.reco1l.toolkt.kotlin.fastForEach
import com.osudroid.beatmaps.constants.HitObjectType
import javax.microedition.khronos.opengles.GL10
import org.anddev.andengine.engine.camera.Camera

class HUDColorHitErrorMeter : HUDHitErrorMeter() {
    private val indicators = MutableList(20) { Indicator(it, 0f, Color4.Black) }
    private var currentIndicatorIndex = 0

    private val indicatorBox = UIBox().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        setSize(INDICATOR_SIZE, INDICATOR_SIZE)

        // Used to get anchor working properly.
        parent = this@HUDColorHitErrorMeter
    }

    init {
        // Extra 2 to indicator spacing for left and right padding.
        val width = INDICATOR_SIZE * indicators.size + INDICATOR_SPACING * (indicators.size + 2)

        setSize(width, INDICATOR_SIZE)

        background = UIBox().apply {
            applyTheme = {
                color = Color4.Black
                alpha = 0.6f
            }
        }
    }

    override fun addResult(type: HitObjectType, accuracy: Float, color: Color4) {
        indicators.fastForEach { it.alpha = (it.alpha - ALPHA_RAMP).coerceAtLeast(0f) }

        val indicator = indicators[currentIndicatorIndex]

        indicator.alpha = 1f
        indicator.color = color

        currentIndicatorIndex = (currentIndicatorIndex + 1) % indicators.size
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        super.onDrawChildren(gl, camera)

        indicators.fastForEach {
            indicatorBox.x = INDICATOR_SPACING + it.index * (INDICATOR_SIZE + INDICATOR_SPACING)
            indicatorBox.color = it.color
            indicatorBox.alpha = it.alpha
            indicatorBox.onDraw(gl, camera)
        }
    }

    private data class Indicator(val index: Int, var alpha: Float, var color: Color4)

    companion object {
        private const val INDICATOR_SIZE = 30f
        private const val INDICATOR_SPACING = 3f
        private const val ALPHA_RAMP = 0.08f
    }
}