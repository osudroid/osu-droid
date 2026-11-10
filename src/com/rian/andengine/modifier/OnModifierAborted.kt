package com.rian.andengine.modifier

import com.reco1l.andengine.component.UIComponent

/**
 * A callback interface for when a [UniversalModifier] is removed from a [UniversalModifierTargetTracker] before it
 * completes (e.g. because it was overridden by another [UniversalModifier] of the same [ModifierType] or cleared).
 */
fun interface OnModifierAborted {
    operator fun invoke(component: UIComponent)
}
