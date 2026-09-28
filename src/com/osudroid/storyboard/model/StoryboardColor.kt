package com.osudroid.storyboard.model

/**
 * Represents an RGB color used by storyboard `C` commands, with each component in the range `[0, 1]`.
 */
data class StoryboardColor(
    @JvmField
    val red: Float,

    @JvmField
    val green: Float,

    @JvmField
    val blue: Float
) {
    companion object {
        @JvmField
        val WHITE = StoryboardColor(1f, 1f, 1f)
    }
}
