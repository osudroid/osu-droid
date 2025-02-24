package com.reco1l.osu.hud.editor

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.ConstraintContainer
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.container.LinearContainer
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.ExtendedSprite
import com.reco1l.andengine.text.*
import com.reco1l.framework.ColorARGB
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.updateThread
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.math.*

class HUDElementOverlay(private val element: HUDElement) : ConstraintContainer() {


    val outline = OutlineBox().apply {
        color = ColorARGB(0xFFF27272)
        lineWidth = 8f
    }


    private val toolbar = LinearContainer().apply {
        anchor = Anchor.TopCenter
        origin = Anchor.BottomCenter
        orientation = Orientation.Horizontal
        spacing = 4f

        attachChild(Button("delete", ColorARGB(0xFF260000)) {
            updateThread {
                element.remove()
            }
        })

        // Flip horizontally
        attachChild(Button("flip", ColorARGB(0xFF181825)) {
            element.scaleX = -element.scaleX
        })

        // Flip vertically
        attachChild(Button("flip", ColorARGB(0xFF181825)) {
            element.scaleY = -element.scaleY
        }.apply { icon.rotation = -90f })

    }

    private val nameText = ExtendedText().apply {
        anchor = Anchor.BottomCenter
        origin = Anchor.TopCenter
        font = ResourceManager.getInstance().getFont("smallFont")
        color = ColorARGB(0xFFF27272)
        text = element.name
    }


    init {
        isVisible = false
        setSize(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())

        attachChild(outline)
        attachChild(toolbar)
        attachChild(nameText)

        addConstraint(toolbar, outline)
        addConstraint(nameText, outline)

        val topLeftTip = Tip().apply { anchor = Anchor.BottomLeft }
        val topRightTip = Tip().apply { anchor = Anchor.BottomRight }
        val bottomLeftTip = Tip().apply { anchor = Anchor.TopLeft }
        val bottomRightTip = Tip().apply { anchor = Anchor.TopRight }

        attachChild(topLeftTip)
        attachChild(topRightTip)
        attachChild(bottomLeftTip)
        attachChild(bottomRightTip)

        addConstraint(topLeftTip, outline)
        addConstraint(topRightTip, outline)
        addConstraint(bottomLeftTip, outline)
        addConstraint(bottomRightTip, outline)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (isVisible) {
            return super.onAreaTouched(event, localX, localY)
        }
        return false
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        // We need to cancel scale center
        outline.x = element.drawX + (element.drawWidth * element.scaleCenterX) * (1f - abs(element.scaleX))
        outline.y = element.drawY + (element.drawHeight * element.scaleCenterY) * (1f - abs(element.scaleY))

        outline.width = element.drawWidth * abs(element.scaleX)
        outline.height = element.drawHeight * abs(element.scaleY)

        super.onManagedUpdate(pSecondsElapsed)
    }



    /**
     * Represents a tip in the overlay toolbar.
     */
    private inner class Tip : Container() {

        init {
            origin = Anchor.Center
            setSize(TIP_SIZE, TIP_SIZE)

            // The tip container is bigger than the actual tip in order to make it easier to touch.
            attachChild(RoundedBox().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                color = ColorARGB(0xFFF27272)
                cornerRadius = TIP_SIZE
                relativeSizeAxes = Axes.Both
                setSize(0.5f, 0.5f)
            })
        }


        private var initialX = 0f
        private var initialY = 0f

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

            if (event.isActionDown) {
                initialX = localX
                initialY = localY
                return true
            }

            if (event.isActionMove) {
                var deltaX = localX - initialX
                var deltaY = localY - initialY

                if (anchor.x == 0f) {
                    deltaX = -deltaX
                }

                if (anchor.y == 0f) {
                    deltaY = -deltaY
                }

                val deltaScaleX = deltaX / 100f
                val deltaScaleY = deltaY / 100f

                element.scaleX = (abs(element.scaleX) + deltaScaleX).coerceIn(0.5f, 5f).withSign(element.scaleX)
                element.scaleY = (abs(element.scaleY) + deltaScaleY).coerceIn(0.5f, 5f).withSign(element.scaleY)
                return true
            }

            return false
        }
    }



    /**
     * Represents a button in the overlay toolbar.
     */
    private inner class Button(texture: String, back: ColorARGB, val action: () -> Unit) : Container() {

        val icon = ExtendedSprite().apply {
            textureRegion = ResourceManager.getInstance().getTexture(texture)
            anchor = Anchor.Center
            origin = Anchor.Center
            relativeSizeAxes = Axes.Both
            setSize(0.8f, 0.8f)
        }

        init {
            setSize(BUTTON_SIZE, BUTTON_SIZE)

            background = RoundedBox().apply {
                cornerRadius = 12f
                color = back
            }

            attachChild(icon)

        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (event.isActionUp) {
                action()
                clearEntityModifiers()
                scaleTo(0.9f, 0.1f).scaleTo(1f, 0.1f)
                return true
            }

            if (event.isActionDown) {
                return true
            }

            return false
        }
    }


    companion object {
        private const val TIP_SIZE = 36f
        private const val BUTTON_SIZE = 46f
    }

}