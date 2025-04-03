package com.reco1l.andengine.info

import android.opengl.GLES10

/**
 * Information about how to behave with the depth buffer.
 */
data class DepthInfo(

    /**
     * Whether to test with the depth buffer.
     */
    val test: Boolean = true,

    /**
     * Whether to write to the depth buffer.
     */
    val mask: Boolean = true,

    /**
     * The function to use during depth testing.
     */
    val function: Int,

) {

    companion object {

        @JvmField
        val Less = DepthInfo(
            test = true,
            mask = true,
            function = GLES10.GL_LESS,
        )

        @JvmField
        val Default = DepthInfo(
            test = true,
            mask = true,
            function = GLES10.GL_LESS
        )

        val None = DepthInfo(
            test = false,
            mask = false,
            function = GLES10.GL_ALWAYS
        )

    }

}