package com.reco1l.osu.hud.elements

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.shape.Line
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.andengine.text.ExtendedText
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.GameplayHUD
import com.reco1l.osu.hud.data.HUDElementSkinData
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.math.abs
import kotlin.reflect.KClass


fun <T : HUDElement> KClass<T>.create(): T {
    return constructors.first().call()
}

abstract class HUDElement : Container() {

    /**
     * The HUD element data for this element. This property can only be set once.
     */
    var elementData: HUDElementSkinData? = null
        set(value) {
            if (field == null) {
                field = value
            } else {
                throw IllegalStateException("The layout data for this element has already been set.")
            }
        }

    /**
     * Whether the element is in edit mode or not.
     */
    var isInEditMode = false
        set(value) {
            if (field != value) {
                onEditModeChange(value)
                field = value
            }
        }

    /**
     * Whether the element is selected or not.
     */
    var isSelected = false
        set(value) {
            if (field != value) {
                val background = background as HUDElementUnderlay

                if (value) {
                    background.select()
                } else {
                    background.unselect()
                }
                field = value
            }
        }

    /**
     * The line that connects this element to the parent's anchor
     */
    var nodeLine: Line? = null

    /**
     * Returns the name of this element.
     */
    val name: String
        get() = this::class.simpleName!!.substring(3).replace("([a-z])([A-Z])".toRegex(), "$1 $2")


    private var initialX = 0f
    private var initialY = 0f


    open fun onEditModeChange(value: Boolean) {
        background = if (value) HUDElementUnderlay(this) else null
    }


    open fun onSkinDataChange(data: HUDElementSkinData?) {

        val layout = data?.layout ?: return

        anchor = layout.anchor
        origin = layout.origin

        setScale(layout.scale)
        setPosition(layout.position.x, layout.position.y)
    }


    private fun calculateNearestAnchor(deltaX: Float, deltaY: Float) {

        val anchors = floatArrayOf(0f, 0.5f, 1f)

        val originX = drawX + drawWidth * origin.x + deltaX
        val originY = drawY + drawHeight * origin.y + deltaY

        val nearestX = anchors.minBy { abs(originX - parent!!.drawWidth * it) }
        val nearestY = anchors.minBy { abs(originY - parent!!.drawHeight * it) }

        if (nearestX != anchor.x) {
            x = -x
        }

        if (nearestY != anchor.y) {
            y = -y
        }

        anchor = Vec2(nearestX, nearestY)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (isInEditMode) {
            val parentLocalX = drawX + localX
            val parentLocalY = drawY + localY

            if (event.action == TouchEvent.ACTION_DOWN) {
                parent!!.selected = this
                initialX = parentLocalX
                initialY = parentLocalY
                return true
            }

            if (event.action == TouchEvent.ACTION_MOVE) {
                val deltaX = parentLocalX - initialX
                val deltaY = parentLocalY - initialY

                calculateNearestAnchor(deltaX, deltaY)
                setPosition(x + deltaX, y + deltaY)

                nodeLine?.toPoint = Vec2(
                    parent!!.drawWidth * anchor.x,
                    parent!!.drawHeight * anchor.y
                )

                nodeLine?.fromPoint = Vec2(
                    drawX + drawWidth * origin.x,
                    drawY + drawHeight * origin.y
                )

                initialX = parentLocalX
                initialY = parentLocalY
                return true
            }

        }
        return false
    }


    //region Gameplay Events
    open fun onGameplayUpdate(game: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        // Override this method to update the element with the latest gameplay data.
    }

    open fun onNoteHit(statistics: StatisticV2) {
        // Override this method to handle hit object hits.
    }

    open fun onBreakStateChange(isBreak: Boolean) {
        // Override this method to handle break state changes.
    }
    //endregion


    override fun getParent(): GameplayHUD? {
        return super.getParent() as? GameplayHUD
    }

}


class HUDElementUnderlay(private val element: HUDElement) : Container() {

    private val nameText = ExtendedText()


    init {
        nameText.font = ResourceManager.getInstance().getFont("smallFont")
        nameText.color = ColorARGB(0xFFF27272)
        nameText.text = element.name
        nameText.isVisible = false
        attachChild(nameText)

        autoSizeAxes = Axes.None

        background = RoundedBox().apply {
            color = ColorARGB(0x29F27272)
            alpha = 0.25f
        }
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (element.drawY - drawHeight <= 0f) {
            nameText.anchor = Anchor.BottomLeft
            nameText.origin = Anchor.TopLeft
        } else {
            nameText.anchor = Anchor.TopLeft
            nameText.origin = Anchor.BottomLeft
        }

        // Cancel the scaling of the HUD element so the text is not affected by it, the same goes for the background.
        nameText.setScale(1f / element.scaleX, 1f / element.scaleY)

        (background as RoundedBox).cornerRadius = 6f * (1f / element.scaleX)
    }


    fun select() {
        background!!.color = ColorARGB(0x80F27272)
        nameText.isVisible = true
        element.nodeLine?.isVisible = true
    }

    fun unselect() {
        background!!.color = ColorARGB(0x29F27272)
        nameText.isVisible = false
        element.nodeLine?.isVisible = false
    }


}