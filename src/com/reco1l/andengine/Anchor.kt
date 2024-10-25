package com.reco1l.andengine

/**
 * The anchor points in the range of [0, 1].
 */
enum class Anchor(val factorX: Float, val factorY: Float) {

    TopLeft(0f, 0f),

    TopCenter(0.5f, 0f),

    TopRight(1f, 0f),

    CenterLeft(0f, 0.5f),

    Center(0.5f, 0.5f),

    CenterRight(1f, 0.5f),

    BottomLeft(0f, 1f),

    BottomCenter(0.5f, 1f),

    BottomRight(1f, 1f)

}