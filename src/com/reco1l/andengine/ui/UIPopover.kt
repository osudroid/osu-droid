package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import org.andengine.engine.camera.Camera
import org.andengine.graphics.DrawContext
import kotlin.math.max

/**
 * A modal that opens below another component instead of in the middle of the screen, e.g. below the button
 * that shows it. The rest of the screen is not dimmed, touching it hides the popover.
 */
open class UIPopover(

    /**
     * The component the popover opens below.
     */
    val trigger: UIComponent,

    card: UIContainer = UIContainer()

) : UIModal(card) {

    /**
     * The space between the [trigger] and the [card].
     */
    var gap = 8f


    init {
        detachOnHide = true
        background?.alpha = 0f
        card.scaleCenter = Anchor.TopLeft
    }


    override fun onManagedDraw(drawContext: DrawContext, camera: Camera) {
        val (sceneX, sceneY) = trigger.convertLocalToSceneCoordinates(0f, trigger.height)

        // Moved back onto the screen if the card would reach beyond its right or bottom edge.
        card.x = sceneX.coerceIn(0f, max(0f, width - card.width))
        card.y = (sceneY + gap).coerceIn(0f, max(0f, height - card.height))

        super.onManagedDraw(drawContext, camera)
    }

}
