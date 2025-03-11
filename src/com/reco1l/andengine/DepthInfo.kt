package com.reco1l.andengine

import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

/**
 * Information about how to behave with the depth buffer.
 */
data class DepthInfo(

    /**
     * The depth function to use.
     */
    val function: DepthFunction,

    /**
     * Whether to write to the depth buffer.
     */
    val mask: Boolean,

    /**
     * Whether to clear the depth buffer.
     */
    val clear: Boolean

) {


    fun apply(gl: GL10) {
        gl.glDepthFunc(function.glType)
        gl.glDepthMask(mask)

        if (clear) {
            gl.glClear(GL10.GL_DEPTH_BUFFER_BIT)
        }

        GLHelper.enableDepthTest(gl)
    }


    companion object {

        @JvmField
        val Clear = DepthInfo(
            function = DepthFunction.Less,
            mask = true,
            clear = true
        )

        @JvmField
        val Default = DepthInfo(
            function = DepthFunction.Less,
            mask = true,
            clear = false
        )

    }

}


enum class DepthFunction(val glType: Int) {
    Never(GL10.GL_NEVER),
    Less(GL10.GL_LESS),
    Equal(GL10.GL_EQUAL),
    LessOrEqual(GL10.GL_LEQUAL),
    Greater(GL10.GL_GREATER),
    NotEqual(GL10.GL_NOTEQUAL),
    GreaterOrEqual(GL10.GL_GEQUAL),
    Always(GL10.GL_ALWAYS)
}