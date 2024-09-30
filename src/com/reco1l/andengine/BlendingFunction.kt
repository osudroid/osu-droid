package com.reco1l.andengine

import javax.microedition.khronos.opengles.*

/**
 * Determines the blending function for the sprite.
 */
enum class BlendingFunction(

    /**
     * The source blending factor.
     */
    val source: Int,

    /**
     * The destination blending factor.
     */
    val destination: Int

) {

    None(
        GL10.GL_ONE,
        GL10.GL_ZERO
    ),

    Mixture(
        GL10.GL_SRC_ALPHA,
        GL10.GL_ONE_MINUS_SRC_ALPHA
    ),

    Additive(
        GL10.GL_SRC_ALPHA,
        GL10.GL_ONE,
    ),

    Inherit(-1, -1)

}