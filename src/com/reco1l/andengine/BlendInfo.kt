package com.reco1l.andengine

import javax.microedition.khronos.opengles.*

/**
 * Determines the blending function for the sprite.
 */
data class BlendInfo(

    /**
     * The source blending factor.
     */
    val sourceFactor: Int,

    /**
     * The destination blending factor.
     */
    val destinationFactor: Int

) {

    companion object {

        val None = BlendInfo(
            GL10.GL_ONE,
            GL10.GL_ZERO
        )

        val Mixture = BlendInfo(
            GL10.GL_SRC_ALPHA,
            GL10.GL_ONE_MINUS_SRC_ALPHA
        )

        val Additive = BlendInfo(
            GL10.GL_SRC_ALPHA,
            GL10.GL_ONE,
        )

        val PreMultiply = BlendInfo(
            GL10.GL_ONE,
            GL10.GL_ONE_MINUS_SRC_ALPHA
        )

        val Inherit = BlendInfo(-1, -1)

    }
}