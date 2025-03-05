package com.reco1l.osu.hud.editor

import com.reco1l.andengine.*
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
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager

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
        y = -10f

        // Toolbar buttons:

        attachChild(Button("delete", ColorARGB(0xFF260000)) {
            updateThread {
                element.remove()
            }
        })

        attachChild(Button("oneone") {
            element.setScale((element.scaleX + element.scaleY) / 2f)
        })

        attachChild(Button("restore") {
            element.restore()
        })

    }

    private val nameText = ExtendedText().apply {
        anchor = Anchor.BottomCenter
        origin = Anchor.TopCenter
        font = ResourceManager.getInstance().getFont("smallFont")
        color = ColorARGB(0xFFF27272)
        text = element.name
    }


    init {
        alpha = 0f
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
        if (alpha == 1f) {
            return super.onAreaTouched(event, localX, localY)
        }
        return false
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        // We need to cancel scale center
        outline.x = element.anchorOffsetX + element.x - (element.widthScaled * element.origin.x)
        outline.y = element.anchorOffsetY + element.y - (element.heightScaled * element.origin.y)

        outline.width = element.widthScaled
        outline.height = element.heightScaled

        // Show only the tips that are not at the origin.
        mChildren?.fastForEach {
            if (it is Tip) {
                it.isVisible = it.anchor.x != element.origin.x && it.anchor.y != element.origin.y
            }
        }

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

                val deltaScaleX = deltaX / element.widthScaled
                val deltaScaleY = deltaY / element.heightScaled

                element.scaleX = (element.scaleX + deltaScaleX).coerceIn(0.5f, 5f)
                element.scaleY = (element.scaleY + deltaScaleY).coerceIn(0.5f, 5f)
                return true
            }

            return false
        }
    }



    /**
     * Represents a button in the overlay toolbar.
     */
    private inner class Button(texture: String, back: ColorARGB = ColorARGB(0xFF002626), val action: () -> Unit) : Container() {

        init {
            setSize(BUTTON_SIZE, BUTTON_SIZE)
            scaleCenter = Anchor.Center

            attachChild(RoundedBox().apply {
                cornerRadius = 12f
                color = back
                relativeSizeAxes = Axes.Both
                setSize(1f, 1f)
            })

            attachChild(ExtendedSprite().apply {
                textureRegion = ResourceManager.getInstance().getTexture(texture)
                anchor = Anchor.Center
                origin = Anchor.Center
                relativeSizeAxes = Axes.Both
                setSize(0.8f, 0.8f)
            })
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