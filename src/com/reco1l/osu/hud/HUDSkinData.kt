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
                HUDElementSkinData(
                    type = HUDAccuracyCounter::class,
                    anchor = Anchor.TopRight,
                    origin = Anchor.TopRight,
                    scale = Vec2(0.6f * 0.96f),
                    position = Vec2(-17f, 9f)
                ),
                HUDElementSkinData(
                    type = HUDComboCounter::class,
                    anchor = Anchor.BottomLeft,
                    origin = Anchor.BottomLeft,
                    position = Vec2(10f, -10f),
                    scale = Vec2(1.28f)
                ),
                HUDElementSkinData(
                    type = HUDPieSongProgress::class,
                    anchor = Anchor.TopRight,
                    origin = Anchor.CenterRight
                ),
                HUDElementSkinData(
                    type = HUDHealthBar::class,
                    anchor = Anchor.TopLeft,
                    origin = Anchor.TopLeft
                ),
                HUDElementSkinData(
                    type = HUDScoreCounter::class,
                    anchor = Anchor.TopRight,
                    origin = Anchor.TopRight,
                    scale = Vec2(0.96f),
                    position = Vec2(-10f, 0f)
                )
            )
        )


        @JvmStatic
        fun writeToJSON(data: HUDSkinData) = JSONArray().apply {
            data.elements.forEach {
                putObject {
                    put("type", HUDElements[it.type].name)
                    put("x", it.position.x)
                    put("y", it.position.y)
                    put("anchor", Anchor.getName(it.anchor))
                    put("origin", Anchor.getName(it.origin))
                    put("scaleX", it.scale.x)
                    put("scaleY", it.scale.y)
                }
            }
        }

        @JvmStatic
        fun readFromJSON(json: JSONArray) = HUDSkinData(
            elements = MutableList(json.length()) { i ->

                val element = json.getJSONObject(i)

                HUDElementSkinData(
                    type = HUDElements[element.getString("type")]?.type ?: return@MutableList null,
                    position = Vec2(
                        element.optDouble("x", 0.0).toFloat(),
                        element.optDouble("y", 0.0).toFloat(),
                    ),
                    anchor = Anchor.getFromName(element.optString("anchor", "Center")),
                    origin = Anchor.getFromName(element.optString("origin", "Center")),
                    scale = Vec2(
                        element.optDouble("scaleX", 1.0).toFloat(),
                        element.optDouble("scaleY", 1.0).toFloat()
                    )
                )
            }.filterNotNull()
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
    val anchor: Vec2 = Anchor.Center,

    /**
     * The origin of the element.
     */
    val origin: Vec2 = Anchor.Center,

    /**
     * The scale applied to the element.
     */
    val scale: Vec2 = Vec2.One
)