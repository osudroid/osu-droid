package com.osudroid.ui.v2.hud.editor

import com.reco1l.andengine.*
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.UIText
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import com.osudroid.ui.v2.hud.GameplayHUD
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.HUDElementSkinData
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Size
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.input.touch.TouchEvent
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.min

class HUDElementPreview(private val element: HUDElement, val hud: GameplayHUD): UIContainer() {


    private val label = UIText().apply {
        fontSize = FontSize.SM
        anchor = Anchor.BottomLeft
        origin = Anchor.BottomLeft
        text = element.name
        color = Color4.White
    }

    init {
        width = Size.Full
        height = 120f
        padding = Vec4(12f)
        scaleCenterX = 0.5f
        scaleCenterY = 0.5f
        backgroundColor = Color4(0xFF363653)
        radius = 12f

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

        if (element.width > element.height) {
            element.setScale(min(1f, innerWidth / element.width))
        } else {
            element.setScale(min(1f, (innerHeight - label.height) / element.height))
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
                beginSequence {
                    scaleTo(0.9f, 0.1f)
                    scaleTo(1f, 0.1f)
                }
                hud.addElement(HUDElementSkinData(element::class))
                hud.elementSelector?.collapse()
            }
        }

        return false
    }
    //endregion

}