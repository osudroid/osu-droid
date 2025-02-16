package com.reco1l.osu.hud

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.originOffsetX
import com.reco1l.andengine.originOffsetY
import com.reco1l.andengine.shape.Line
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.andengine.text.ExtendedText
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.editor.HUDElementToolbar
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDHealthBar
import com.reco1l.osu.hud.elements.HUDPPCounter
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.osu.hud.elements.HUDScoreCounter
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.math.abs


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
)


abstract class HUDElement : Container() {

    /**
     * Returns the name of this element.
     */
    val name: String
        get() = this::class.simpleName!!.replace("HUD", "").replace("([a-z])([A-Z])".toRegex(), "$1 $2")


    private var toolbar: HUDElementToolbar? = null

    private var connectionLine: Line? = null

    private var isInEditMode = false


    //region Skinning

    open fun setSkinData(data: HUDElementSkinData?) {
        if (data != null) {
            anchor = data.anchor
            origin = data.origin
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
            toolbar = HUDElementToolbar(this)

            parent!!.attachChild(toolbar!!)
        } else {
            connectionLine?.detachSelf()
            toolbar?.detachSelf()

            connectionLine = null
            background = null
            toolbar = null
        }
    }

    open fun onSelectionStateChange(isSelected: Boolean) {

        (background as HUDElementBackground).isSelected = isSelected
        toolbar?.isVisible = isSelected

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
                parent!!.selected = this

                initialX = parentLocalX
                initialY = parentLocalY
                return true
            }

            if (event.isActionMove) {
                val deltaX = parentLocalX - initialX
                val deltaY = parentLocalY - initialY

                setPosition(x + deltaX, y + deltaY)
                findNearestAnchorPoint()
                updateConnectionLine()

                initialX = parentLocalX
                initialY = parentLocalY
                return true
            }

        }
        return false
    }

    override fun invalidateTransformations() {
        super.invalidateTransformations()

        toolbar?.invalidateTransformations()
    }

    private fun findNearestAnchorPoint() {

        val anchors = floatArrayOf(0f, 0.5f, 1f)

        val nearestAnchor = Vec2(
            anchors.minBy { abs(drawX - originOffsetX - parent!!.drawWidth * it) },
            anchors.minBy { abs(drawY - originOffsetY - parent!!.drawHeight * it) }
        )

        if (nearestAnchor.x != anchor.x) {
            x = -x
        }

        if (nearestAnchor.y != anchor.y) {
            y = -y
        }

        anchor = nearestAnchor
    }

    private fun updateConnectionLine() {

        val pointOnParent = Vec2(
            parent!!.drawWidth * anchor.x,
            parent!!.drawHeight * anchor.y
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

    //region Gameplay Events
    open fun onGameplayUpdate(game: GameScene, statistics: StatisticV2, secondsElapsed: Float) = Unit
    open fun onNoteHit(statistics: StatisticV2) = Unit
    open fun onBreakStateChange(isBreak: Boolean) = Unit
    //endregion


    override fun getParent(): GameplayHUD? {
        return super.getParent() as? GameplayHUD
    }


    //region Edit mode
    inner class HUDElementBackground : Container() {


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
