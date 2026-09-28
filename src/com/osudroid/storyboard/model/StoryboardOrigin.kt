package com.osudroid.storyboard.model

/**
 * Represents the origin of a storyboard element, which acts as both the positional anchor
 * and the center of rotation and scaling.
 */
enum class StoryboardOrigin(
    /**
     * The normalized horizontal factor of this origin relative to the element's width.
     */
    @JvmField
    val factorX: Float,

    /**
     * The normalized vertical factor of this origin relative to the element's height.
     */
    @JvmField
    val factorY: Float
) {
    TopLeft(0f, 0f),
    Centre(0.5f, 0.5f),
    CentreLeft(0f, 0.5f),
    TopRight(1f, 0f),
    BottomCentre(0.5f, 1f),
    TopCentre(0.5f, 0f),
    CentreRight(1f, 0.5f),
    BottomLeft(0f, 1f),
    BottomRight(1f, 1f);

    companion object {
        /**
         * Parses an origin from its name or numeric representation.
         *
         * Invalid values (including the unsupported `Custom` origin) fall back to [TopLeft],
         * matching osu!stable behavior.
         *
         * @param value The value to parse.
         * @return The parsed [StoryboardOrigin].
         */
        @JvmStatic
        fun parse(value: String) = when (value) {
            "0", "TopLeft" -> TopLeft
            "1", "Centre" -> Centre
            "2", "CentreLeft" -> CentreLeft
            "3", "TopRight" -> TopRight
            "4", "BottomCentre" -> BottomCentre
            "5", "TopCentre" -> TopCentre
            "7", "CentreRight" -> CentreRight
            "8", "BottomLeft" -> BottomLeft
            "9", "BottomRight" -> BottomRight
            else -> TopLeft
        }
    }
}
