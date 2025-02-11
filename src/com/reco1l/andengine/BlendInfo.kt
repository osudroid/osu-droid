package com.reco1l.andengine

import javax.microedition.khronos.opengles.GL10

data class BlendInfo(

    /**
     * The blending function to use.
     */
    val function: BlendingFunction,

    /**
     * Whether to mask the red channel.
     */
    val redMask: Boolean = true,

    /**
     * Whether to mask the green channel.
     */
    val greenMask: Boolean = true,

    /**
     * Whether to mask the blue channel.
     */
    val blueMask: Boolean = true,

    /**
     * Whether to mask the alpha channel.
     */
    val alphaMask: Boolean = true,

    /**
     * Whether to clear the color buffer.
     */
    val clear: Boolean = false

) {

    fun apply(gl: GL10) {

        gl.glColorMask(redMask, greenMask, blueMask, alphaMask)

        if (function != BlendingFunction.Inherit) {
            gl.glBlendFunc(function.source, function.destination)
        }

        if (clear) {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
        }
    }


    companion object {

        val Inherit = BlendInfo(BlendingFunction.Inherit)

        val Additive = BlendInfo(BlendingFunction.Additive)

        val Default = BlendInfo(BlendingFunction.Mixture)

    }

}