package com.reco1l.framework

import com.edlplan.framework.easing.*
import com.edlplan.framework.easing.Easing.*
import com.reco1l.toolkt.*
import kotlin.math.*


// Extracted from com.edlplan.framework.easing.EasingManager

// Instead of using EasingManager which creates instances of EasingInterpolator
// per Easing type we just inlined them.

fun Easing.interpolate(value: Float): Float {

    var n = value

    return when (this) {

        In, InQuad -> {
            n * n
        }

        Out, OutQuad -> {
            n * (2f - n)
        }

        InOutQuad -> {
            if (n < 0.5f) n * n * 2f else --n * n * -2f + 1f
        }

        InCubic -> {
            n * n * n
        }

        OutCubic -> {
            --n * n * n + 1f
        }

        InOutCubic -> {
            if (n < 0.5f) n * n * n * 4f else --n * n * n * 4f + 1f
        }

        InQuart -> {
            n * n * n * n
        }

        OutQuart -> {
            1f - --n * n * n * n
        }

        InOutQuart -> {
            if (n < 0.5f) n * n * n * n * 8 else --n * n * n * n * -8f + 1f
        }

        InQuint -> {
            n * n * n * n * n
        }

        OutQuint -> {
            --n * n * n * n * n + 1
        }

        InOutQuint -> {
            if (n < 0.5f) n * n * n * n * n * 16f else --n * n * n * n * n * 16f + 1f
        }

        InSine -> {
            1f - cos(n * MathF.PI * 0.5f)
        }

        OutSine -> {
            sin(n * MathF.PI * 0.5f)
        }

        InOutSine -> {
            0.5f - 0.5f * cos(MathF.PI * n)
        }

        InExpo -> {
            2f.pow(10f * (n - 1f))
        }

        OutExpo -> {
            -(2f.pow(-10f * n)) + 1f
        }

        InOutExpo -> {
            if (n < 0.5f) 0.5f * 2f.pow(20f * n - 10f) else 1f - 0.5f * 2f.pow(-20f * n + 10f)
        }

        InCirc -> {
            1f - sqrt(1f - n * n)
        }

        OutCirc -> {
            sqrt(1f - --n * n)
        }

        InOutCirc -> {
            if (let { n *= 2f; n } < 1f) {
                0.5f - 0.5f * sqrt(1f - n * n)
            } else {
                0.5f * sqrt(1f - let { n -= 2f; n } * n) + 0.5f
            }
        }

        InElastic -> {
            -(2f.pow(-10f + 10f * n)) * sin((1f - 0.3f / 4f - n) * (2f * MathF.PI / 0.3f))
        }

        OutElastic -> {
            2f.pow(-10f * n) * sin((n - 0.3f / 4f) * (2f * MathF.PI / 0.3f)) + 1f
        }

        OutElasticHalf -> {
            2f.pow(-10 * n) * sin((0.5f * n - 0.3f / 4f) * (2f * MathF.PI / 0.3f)) + 1f
        }

        OutElasticQuarter -> {
            2f.pow(-10f * n) * sin((0.25f * n - 0.3f / 4f) * (2f * MathF.PI / 0.3f)) + 1f
        }

        InOutElastic -> {
            if (let { n *= 2f; n } < 1f) {
                -0.5f * 2f.pow(-10f + 10f * n) * sin((1f - 0.3f / 4f * 1.5f - n) * (2f * MathF.PI / 0.3f) / 1.5f)
            } else {
                0.5f * 2f.pow(-10f * --n) * sin((n - 0.3f / 4f * 1.5f) * (2f * MathF.PI / 0.3f) / 1.5f) + 1f
            }
        }

        InBack -> {
            n * n * ((1.70158f + 1) * n - 1.70158f)
        }

        OutBack -> {
            --n * n * ((1.70158f + 1f) * n + 1.70158f) + 1f
        }

        InOutBack -> {
            if (let { n *= 2f; n } < 1f) {
                0.5f * n * n * ((1.70158f * 1.525f + 1f) * n - 1.70158f * 1.525f)
            } else {
                0.5f * (let { n -= 2f; n } * n * ((1.70158f * 1.525f + 1f) * n + 1.70158f * 1.525f) + 2f)
            }
        }

        InBounce -> {
            n = 1 - n
            when {
                n < 1f / 2.75f -> 1f - 7.5625f * n * n

                n < 2f * (1f / 2.75f) -> 1f - (7.5625f * let { n -= 1.5f * (1f / 2.75f); n } * n + 0.75f)

                n < 2.5f * (1f / 2.75f) -> 1f - (7.5625f * let { n -= 2.25f * (1f / 2.75f); n } * n + 0.9375f)

                else -> 1f - (7.5625f * let { n -= 2.625f * (1f / 2.75f); n } * n + 0.984375f)
            }
        }

        OutBounce -> {
            when {
                n < 1f / 2.75f -> 7.5625f * n * n

                n < 2 * (1f / 2.75f) -> 7.5625f * let { n -= 1.5f * (1f / 2.75f); n } * n + 0.75f

                n < 2.5 * (1f / 2.75f) -> 7.5625f * let { n -= 2.25f * (1f / 2.75f); n } * n + 0.9375f

                else -> 7.5625f * let { n -= 2.625f * (1f / 2.75f); n } * n + 0.984375f
            }
        }

        InOutBounce -> {
            if (n < 0.5f) {
                0.5f - 0.5f * OutBounce.interpolate(1f - n * 2f)
            } else {
                OutBounce.interpolate((n - 0.5f) * 2f) * 0.5f + 0.5f
            }
        }

        OutPow10 -> {
            --n * n.pow(10f) + 1f
        }

        else -> n
    }
}