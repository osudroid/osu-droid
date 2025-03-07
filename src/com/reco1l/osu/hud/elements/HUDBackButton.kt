package com.reco1l.osu.hud.elements

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.*
import com.reco1l.andengine.shape.Circle
import com.reco1l.andengine.sprite.ExtendedSprite
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.Interpolation
import com.reco1l.osu.hud.HUDElement
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

class HUDBackButton : HUDElement() {

    private val requiredPressTimeMs = Config.getInt("back_button_press_time", 300).toFloat()

    private val arrow = ExtendedSprite().apply {
        textureRegion = ResourceManager.getInstance().getTexture("back-arrow")
        anchor = Anchor.Center
        origin = Anchor.Center

        relativeSizeAxes = Axes.Both
        setSize(0.6f, 0.6f)
    }

    private val backCircle = Circle().apply {
        setPortion(0f)
        anchor = Anchor.Center
        origin = Anchor.Center
        color = ColorARGB.White
        depthInfo = DepthInfo.Default

        relativeSizeAxes = Axes.Both
        setSize(1f, 1f)
    }

    private val frontCircle = Circle().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        color = ColorARGB(0xFF002626)
        depthInfo = DepthInfo.Clear

        relativeSizeAxes = Axes.Both
        setSize(0.95f, 0.95f)
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


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (!isInEditMode) {
            val realMsElapsed = pSecondsElapsed * 1000

            if (isPressed) {
                holdDurationMs += realMsElapsed

                if (holdDurationMs >= requiredPressTimeMs) {
                    isPressed = false
                    GlobalManager.getInstance().gameScene.pause()
                    (parent as ExtendedEntity).invalidateInputBindings()
                }
            } else {
                holdDurationMs -= realMsElapsed * 2
            }
        }

        super.onManagedUpdate(pSecondsElapsed)
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
                if (localX <= 0f || localY <= 0f || localX >= drawWidth || localY >= drawHeight) {
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