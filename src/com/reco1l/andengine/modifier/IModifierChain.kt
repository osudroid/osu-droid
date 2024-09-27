package com.reco1l.andengine.modifier

import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.ModifierType.*
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.util.modifier.ease.*


/**
 * A chain of modifiers that can be applied to an entity.
 *
 * An entity is the first node in the chain, and each modifier is a node that follows it.
 */
interface IModifierChain {

    /**
     * In a modifier chain, this is the entity that will be modified.
     *
     * For [ExtendedEntity] instances, this will be the entity itself.
     */
    var modifierChainTarget: IEntity?


    /**
     * Obtains a modifier from the modifier pool or creates a new one.
     *
     * Implementations should override this method to support their own modifier pools
     * as well register the modifier as nested to the current chain and use the [block]
     * callback on it.
     */
    fun applyModifier(block: (UniversalModifier) -> Unit): UniversalModifier?


    /**
     * Begins a sequential chain of modifiers.
     */
    fun beginSequenceChain(delay: Float = 0f, onFinished: OnModifierFinished? = null, block: IModifierChain.() -> Unit): IModifierChain {
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
        } ?: this
    }

    /**
     * Begins a parallel chain of modifiers.
     */
    fun beginParallelChain(delay: Float = 0f, onFinished: OnModifierFinished? = null, block: IModifierChain.() -> Unit): IModifierChain {
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
        } ?: this
    }


    fun delay(durationSec: Float, onFinished: OnModifierFinished? = null): IModifierChain {
        apply(NONE, durationSec, null, onFinished)
        return this
    }

    fun translateXTo(x: Float, durationSec: Float = 0f, easing: IEaseFunction? = null, onFinished: OnModifierFinished? = null): IModifierChain {
        if (modifierChainTarget is ExtendedEntity) {
            apply(TRANSLATE_X, durationSec, easing, onFinished, (modifierChainTarget as ExtendedEntity).translationX, x)
        }
        return this
    }

    fun translateYTo(y: Float, durationSec: Float = 0f, easing: IEaseFunction? = null, onFinished: OnModifierFinished? = null): IModifierChain {
        if (modifierChainTarget is ExtendedEntity) {
            apply(TRANSLATE_Y, durationSec, easing, onFinished, (modifierChainTarget as ExtendedEntity).translationY, y)
        }
        return this
    }

    fun translateTo(x: Float, y: Float, durationSec: Float = 0f, easing: IEaseFunction? = null, onFinished: OnModifierFinished? = null): IModifierChain {
        if (modifierChainTarget is ExtendedEntity) {
            apply(TRANSLATE, durationSec, easing, onFinished,
                (modifierChainTarget as ExtendedEntity).translationX, x,
                (modifierChainTarget as ExtendedEntity).translationY, y
            )
        }
        return this
    }

    fun moveTo(x: Float, y: Float, durationSec: Float = 0f, easing: IEaseFunction? = null, onFinished: OnModifierFinished? = null): IModifierChain {
        apply(MOVE, durationSec, easing, onFinished,
            modifierChainTarget!!.x, x,
            modifierChainTarget!!.y, y
        )
        return this
    }

    fun scaleTo(value: Float, durationSec: Float = 0f, easing: IEaseFunction? = null, onFinished: OnModifierFinished? = null): IModifierChain {
        apply(SCALE, durationSec, easing, onFinished,
            modifierChainTarget!!.scaleX, value,
            modifierChainTarget!!.scaleY, value
        )
        return this
    }

    fun fadeTo(value: Float, durationSec: Float = 0f, easing: IEaseFunction? = null, onFinished: OnModifierFinished? = null): IModifierChain {
        apply(ALPHA, durationSec, easing, onFinished, modifierChainTarget!!.alpha, value)
        return this
    }

    fun colorTo(red: Float, green: Float, blue: Float, durationSec: Float = 0f, easing: IEaseFunction? = null, onFinished: OnModifierFinished? = null): IModifierChain {
        apply(RGB, durationSec, easing, onFinished,
            modifierChainTarget!!.red, red,
            modifierChainTarget!!.green, green,
            modifierChainTarget!!.blue, blue
        )
        return this
    }

    fun rotateTo(value: Float, durationSec: Float = 0f, easing: IEaseFunction? = null, onFinished: OnModifierFinished? = null): IModifierChain {
        apply(ROTATION, durationSec, easing, onFinished, modifierChainTarget!!.rotation, value)
        return this
    }


    private fun apply(type: ModifierType, durationSec: Float, easing: IEaseFunction?, onFinished: OnModifierFinished?, vararg values: Float) {

        if (durationSec == 0f) {
            type.onApply?.invoke(modifierChainTarget ?: return, SpanArray(values), 1f)
        }

        applyModifier {
            it.type = type
            it.values = SpanArray(values)
            it.onFinished = onFinished
            it.easeFunction = easing ?: EaseLinear.getInstance()
            it.duration = durationSec
        }
    }

}