package com.reco1l.osu.hud.elements

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.shape.RoundedBox
import com.reco1l.andengine.text.ExtendedText
import com.reco1l.framework.ColorARGB
import com.reco1l.osu.hud.GameplayHUD
import com.reco1l.osu.hud.data.HUDElementData
import com.reco1l.toolkt.kotlin.capitalize
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

abstract class HUDElement(

    /**
     * The tag of the element. This is used to identify the element in the HUD.
     */
    val tag: String

) : Container() {

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
     * Returns the tag with a readable manner.
     */
    val readableTag: String
        get() = tag.replace("([a-z])([A-Z])".toRegex(), "$1 $2").lowercase().capitalize()


    override fun getParent(): GameplayHUD? {
        return super.getParent() as? GameplayHUD
    }


    open fun onEditModeChange(value: Boolean) {
        background = if (value) HUDElementUnderlay(this) else null
    }

    fun onElementDataChange(data: HUDElementData) {

        parent!!.removeConstraint(this)

        if (data.constraintTo != null) {
            parent!!.addConstraint(this, parent!!.getElementByTag(data.constraintTo)!!)
        }

        anchor = data.anchor
        origin = data.origin
        setScale(data.scale)
        setPosition(data.constraintOffset.x, data.constraintOffset.y)
    }


    private var initialX = 0f
    private var initialY = 0f

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (isInEditMode) {
            val parentLocalX = drawX + localX
            val parentLocalY = drawY + localY

            if (event.action == TouchEvent.ACTION_DOWN) {
                //parent!!.onElementSelected(this)
                initialX = parentLocalX
                initialY = parentLocalY
                return true
            }

            if (event.action == TouchEvent.ACTION_MOVE) {
                val deltaX = parentLocalX - initialX
                val deltaY = parentLocalY - initialY

                setPosition(x + deltaX, y + deltaY)

                initialX = parentLocalX
                initialY = parentLocalY
                return true
            }

        }
        return false
    }


    open fun onGameplayUpdate(game: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        // Override this method to update the element with the latest gameplay data.
    }

    open fun onNoteHit(statistics: StatisticV2) {
        // Override this method to handle hit object hits.
    }

    open fun onBreakStateChange(isBreak: Boolean) {
        // Override this method to handle break state changes.
    }

}


class HUDElementUnderlay(private val element: HUDElement) : Container() {

    private val tagText = ExtendedText()


    init {
        tagText.font = ResourceManager.getInstance().getFont("smallFont")
        tagText.color = ColorARGB.White
        tagText.text = element.readableTag
        attachChild(tagText)

        autoSizeAxes = Axes.None

        background = RoundedBox().apply {
            color = ColorARGB.Red
            alpha = 0.25f
        }
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (element.drawY - drawHeight <= 0f) {
            tagText.anchor = Anchor.BottomLeft
            tagText.origin = Anchor.TopLeft
        } else {
            tagText.anchor = Anchor.TopLeft
            tagText.origin = Anchor.BottomLeft
        }

        // Cancel the scaling of the HUD element so the text is not affected by it, the same goes for the background.
        tagText.setScale(1f / element.scaleX, 1f / element.scaleY)

        (background as RoundedBox).cornerRadius = 6f * (1f / element.scaleX)
    }




}