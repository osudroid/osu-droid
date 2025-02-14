package com.reco1l.osu.hud.data

import com.reco1l.andengine.Anchor
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDElement
import com.reco1l.osu.hud.elements.HUDHealthBar
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.osu.hud.elements.HUDScoreCounter
import kotlin.reflect.KClass

data class HUDSkinData(val elements: List<HUDElementSkinData>) {

    companion object {

        /**
         * The default layout data for the HUD.
         * Based on the default skin layout from osu!stable.
         */
        val Default = HUDSkinData(
            listOf(
                HUDElementSkinData(type = HUDAccuracyCounter::class),
                HUDElementSkinData(type = HUDComboCounter::class),
                HUDElementSkinData(type = HUDPieSongProgress::class),
                HUDElementSkinData(type = HUDHealthBar::class),
                HUDElementSkinData(type = HUDScoreCounter::class)
            )
        )

    }

}

data class HUDElementSkinData(

    /**
     * The type of the element.
     */
    val type: KClass<out HUDElement>,

    /**
     * The layout of the element.
     */
    val layout: HUDElementLayoutData? = null,

    /**
     * The set of settings for the element.
     */
    val settings: Map<String, Any> = mapOf(),
)

data class HUDElementLayoutData(

    /**
     * The offset of the element from the constraint.
     */
    val position: Vec2 = Vec2.Zero,

    /**
     * The anchor of the element.
     */
    val anchor: Vec2 = Anchor.TopLeft,

    /**
     * The origin of the element.
     */
    val origin: Vec2 = Anchor.TopLeft,

    /**
     * The scale applied to the element.
     */
    val scale: Float = 1f,
)