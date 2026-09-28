package com.osudroid.storyboard.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents the easing functions available to storyboard commands.
 *
 * The functions are ported from osu!framework's `DefaultEasingFunction` so that command
 * interpolation matches osu!stable and osu!lazer.
 */
enum class StoryboardEasing {
    None,
    Out,
    In,
    InQuad,
    OutQuad,
    InOutQuad,
    InCubic,
    OutCubic,
    InOutCubic,
    InQuart,
    OutQuart,
    InOutQuart,
    InQuint,
    OutQuint,
    InOutQuint,
    InSine,
    OutSine,
    InOutSine,
    InExpo,
    OutExpo,
    InOutExpo,
    InCirc,
    OutCirc,
    InOutCirc,
    InElastic,
    OutElastic,
    OutElasticHalf,
    OutElasticQuarter,
    InOutElastic,
    InBack,
    OutBack,
    InOutBack,
    InBounce,
    OutBounce,
    InOutBounce;

    /**
     * Applies this easing function to a normalized progress value.
     *
     * @param progress The linear progress in the range `[0, 1]`.
     * @return The eased progress. Some easings intentionally overshoot the `[0, 1]` range.
     */
    fun interpolate(progress: Double): Double {
        var time = progress

        return when (this) {
            None -> time

            In, InQuad -> time * time
            Out, OutQuad -> time * (2 - time)
            InOutQuad -> if (time < 0.5) time * time * 2 else { time -= 1; time * time * -2 + 1 }

            InCubic -> time * time * time
            OutCubic -> { time -= 1; time * time * time + 1 }
            InOutCubic ->
                if (time < 0.5) time * time * time * 4
                else { time -= 1; time * time * time * 4 + 1 }

            InQuart -> time * time * time * time
            OutQuart -> { time -= 1; 1 - time * time * time * time }
            InOutQuart ->
                if (time < 0.5) time * time * time * time * 8
                else { time -= 1; time * time * time * time * -8 + 1 }

            InQuint -> time * time * time * time * time
            OutQuint -> { time -= 1; time * time * time * time * time + 1 }
            InOutQuint ->
                if (time < 0.5) time * time * time * time * time * 16
                else { time -= 1; time * time * time * time * time * 16 + 1 }

            InSine -> 1 - cos(time * PI * 0.5)
            OutSine -> sin(time * PI * 0.5)
            InOutSine -> 0.5 - 0.5 * cos(PI * time)

            InExpo -> 2.0.pow(10 * (time - 1)) + EXPO_OFFSET * (time - 1)
            OutExpo -> -(2.0.pow(-10 * time)) + 1 + EXPO_OFFSET * time
            InOutExpo ->
                if (time < 0.5) 0.5 * (2.0.pow(20 * time - 10) + EXPO_OFFSET * (2 * time - 1))
                else 1 - 0.5 * (2.0.pow(-20 * time + 10) + EXPO_OFFSET * (-2 * time + 1))

            InCirc -> 1 - sqrt(1 - time * time)
            OutCirc -> { time -= 1; sqrt(1 - time * time) }
            InOutCirc -> {
                time *= 2
                if (time < 1) 0.5 - 0.5 * sqrt(1 - time * time)
                else { time -= 2; 0.5 * sqrt(1 - time * time) + 0.5 }
            }

            InElastic ->
                -(2.0.pow(-10 + 10 * time)) * sin((1 - ELASTIC_CONST2 - time) * ELASTIC_CONST) +
                    ELASTIC_OFFSET_FULL * (1 - time)

            OutElastic ->
                2.0.pow(-10 * time) * sin((time - ELASTIC_CONST2) * ELASTIC_CONST) +
                    1 - ELASTIC_OFFSET_FULL * time

            OutElasticHalf ->
                2.0.pow(-10 * time) * sin((0.5 * time - ELASTIC_CONST2) * ELASTIC_CONST) +
                    1 - ELASTIC_OFFSET_HALF * time

            OutElasticQuarter ->
                2.0.pow(-10 * time) * sin((0.25 * time - ELASTIC_CONST2) * ELASTIC_CONST) +
                    1 - ELASTIC_OFFSET_QUARTER * time

            InOutElastic -> {
                time *= 2
                if (time < 1) {
                    -0.5 * (
                        2.0.pow(-10 + 10 * time) * sin((1 - ELASTIC_CONST2 * 1.5 - time) * ELASTIC_CONST / 1.5) -
                            IN_OUT_ELASTIC_OFFSET * (1 - time)
                    )
                } else {
                    time -= 1
                    0.5 * (
                        2.0.pow(-10 * time) * sin((time - ELASTIC_CONST2 * 1.5) * ELASTIC_CONST / 1.5) +
                            2 - IN_OUT_ELASTIC_OFFSET * time
                    )
                }
            }

            InBack -> time * time * ((BACK_CONST + 1) * time - BACK_CONST)
            OutBack -> { time -= 1; time * time * ((BACK_CONST + 1) * time + BACK_CONST) + 1 }
            InOutBack -> {
                time *= 2
                if (time < 1) 0.5 * time * time * ((BACK_CONST2 + 1) * time - BACK_CONST2)
                else { time -= 2; 0.5 * (time * time * ((BACK_CONST2 + 1) * time + BACK_CONST2) + 2) }
            }

            InBounce -> 1 - OutBounce.interpolate(1 - time)

            OutBounce -> when {
                time < BOUNCE_CONST -> 7.5625 * time * time
                time < 2 * BOUNCE_CONST -> { time -= 1.5 * BOUNCE_CONST; 7.5625 * time * time + 0.75 }
                time < 2.5 * BOUNCE_CONST -> { time -= 2.25 * BOUNCE_CONST; 7.5625 * time * time + 0.9375 }
                else -> { time -= 2.625 * BOUNCE_CONST; 7.5625 * time * time + 0.984375 }
            }

            InOutBounce ->
                if (time < 0.5) 0.5 - 0.5 * OutBounce.interpolate(1 - time * 2)
                else OutBounce.interpolate((time - 0.5) * 2) * 0.5 + 0.5
        }
    }

    companion object {
        private const val ELASTIC_CONST = 2 * PI / 0.3
        private const val ELASTIC_CONST2 = 0.3 / 4
        private const val BACK_CONST = 1.70158
        private const val BACK_CONST2 = BACK_CONST * 1.525
        private const val BOUNCE_CONST = 1 / 2.75

        // Constants used to fix expo and elastic curves to start/end at 0 and 1.
        private val EXPO_OFFSET = 2.0.pow(-10)
        private val ELASTIC_OFFSET_FULL = 2.0.pow(-11)
        private val ELASTIC_OFFSET_HALF = 2.0.pow(-10) * sin((0.5 - ELASTIC_CONST2) * ELASTIC_CONST)
        private val ELASTIC_OFFSET_QUARTER = 2.0.pow(-10) * sin((0.25 - ELASTIC_CONST2) * ELASTIC_CONST)
        private val IN_OUT_ELASTIC_OFFSET = 2.0.pow(-10) * sin((1 - ELASTIC_CONST2 * 1.5) * ELASTIC_CONST / 1.5)

        /**
         * Parses an easing from its numeric representation.
         *
         * Out-of-range values fall back to [None], matching osu!stable behavior.
         *
         * @param value The value to parse.
         * @return The parsed [StoryboardEasing].
         */
        @JvmStatic
        fun parse(value: Int) = entries.getOrElse(value) { None }
    }
}
