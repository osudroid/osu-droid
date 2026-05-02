package com.osudroid.ui.v2.hud.elements

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.UIBox
import com.reco1l.framework.Color4
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.beatmap.constants.HitObjectType
import org.andengine.engine.camera.Camera
import org.andengine.opengl.util.GLState

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

    override fun onManagedDraw(pGLState: GLState, pCamera: Camera) {
        super.onManagedDraw(pGLState, pCamera)

        // After super.onManagedDraw() the matrix has been popped; re-push our translation
        // so that indicatorBox draws in this element's local coordinate space.
        pGLState.pushModelViewGLMatrix()
        pGLState.translateModelViewGLMatrixf(absoluteX, absoluteY, 0f)

        indicators.fastForEach {
            indicatorBox.x = INDICATOR_SPACING + it.index * (INDICATOR_SIZE + INDICATOR_SPACING)
            indicatorBox.color = it.color
            indicatorBox.alpha = it.alpha
            indicatorBox.onDraw(pGLState, pCamera)
        }

        pGLState.popModelViewGLMatrix()
    }

    private data class Indicator(val index: Int, var alpha: Float, var color: Color4)

    companion object {
        private const val INDICATOR_SIZE = 30f
        private const val INDICATOR_SPACING = 3f
        private const val ALPHA_RAMP = 0.08f
    }
}