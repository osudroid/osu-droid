package com.reco1l.osu.hud

import com.reco1l.andengine.Anchor
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDHealthBar
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.osu.hud.elements.HUDScoreCounter
import com.reco1l.toolkt.data.putObject
import org.json.JSONArray
import kotlin.reflect.KClass

data class HUDSkinData(val elements: List<HUDElementSkinData>) {


    fun hasElement(type: Class<out HUDElement>) = elements.any { it.type == type }


    companion object {

        /**
         * The default layout data for the HUD.
         * Based on the default skin layout from osu!stable.
         */
        @JvmField
        val Default = HUDSkinData(
            listOf(
                HUDElementSkinData(type = HUDAccuracyCounter::class),
                HUDElementSkinData(type = HUDComboCounter::class),
                HUDElementSkinData(type = HUDPieSongProgress::class),
                HUDElementSkinData(type = HUDHealthBar::class),
                HUDElementSkinData(type = HUDScoreCounter::class)
            )
        )


        @JvmStatic
        fun writeToJSON(data: HUDSkinData) = JSONArray().apply {
            data.elements.forEach {
                putObject {
                    put("type", it.type.simpleName)
                    put("x", it.position.x)
                    put("y", it.position.y)
                    put("anchor", Anchor.getName(it.anchor))
                    put("origin", Anchor.getName(it.origin))
                    put("scale", it.scale)
                    put("rotation", it.rotation)
                }
            }
        }

        @JvmStatic
        fun readFromJSON(json: JSONArray) = HUDSkinData(
            elements = MutableList(json.length()) { i ->

                val element = json.getJSONObject(i)

                HUDElementSkinData(
                    type = HUDElements.first { it.simpleName == element.getString("type") },
                    position = Vec2(
                        element.optDouble("x", 0.0).toFloat(),
                        element.optDouble("y", 0.0).toFloat(),
                    ),
                    anchor = Anchor.getFromName(element.optString("anchor", "TopLeft")),
                    origin = Anchor.getFromName(element.optString("origin", "TopLeft")),
                    scale = element.optDouble("scale", 1.0).toFloat(),
                    rotation = element.optDouble("rotation", 1.0).toFloat()
                )
            }

        )


    }

}

data class HUDElementSkinData(

    /**
     * The type of the element.
     */
    val type: KClass<out HUDElement>,

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

    /**
     * The rotation of the element.
     */
    val rotation: Float = 0f
)