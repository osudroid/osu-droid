package org.andengine.graphics

/**
 * How a draw uses the depth buffer: the compare function of the depth test ([test], null for no
 * test) and whether it writes depth ([write]), as a [Pipeline] takes them.
 */
data class DepthState(val test: CompareFunction?, val write: Boolean) {

    companion object {

        /** No depth test, no depth write. */
        @JvmField
        val Disabled = DepthState(null, false)

        /** Passes nearer fragments and writes their depth. */
        @JvmField
        val Less = DepthState(CompareFunction.Less, true)
    }
}
