package com.reco1l.osu.hud.editor

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.container.LinearContainer
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.andengine.sprite.ExtendedSprite
import com.reco1l.framework.ColorARGB
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.updateThread
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager

class HUDElementToolbar(private val element: HUDElement) : LinearContainer() {

    init {
        orientation = Orientation.Horizontal
        isVisible = false
        spacing = 4f

        attachChild(createToolbarButton("delete", ColorARGB(0xFF260000)) {
            updateThread {
                element.parent?.detachChild(element)
            }
        })

        attachChild(createToolbarButton("rotate_left", ColorARGB(0xFF181825)) {
            element.rotation -= 90f
        })

        attachChild(createToolbarButton("rotate_right", ColorARGB(0xFF181825)) {
            element.rotation += 90f
        })

        onMeasureContentSize()
    }

    private fun createToolbarButton(texture: String, back: ColorARGB, action: () -> Unit) = object : Container() {

        init {
            setSize(46f, 46f)

            background = RoundedBox().apply {
                cornerRadius = 12f
                color = back
            }

            attachChild(ExtendedSprite().apply {
                textureRegion = ResourceManager.getInstance().getTexture(texture)
                anchor = Anchor.Center
                origin = Anchor.Center
                relativeSizeAxes = Axes.Both
                setSize(0.9f, 0.9f)
            })

        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (event.isActionUp) {
                action()
                return true
            }
            return false
        }

        override fun onManagedUpdate(pSecondsElapsed: Float) {

            x = element.drawX + element.drawWidth / 2 - drawWidth / 2

            if (element.drawY < drawHeight) {
                y = element.drawY + element.drawHeight + 4f
            } else {
                y = element.drawY - drawHeight - 4f
            }

            super.onManagedUpdate(pSecondsElapsed)
        }
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (!isVisible) {
            return false
        }
        return super.onAreaTouched(event, localX, localY)
    }

}