package com.reco1l.andengine

import com.reco1l.framework.math.Vec2

object Anchor {

    @JvmField
    val TopLeft = Vec2(0f, 0f)

    @JvmField
    val TopCenter = Vec2(0.5f, 0f)

    @JvmField
    val TopRight = Vec2(1f, 0f)

    @JvmField
    val CenterLeft = Vec2(0f, 0.5f)

    @JvmField
    val Center = Vec2(0.5f, 0.5f)

    @JvmField
    val CenterRight = Vec2(1f, 0.5f)

    @JvmField
    val BottomLeft = Vec2(0f, 1f)

    @JvmField
    val BottomCenter = Vec2(0.5f, 1f)

    @JvmField
    val BottomRight = Vec2(1f, 1f)


    fun getName(anchor: Vec2): String {
        return when (anchor) {
            TopLeft -> "TopLeft"
            TopCenter -> "TopCenter"
            TopRight -> "TopRight"
            CenterLeft -> "CenterLeft"
            Center -> "Center"
            CenterRight -> "CenterRight"
            BottomLeft -> "BottomLeft"
            BottomCenter -> "BottomCenter"
            BottomRight -> "BottomRight"
            else -> throw IllegalArgumentException("Unknown anchor point: $anchor")
        }
    }

    fun getFromName(name: String): Vec2 {
        return when (name) {
            "TopLeft" -> TopLeft
            "TopCenter" -> TopCenter
            "TopRight" -> TopRight
            "CenterLeft" -> CenterLeft
            "Center" -> Center
            "CenterRight" -> CenterRight
            "BottomLeft" -> BottomLeft
            "BottomCenter" -> BottomCenter
            "BottomRight" -> BottomRight
            else -> throw IllegalArgumentException("Unknown anchor point: $name")
        }
    }

}