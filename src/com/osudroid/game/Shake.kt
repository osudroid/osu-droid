package com.osudroid.game

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.component.UIComponent
import com.rian.andengine.modifier.ModifierType

/**
 * The duration of a single shake, in seconds. This is the value osu!lazer uses for hit circles and sliders.
 */
const val DEFAULT_SHAKE_DURATION = 0.03f

/**
 * Shakes this [UIComponent] horizontally, mirroring osu!lazer's `DrawableExtensions.Shake`.
 *
 * The shake animates [UIComponent.translationX] using the [com.rian.andengine.modifier.UniversalModifier] system. As
 * the translation is independent of [UIComponent.x], it does not interfere with code that positions the
 * [UIComponent] itself.
 *
 * Calling this while the [UIComponent] is already shaking restarts the shake.
 *
 * @param magnitude The displacement of the shake, in pixels.
 * @param shakeDuration The duration of a single shake, in seconds. The whole animation takes 4 times as long.
 */
@JvmOverloads
fun UIComponent.shake(magnitude: Float, shakeDuration: Float = DEFAULT_SHAKE_DURATION) {
    // Finish any running shake so the translation is back at its resting value before a new one starts.
    finishModifiers(false, ModifierType.TranslateX)

    beginModifierSequence {
        translateToX(magnitude, shakeDuration / 2, Easing.OutSine)
        then().translateToX(-magnitude, shakeDuration, Easing.InOutSine)
        then().translateToX(magnitude, shakeDuration, Easing.InOutSine)
        then().translateToX(-magnitude, shakeDuration, Easing.InOutSine)
        then().translateToX(0f, shakeDuration / 2, Easing.InSine)
    }
}
