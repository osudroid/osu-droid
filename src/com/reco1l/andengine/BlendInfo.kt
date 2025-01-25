package com.reco1l.andengine

import javax.microedition.khronos.opengles.GL10

data class BlendInfo(

    /**
     * The blending function to use.
     */
    var function: BlendingFunction,

    /**
     * Whether to mask the red channel.
     */
    var redMask: Boolean = true,

    /**
     * Whether to mask the green channel.
     */
    var greenMask: Boolean = true,

    /**
     * Whether to mask the blue channel.
     */
    var blueMask: Boolean = true,

    /**
     * Whether to mask the alpha channel.
     */
    var alphaMask: Boolean = true,

    /**
     * Whether to clear the color buffer.
     */
    var clear: Boolean = false

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

    }

}