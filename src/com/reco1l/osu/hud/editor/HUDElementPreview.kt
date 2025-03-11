package com.reco1l.osu.hud.editor

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.getPaddedHeight
import com.reco1l.andengine.getPaddedWidth
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.andengine.text.ExtendedText
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.math.Vec4
import com.reco1l.osu.hud.GameplayHUD
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.hud.HUDElementSkinData
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.min

class HUDElementPreview(private val element: HUDElement, val hud: GameplayHUD): Container() {


    private val label = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        text = element.name
        color = ColorARGB.White
    }

    init {
        width = HUDElementSelector.SELECTOR_WIDTH - 16f * 2
        height = 120f
        padding = Vec4(12f)
        scaleCenterX = 0.5f
        scaleCenterY = 0.5f

        background = RoundedBox().apply {
            color = ColorARGB(0xFF363653)
            cornerRadius = 12f
        }

        attachChild(element)
        attachChild(label)
        element.setSkinData(
            HUDElementSkinData(
                type = element::class,
                anchor = Anchor.TopLeft,
                origin = Anchor.TopLeft,
            )
        )
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        // Scaling the element inside the box
        element.setScaleCenter(0f, 0f)

        if (element.drawWidth > element.drawHeight) {
            element.setScale(min(1f, getPaddedWidth() / element.drawWidth))
        } else {
            element.setScale(min(1f, (getPaddedHeight() - label.drawHeight) / element.drawHeight))
        }


        super.onManagedDraw(gl, camera)
    }

    //region Input handling
    private var initialX = 0f
    private var initialY = 0f

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (event.isActionDown) {
            initialX = localX
            initialY = localY
        }

        if (event.isActionUp) {

            if (abs(localX - initialX) < 1f && abs(localY - initialY) < 1f) {
                clearEntityModifiers()
                scaleTo(0.9f, 0.1f).scaleTo(1f, 0.1f)

                hud.addElement(HUDElementSkinData(element::class))
                hud.elementSelector?.collapse()
            }
        }

        return false
    }
    //endregion

}