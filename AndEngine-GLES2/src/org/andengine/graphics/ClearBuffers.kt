package org.andengine.graphics

/**
 * Which buffers of the current target a draw clears first; see [DrawContext.clear].
 */
data class ClearBuffers(val color: Boolean, val depth: Boolean, val stencil: Boolean) {

    /** Whether no buffer is cleared. */
    val isNone: Boolean get() = !color && !depth && !stencil

    companion object {

        @JvmField
        val None = ClearBuffers(color = false, depth = false, stencil = false)

        @JvmField
        val Color = ClearBuffers(color = true, depth = false, stencil = false)

        @JvmField
        val Depth = ClearBuffers(color = false, depth = true, stencil = false)

        @JvmField
        val Stencil = ClearBuffers(color = false, depth = false, stencil = true)

        @JvmField
        val All = ClearBuffers(color = true, depth = true, stencil = true)
    }
}
