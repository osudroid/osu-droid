package com.reco1l.andengine.component

import android.opengl.GLES32

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
            GLES32.GL_ONE,
            GLES32.GL_ZERO
        )

        val Mixture = BlendInfo(
            GLES32.GL_SRC_ALPHA,
            GLES32.GL_ONE_MINUS_SRC_ALPHA
        )

        val Additive = BlendInfo(
            GLES32.GL_SRC_ALPHA,
            GLES32.GL_ONE,
        )

        val PreMultiply = BlendInfo(
            GLES32.GL_ONE,
            GLES32.GL_ONE_MINUS_SRC_ALPHA
        )

        val Inherit = BlendInfo(-1, -1)

    }
}