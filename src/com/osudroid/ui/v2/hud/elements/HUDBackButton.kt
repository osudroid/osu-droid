package com.osudroid.ui.v2.hud.elements

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.*
import com.reco1l.andengine.shape.UICircle
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.framework.Color4
import com.reco1l.framework.Interpolation
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.andengine.component.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.pct
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

class HUDBackButton : HUDElement() {

    override val shouldBeShown = true

    private val requiredPressTimeMs = Config.getBackButtonPressTime().toFloat()

    private val arrow = UISprite().apply {
        textureRegion = ResourceManager.getInstance().getTexture("back-arrow")
        anchor = Anchor.Center
        origin = Anchor.Center
        width = 0.6f.pct
        height = 0.6f.pct
    }

    private val backCircle = UICircle().apply {
        setPortion(0f)
        anchor = Anchor.Center
        origin = Anchor.Center
        color = Color4.White
        depthInfo = DepthInfo.Default

        width = Size.Full
        height = Size.Full
    }

    private val frontCircle = UICircle().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        color = Color4(0xFF002626)
        clearInfo = ClearInfo.ClearDepthBuffer
        depthInfo = DepthInfo.Less

        width = 0.95f.pct
        height = 0.95f.pct
    }


    private var holdDurationMs = 0f
        set(value) {
            field = value.coerceIn(0f, requiredPressTimeMs)

            // The progress is intentionally kept at 0 for 0 activation time to keep the button in its initial state.
            val progress = if (requiredPressTimeMs > 0) field / requiredPressTimeMs else 0f
            val scale = 1f + progress / 2f

            alpha = Interpolation.floatAt(holdDurationMs, 0.25f, 0.5f, 0f, requiredPressTimeMs, Easing.OutCubic)
            backCircle.setPortion(progress)
            backCircle.setScale(scale)
            frontCircle.setScale(scale)
            arrow.setScale(scale)
        }


    private var isPressed = false


    init {
        setSize(SIZE, SIZE)
        alpha = 0.25f

        attachChild(frontCircle)
        attachChild(backCircle)
        attachChild(arrow)
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (!isInEditMode) {
            val realMsElapsed = deltaTimeSec * 1000

            if (isPressed) {
                holdDurationMs += realMsElapsed

                if (holdDurationMs >= requiredPressTimeMs) {
                    isPressed = false
                    GlobalManager.getInstance().gameScene.pause()
                    (parent as UIComponent).invalidate(InvalidationFlag.InputBindings)
                }
            } else {
                holdDurationMs -= realMsElapsed * 2
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (isInEditMode) {
            return super.onAreaTouched(event, localX, localY)
        }

        if (event.isActionDown) {
            isPressed = true
            return true
        }

        if (event.isActionMove) {
            if (isPressed) {
                if (localX <= 0f || localY <= 0f || localX >= width || localY >= height) {
                    isPressed = false
                }
                return true
            }
        }

        if (event.isActionUp) {
            if (isPressed) {
                isPressed = false
                return true
            }
        }

        return false
    }


    companion object {
        private const val SIZE = 72f
    }
}