package com.reco1l.osu.hud

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.ExtendedEntity
import com.reco1l.andengine.anchorOffset
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.drawPosition
import com.reco1l.andengine.drawSize
import com.reco1l.andengine.getDrawHeight
import com.reco1l.andengine.getDrawWidth
import com.reco1l.andengine.originOffset
import com.reco1l.andengine.position
import com.reco1l.andengine.shape.Line
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.andengine.text.ExtendedText
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.editor.HUDElementOverlay
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDAverageOffsetCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDHealthBar
import com.reco1l.osu.hud.elements.HUDHitErrorMeter
import com.reco1l.osu.hud.elements.HUDLinearSongProgress
import com.reco1l.osu.hud.elements.HUDPPCounter
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.osu.hud.elements.HUDScoreCounter
import com.reco1l.osu.hud.elements.HUDUnstableRateCounter
import com.reco1l.osu.ui.entity.GameplayLeaderboard
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.math.abs
import kotlin.math.min


/**
 * List all the elements that can be added to the HUD.
 */
val HUDElements = listOf(
    HUDAccuracyCounter::class,
    HUDComboCounter::class,
    HUDHealthBar::class,
    HUDPieSongProgress::class,
    HUDPPCounter::class,
    HUDScoreCounter::class,
    HUDUnstableRateCounter::class,
    HUDAverageOffsetCounter::class,
    HUDHitErrorMeter::class,
    HUDLinearSongProgress::class,
    GameplayLeaderboard::class
)


abstract class HUDElement : Container(), IGameplayEvents {

    /**
     * Returns the name of this element.
     */
    val name: String
        get() = this::class.simpleName!!.replace("HUD", "").replace("([a-z])([A-Z])".toRegex(), "$1 $2")


    private var overlay: HUDElementOverlay? = null

    private var connectionLine: Line? = null

    private var isInEditMode = false


    //region Skinning

    open fun setSkinData(data: HUDElementSkinData?) {
        if (data != null) {
            anchor = data.anchor
            origin = data.origin

            // We always expect to rotate around the center of the element.
            rotationCenterX = 0.5f
            rotationCenterY = 0.5f
            rotation = data.rotation

            setScale(data.scale)
            setPosition(data.position.x, data.position.y)
        }
    }

    open fun getSkinData() = HUDElementSkinData(
        type = this::class,
        anchor = anchor,
        origin = origin,
        scale = scaleX, // Scale is uniform currently.
        position = Vec2(x, y),
        rotation = rotation
    )

    //endregion

    //region Element events

    open fun setEditMode(value: Boolean) {
        isInEditMode = value

        if (value) {
            background = HUDElementBackground()
            overlay = HUDElementOverlay(this)

            parent!!.attachChild(overlay!!)
        } else {
            connectionLine?.detachSelf()
            overlay?.detachSelf()

            connectionLine = null
            background = null
            overlay = null
        }
    }

    open fun onSelectionStateChange(isSelected: Boolean) {

        (background as? HUDElementBackground)?.isSelected = isSelected
        overlay?.isVisible = isSelected

        if (isSelected) {
            updateConnectionLine()
            connectionLine?.isVisible = true
        } else {
            connectionLine?.isVisible = false
        }
    }

    //endregion

    //region Entity events

    private var initialX = 0f
    private var initialY = 0f

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (isInEditMode) {
            val parentLocalX = drawX + localX
            val parentLocalY = drawY + localY

            if (event.isActionDown) {
                (parent as? GameplayHUD)?.selected = this

                initialX = parentLocalX
                initialY = parentLocalY
                return true
            }

            if (event.isActionMove) {
                val deltaX = parentLocalX - initialX
                val deltaY = parentLocalY - initialY

                // Preventing from moving the element if it's not selected.
                if ((parent as? GameplayHUD)?.selected == this) {
                    applyClosestAnchorOrigin()
                    x += deltaX
                    y += deltaY
                    updateConnectionLine()

                    initialX = parentLocalX
                    initialY = parentLocalY
                    return true
                }
            }

            if (event.isActionUp) {
                return (parent as? GameplayHUD)?.selected == this
            }
        }

        return false
    }

    override fun invalidateTransformations() {
        super.invalidateTransformations()
        overlay?.invalidateTransformations()
    }


    private fun applyClosestAnchorOrigin() {

        val drawSize = drawSize
        val drawPosition = drawPosition
        val parentDrawSize = (parent as ExtendedEntity).drawSize

        val relativeTopLeftDrawPosition = drawPosition / parentDrawSize
        val relativeBottomRightDrawPosition = (drawPosition + drawSize) / parentDrawSize

        val closest = Anchor.getAll().minBy {
            min(abs(relativeTopLeftDrawPosition.distance(it)), abs(relativeBottomRightDrawPosition.distance(it)))
        }

        if (anchor != closest) {
            val previousAnchorOffset = anchorOffset
            anchor = closest
            position -= anchorOffset - previousAnchorOffset
        }

        if (origin != closest) {
            val previousOriginOffset = originOffset
            origin = closest
            position -= originOffset - previousOriginOffset
        }
    }

    private fun updateConnectionLine() {

        val pointOnParent = Vec2(
            parent!!.getDrawWidth() * anchor.x,
            parent!!.getDrawHeight() * anchor.y
        )

        val pointOnChild = Vec2(
            drawX + drawWidth * origin.x,
            drawY + drawHeight * origin.y
        )

        if (connectionLine == null) {
            connectionLine = Line().apply {
                fromPoint = pointOnParent
                toPoint = pointOnChild
                color = ColorARGB(0xFFF27272)
                lineWidth = 10f
            }
            parent!!.attachChild(connectionLine!!)
        } else {
            connectionLine!!.fromPoint = pointOnParent
            connectionLine!!.toPoint = pointOnChild
        }
    }

    //endregion

    //region Edit mode

    /**
     * Removes the element from the HUD.
     */
    fun remove() {
        setEditMode(false)
        detachSelf()
    }

    private inner class HUDElementBackground : Container() {


        var isSelected = false
            set(value) {
                background!!.alpha = if (value) 0.5f else 0.15f
                nameText.isVisible = value
                field = value
            }


        private val nameText = ExtendedText().apply {
            font = ResourceManager.getInstance().getFont("smallFont")
            color = ColorARGB(0xFFF27272)
            text = this@HUDElement.name
            isVisible = false
        }


        init {
            attachChild(nameText)

            background = RoundedBox().apply {
                color = ColorARGB(0x29F27272)
                alpha = 0.25f
            }
        }

        override fun onManagedUpdate(pSecondsElapsed: Float) {

            // Switch the text position according to the element's position.
            if (this@HUDElement.drawY - drawHeight <= 0f) {
                nameText.anchor = Anchor.BottomLeft
                nameText.origin = Anchor.TopLeft
            } else {
                nameText.anchor = Anchor.TopLeft
                nameText.origin = Anchor.BottomLeft
            }

            // The element might contain scale transformations. Those transformations are also applied
            // to the background we might want to scale the text back to its original size.
            nameText.setScale(1f / this@HUDElement.scaleX, 1f / this@HUDElement.scaleY)

            // Same for the corner radius of the background.
            (background as RoundedBox).cornerRadius = 6f * (1f / this@HUDElement.scaleX)
        }

    }
    //endregion
}
