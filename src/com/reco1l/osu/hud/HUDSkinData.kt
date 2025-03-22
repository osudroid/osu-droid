package com.reco1l.osu.hud

import com.reco1l.andengine.Anchor
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.elements.*
import com.reco1l.osu.ui.entity.GameplayLeaderboard
import com.reco1l.toolkt.data.putObject
import org.json.JSONArray
import kotlin.reflect.KClass

data class HUDSkinData(val elements: List<HUDElementSkinData>) {


    fun hasElement(type: Class<out HUDElement>) = elements.any { it.type.java == type }


    companion object {

        /**
         * The default layout data for the HUD.
         *
         * Based on the default skin layout from osu!droid version 1.8.2.1.
         */
        @JvmField
        val Default = HUDSkinData(
            listOf(
                HUDElementSkinData(
                    type = HUDAccuracyCounter::class,
                    anchor = Anchor.TopRight,
                    origin = Anchor.TopRight,
                    scale = 0.6f * 0.96f,
                    position = Vec2(-17f, 9f)
                ),
                HUDElementSkinData(
                    type = GameplayLeaderboard::class,
                    anchor = Anchor.TopLeft,
                    origin = Anchor.TopLeft,
                    position = Vec2(0f, 83f)
                ),
                HUDElementSkinData(
                    type = HUDComboCounter::class,
                    anchor = Anchor.BottomLeft,
                    origin = Anchor.BottomLeft,
                    position = Vec2(10f, -10f),
                    scale = 1.28f
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
                    scale = 0.96f,
                    position = Vec2(-10f, 0f)
                ),
                HUDElementSkinData(
                    type = HUDBackButton::class,
                    anchor = Anchor.CenterRight,
                    origin = Anchor.CenterRight,
                    position = Vec2(-20f, 0f)
                ),
                HUDElementSkinData(
                    type = HUDUnstableRateCounter::class,
                    anchor = Anchor.BottomRight,
                    origin = Anchor.BottomRight,
                    position = Vec2(-10f, -32f)
                ),
                HUDElementSkinData(
                    type = HUDAverageOffsetCounter::class,
                    anchor = Anchor.BottomRight,
                    origin = Anchor.BottomRight,
                    position = Vec2(-10f, -59f)
                ),
                HUDElementSkinData(
                    type = HUDHitErrorMeter::class,
                    anchor = Anchor.BottomCenter,
                    origin = Anchor.BottomCenter
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
                    put("scale", it.scale)
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
                    scale = element.optDouble("scale", 1.0).toFloat(),
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
    val scale: Float = 1f
)