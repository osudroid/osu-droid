package com.reco1l.andengine.info

data class ClearInfo(

    /**
     * Whether to clear the depth buffer.
     */
    val depthBuffer: Boolean,

    /**
     * Whether to clear the color buffer.
     */
    val colorBuffer: Boolean,

    /**
     * Whether to clear the stencil buffer.
     */
    val stencilBuffer: Boolean

) {

    companion object {

        @JvmField
        val None = ClearInfo(
            depthBuffer = false,
            colorBuffer = false,
            stencilBuffer = false
        )

        @JvmField
        val ClearDepthBuffer = ClearInfo(
            depthBuffer = true,
            colorBuffer = false,
            stencilBuffer = false
        )

        @JvmField
        val ClearColorBuffer = ClearInfo(
            depthBuffer = false,
            colorBuffer = true,
            stencilBuffer = false
        )

        @JvmField
        val ClearStencilBuffer = ClearInfo(
            depthBuffer = false,
            colorBuffer = false,
            stencilBuffer = true
        )

        @JvmField
        val ClearAll = ClearInfo(
            depthBuffer = true,
            colorBuffer = true,
            stencilBuffer = true
        )

    }
}
