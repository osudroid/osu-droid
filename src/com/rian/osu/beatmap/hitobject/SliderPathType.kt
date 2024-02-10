package com.rian.osu.beatmap.hitobject

/**
 * Types of slider paths.
 */
enum class SliderPathType {
    Catmull,
    Bezier,
    Linear,
    PerfectCurve;

    companion object {
        @JvmStatic
        fun parse(value: Char) =
            when (value) {
                'C' -> Catmull
                'L' -> Linear
                'P' -> PerfectCurve
                else -> Bezier
            }
    }
}
