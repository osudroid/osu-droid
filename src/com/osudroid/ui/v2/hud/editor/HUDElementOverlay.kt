package com.osudroid.ui.v2.hud.editor

import com.reco1l.andengine.*
import com.reco1l.andengine.container.ConstraintContainer
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.container.LinearContainer
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.ExtendedSprite
import com.reco1l.andengine.text.*
import com.reco1l.framework.ColorARGB
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.osu.updateThread
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager

class HUDElementOverlay(private val element: HUDElement) : ConstraintContainer() {


    /**
     * The position of the outline box that represents the element.
     */
    val outlinePosition
        get() = outline.absolutePosition


    private val outline = Box().apply {
        paintStyle = PaintStyle.Outline
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
        setSize(FitParent, FitParent)

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


    /**
     * Updates the outline box to match the element's position and size.
     */
    fun updateOutline() {
        // We need to cancel scale center
        outline.x = element.anchorPositionX + element.x - (element.transformedWidth * element.origin.x)
        outline.y = element.anchorPositionY + element.y - (element.transformedHeight * element.origin.y)

        outline.width = element.transformedWidth
        outline.height = element.transformedHeight
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (alpha == 1f) {
            return super.onAreaTouched(event, localX, localY)
        }
        return false
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        updateOutline()

        if (outline.y - toolbar.height < 0f) {
            toolbar.y = -(outline.y - toolbar.height)
        } else {
            toolbar.y = 0f
        }

        // Show only the tips that are not at the origin.
        mChildren?.fastForEach {
            if (it is Tip) {
                it.isVisible = it.anchor.x != element.origin.x && it.anchor.y != element.origin.y
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    /**
     * Represents a tip in the overlay toolbar.
     */
    private inner class Tip : Container() {

        init {
            origin = Anchor.Center
            setSize(TIP_SIZE, TIP_SIZE)

            // The tip container is bigger than the actual tip in order to make it easier to touch.
            attachChild(Box().apply {
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

                val deltaScaleX = deltaX / element.transformedWidth
                val deltaScaleY = deltaY / element.transformedHeight

                val scaleX = (element.scaleX + deltaScaleX).coerceIn(0.5f, 5f)
                val scaleY = (element.scaleY + deltaScaleY).coerceIn(0.5f, 5f)

                element.setScale((scaleX + scaleY) / 2f)
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

            attachChild(Box().apply {
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
                beginSequence {
                    scaleTo(0.9f, 0.1f)
                    scaleTo(1f, 0.1f)
                }
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