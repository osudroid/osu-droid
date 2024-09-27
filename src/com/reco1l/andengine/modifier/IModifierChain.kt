package com.reco1l.andengine.modifier

import com.edlplan.framework.easing.Easing
import com.edlplan.ui.EasingHelper
import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.ModifierType.*
import org.anddev.andengine.util.modifier.ease.*


/**
 * A chain of modifiers that can be applied to an entity.
 *
 * An entity is the first node in the chain, and each modifier is a node that follows it.
 */
interface IModifierChain {


    /**
     * In a modifier chain, this is the entity that will be modified.
     */
    var modifierChainTarget: ExtendedEntity?


    /**
     * Obtains a modifier from the modifier pool or creates a new one.
     *
     * Implementations should override this method to support their own modifier pools
     * as well register the modifier as nested to the current chain and use the [block]
     * callback on it.
     */
    fun applyModifier(block: (UniversalModifier) -> Unit): UniversalModifier


    // Nested chains

    /**
     * Begins a sequential chain of modifiers.
     */
    fun beginSequenceChain(delay: Float = 0f, onFinished: OnModifierFinished? = null, block: UniversalModifier.() -> Unit): UniversalModifier {

        if (modifierChainTarget == null) {
            throw IllegalStateException("Modifier target is not set cannot apply modifier.")
        }

        return applyModifier {
            it.type = SEQUENCE
            it.onFinished = onFinished

            if (delay > 0) {
                it.applyModifier { delayed ->
                    delayed.type = NONE
                    delayed.duration = delay
                }

                it.applyModifier { inner ->
                    inner.type = SEQUENCE
                    inner.block()
                }
            } else {
                it.block()
            }
        }
    }

    /**
     * Begins a parallel chain of modifiers.
     */
    fun beginParallelChain(delay: Float = 0f, onFinished: OnModifierFinished? = null, block: UniversalModifier.() -> Unit): UniversalModifier {

        if (modifierChainTarget == null) {
            throw IllegalStateException("Modifier target is not set cannot apply modifier.")
        }

        return applyModifier {
            it.type = if (delay > 0f) SEQUENCE else PARALLEL
            it.onFinished = onFinished

            if (delay > 0) {
                it.applyModifier { delayed ->
                    delayed.type = NONE
                    delayed.duration = delay
                }

                it.applyModifier { inner ->
                    inner.type = PARALLEL
                    inner.block()
                }
            } else {
                it.block()
            }
        }
    }


    // Delay

    fun delay(durationSec: Float, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(NONE, durationSec, null, onFinished)
    }


    // Translate

    fun translateTo(axes: Axes = Axes.Both, value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(TRANSLATE, durationSec, easing, onFinished,
                modifierChainTarget!!.translationX, value,
                modifierChainTarget!!.translationY, value
            )

            Axes.X -> apply(TRANSLATE_X, durationSec, easing, onFinished, modifierChainTarget!!.translationX, value)
            Axes.Y -> apply(TRANSLATE_Y, durationSec, easing, onFinished, modifierChainTarget!!.translationY, value)

            Axes.None -> throw IllegalArgumentException("Cannot translate to none axes.")
        }
    }

    fun translateTo(x: Float, y: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(TRANSLATE, durationSec, easing, onFinished,
            modifierChainTarget!!.translationX, x,
            modifierChainTarget!!.translationY, y
        )
    }


    // Move

    fun moveTo(axes: Axes = Axes.Both, x: Float, y: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(MOVE, durationSec, easing, onFinished,
                modifierChainTarget!!.x, x,
                modifierChainTarget!!.y, y
            )

            Axes.X -> apply(MOVE_X, durationSec, easing, onFinished, modifierChainTarget!!.x, x)
            Axes.Y -> apply(MOVE_Y, durationSec, easing, onFinished, modifierChainTarget!!.y, y)

            Axes.None -> throw IllegalArgumentException("Cannot move to none axes.")
        }
    }

    fun moveTo(x: Float, y: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(MOVE, durationSec, easing, onFinished,
            modifierChainTarget!!.x, x,
            modifierChainTarget!!.y, y
        )
    }


    // Scale

    fun scaleTo(axes: Axes = Axes.Both, value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(SCALE, durationSec, easing, onFinished,
                modifierChainTarget!!.scaleX, value,
                modifierChainTarget!!.scaleY, value
            )

            Axes.X -> apply(SCALE_X, durationSec, easing, onFinished, modifierChainTarget!!.scaleX, value)
            Axes.Y -> apply(SCALE_Y, durationSec, easing, onFinished, modifierChainTarget!!.scaleY, value)

            Axes.None -> throw IllegalArgumentException("Cannot scale to none axes.")
        }
    }

    fun scaleTo(x: Float, y: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(SCALE, durationSec, easing, onFinished,
            modifierChainTarget!!.scaleX, x,
            modifierChainTarget!!.scaleY, y
        )
    }


    // Coloring

    fun fadeTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(ALPHA, durationSec, easing, onFinished, modifierChainTarget!!.alpha, value)
    }

    fun fadeIn(durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return fadeTo(1f, durationSec, easing, onFinished)
    }

    fun fadeOut(durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return fadeTo(0f, durationSec, easing, onFinished)
    }

    fun colorTo(red: Float, green: Float, blue: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(COLOR, durationSec, easing, onFinished,
            modifierChainTarget!!.red, red,
            modifierChainTarget!!.green, green,
            modifierChainTarget!!.blue, blue
        )
    }

    fun colorTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(COLOR, durationSec, easing, onFinished,
            modifierChainTarget!!.red, value,
            modifierChainTarget!!.green, value,
            modifierChainTarget!!.blue, value
        )
    }


    // Rotation

    fun rotateTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(ROTATION, durationSec, easing, onFinished, modifierChainTarget!!.rotation, value)
    }



    private fun apply(type: ModifierType, durationSec: Float, easing: Easing?, onFinished: OnModifierFinished?, vararg values: Float): UniversalModifier {

        if (modifierChainTarget == null) {
            throw IllegalStateException("Modifier target is not set cannot apply modifier.")
        }

        return applyModifier {
            it.type = type
            it.values = values
            it.onFinished = onFinished
            it.easeFunction = EasingHelper.asEaseFunction(easing ?: Easing.None)
            it.duration = durationSec
        }
    }

}