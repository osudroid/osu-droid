package com.reco1l.osu.hud.data

import com.reco1l.andengine.Anchor
import com.reco1l.framework.math.Vec2

data class HUDLayoutData(val elements: Map<String, HUDElementData>) {


    fun hasElement(tag: String): Boolean {
        return elements.containsKey(tag)
    }


    companion object {

        /**
         * The default layout data for the HUD.
         * Based on the default skin layout from osu!stable.
         */
        val Default = HUDLayoutData(
            mapOf(
                "accuracyCounter" to HUDElementData(
                    scale = 0.6f * 0.96f,
                    constraintOffset = Vec2(0f, 9f),
                    constraintTo = "scoreCounter",
                    anchor = Anchor.BottomRight,
                    origin = Anchor.TopRight
                ),
                "comboCounter" to HUDElementData(
                    scale = 1.28f,
                    constraintOffset = Vec2(10f, -10f),
                    anchor = Anchor.BottomLeft,
                    origin = Anchor.BottomLeft
                ),
                "pieSongProgress" to HUDElementData(
                    constraintOffset = Vec2(18f, 0f),
                    constraintTo = "accuracyCounter",
                    anchor = Anchor.TopLeft,
                    origin = Anchor.CenterRight
                ),
                "healthBar" to HUDElementData(),
                "scoreCounter" to HUDElementData(
                    scale = 0.96f,
                    constraintOffset = Vec2(-10f, 0f),
                    anchor = Anchor.TopRight,
                    origin = Anchor.TopRight
                )
            )
        )

    }

}


data class HUDElementData(

    /**
     * The scale applied to the element.
     */
    val scale: Float = 1f,

    /**
     * The offset of the element from the constraint.
     */
    val constraintOffset: Vec2 = Vec2.Zero,

    /**
     * The tag of the constraint to which the element is attached.
     */
    val constraintTo: String? = null,

    /**
     * The anchor of the element.
     */
    val anchor: Vec2 = Anchor.TopLeft,

    /**
     * The origin of the element.
     */
    val origin: Vec2 = Anchor.TopLeft,
)