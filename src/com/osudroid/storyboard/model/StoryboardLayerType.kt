package com.osudroid.storyboard.model

/**
 * Represents the layer a storyboard element is rendered on.
 *
 * The rendering order corresponds to the ordinal of this enum, with [Video] rendered first.
 */
enum class StoryboardLayerType {
    /**
     * Rendered below the [Background] layer.
     */
    Video,

    Background,

    /**
     * Only visible while the player is in a failing state.
     */
    Fail,

    /**
     * Only visible while the player is in a passing state.
     */
    Pass,

    Foreground,

    /**
     * Holds the elements of layers that are not part of the storyboard specification. Rendered
     * above the [Foreground] layer, as in osu!lazer.
     */
    Custom,

    /**
     * Rendered above gameplay elements.
     */
    Overlay;

    companion object {
        /**
         * Parses a layer from its name or numeric representation.
         *
         * @param value The value to parse.
         * @return The parsed [StoryboardLayerType], or `null` if the value is not a valid layer.
         */
        @JvmStatic
        fun parse(value: String) = when (value) {
            "0", "Background" -> Background
            "1", "Fail" -> Fail
            "2", "Pass" -> Pass
            "3", "Foreground" -> Foreground
            "4", "Overlay" -> Overlay
            "5", "Video" -> Video
            else -> null
        }
    }
}
