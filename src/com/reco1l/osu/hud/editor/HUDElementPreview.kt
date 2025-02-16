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
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.math.abs

class HUDElementPreview(val element: HUDElement, val hud: GameplayHUD): Container() {


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

        attachChild(ExtendedText().apply {
            font = ResourceManager.getInstance().getFont("smallFont")
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            text = element.name
            color = ColorARGB.White
        })

        attachChild(element)
        element.setSkinData(HUDElementSkinData(element::class))

        // Scaling the element inside the box

        if (element.drawHeight > getPaddedHeight()) {
            element.setScale(getPaddedHeight() / element.drawHeight)
        }

        if (element.drawWidth > getPaddedWidth()) {
            element.setScale(getPaddedWidth() / element.drawWidth)
        }
    }

    //region Input handling
    private var initialX = 0f
    private var initialY = 0f
    private var initialTime = 0L
    private var wasMoved = false

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (event.isActionDown) {
            initialX = localX
            initialY = localY
            initialTime = System.currentTimeMillis()

            clearEntityModifiers()
            scaleTo(0.9f, 0.1f)
            return true
        }

        if (event.isActionMove) {
            clearEntityModifiers()
            scaleTo(1f, 0.1f)
        }

        if (event.isActionUp) {
            clearEntityModifiers()
            scaleTo(1f, 0.1f)

            wasMoved = abs(localX - initialX) > 1f && abs(localY - initialY) > 1f

            if (!wasMoved && System.currentTimeMillis() - initialTime > 50) {
                hud.addElement(HUDElementSkinData(element::class))
                return false
            }
        }

        return false
    }
    //endregion

}