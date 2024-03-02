package com.rian.osu.beatmap.constants

/**
 * Represents various hit object types.
 */
enum class HitObjectType(
    /**
     * The bitwise type of the hit object.
     */
    @JvmField
    val value: Int
) {
    Normal(1),
    Slider(2),
    NewCombo(4),
    NormalNewCombo(5),
    SliderNewCombo(6),
    Spinner(8),
    ComboColorOffset(1 shl 4 or (1 shl 5) or (1 shl 6));

    companion object {
        @JvmStatic
        fun valueOf(value: Int) =
            when (value) {
                1 -> Normal
                2 -> Slider
                4 -> NewCombo
                5 -> NormalNewCombo
                6 -> SliderNewCombo
                else -> Spinner
            }
    }
}
