package com.reco1l.andengine.modifier

import com.edlplan.framework.easing.Easing
import com.edlplan.ui.EasingHelper
import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.ModifierType.*


/**
 * A block to execute when a modifier chain is built.
 */
fun interface IModifierChainBlock {
    fun UniversalModifier.build()
}

/**
 * A chain of modifiers that can be applied to an entity.
 *
 * An entity is the first node in the chain, and each modifier is a node that follows it.
 */
interface IModifierChain {


    private val target: ExtendedEntity
        get() = when(this) {

            is ExtendedEntity -> this
            is UniversalModifier -> entity ?: throw IllegalStateException("Modifier target is not set in this UniversalModifier cannot apply modifier.")

            else -> throw IllegalStateException("IModifierChain only works with current implementations (ExtendedEntity and UniversalModifier), you must not use this interface.")
        }


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
    fun beginSequenceChain(onFinished: OnModifierFinished? = null, builder: IModifierChainBlock): UniversalModifier {
        return applyModifier {
            it.type = SEQUENCE
            it.onFinished = onFinished
            builder.apply { it.build() }
        }
    }

    /**
     * Begins a parallel chain of modifiers.
     */
    fun beginParallelChain(onFinished: OnModifierFinished? = null, builder: IModifierChainBlock): UniversalModifier {
        return applyModifier {
            it.type = PARALLEL
            it.onFinished = onFinished
            builder.apply { it.build() }
        }
    }


    // Delay

    fun delay(durationSec: Float, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(NONE, durationSec, null, onFinished)
    }


    // Translate

    fun translateTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(TRANSLATE, durationSec, easing, onFinished,
            target.translationX, value,
            target.translationY, value
        )
    }

    fun translateTo(axes: Axes = Axes.Both, value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(TRANSLATE, durationSec, easing, onFinished,
                target.translationX, value,
                target.translationY, value
            )

            Axes.X -> apply(TRANSLATE_X, durationSec, easing, onFinished, target.translationX, value)
            Axes.Y -> apply(TRANSLATE_Y, durationSec, easing, onFinished, target.translationY, value)

            Axes.None -> throw IllegalArgumentException("Cannot translate to none axes.")
        }
    }

    fun translateTo(x: Float, y: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(TRANSLATE, durationSec, easing, onFinished,
            target.translationX, x,
            target.translationY, y
        )
    }


    // Move

    fun moveTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(MOVE, durationSec, easing, onFinished,
            target.x, value,
            target.y, value
        )
    }

    fun moveTo(axes: Axes = Axes.Both, x: Float, y: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(MOVE, durationSec, easing, onFinished,
                target.x, x,
                target.y, y
            )

            Axes.X -> apply(MOVE_X, durationSec, easing, onFinished, target.x, x)
            Axes.Y -> apply(MOVE_Y, durationSec, easing, onFinished, target.y, y)

            Axes.None -> throw IllegalArgumentException("Cannot move to none axes.")
        }
    }

    fun moveTo(x: Float, y: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(MOVE, durationSec, easing, onFinished,
            target.x, x,
            target.y, y
        )
    }


    // Scale

    fun scaleTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(SCALE, durationSec, easing, onFinished,
            target.scaleX, value,
            target.scaleY, value
        )
    }

    fun scaleTo(axes: Axes = Axes.Both, value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(SCALE, durationSec, easing, onFinished,
                target.scaleX, value,
                target.scaleY, value
            )

            Axes.X -> apply(SCALE_X, durationSec, easing, onFinished, target.scaleX, value)
            Axes.Y -> apply(SCALE_Y, durationSec, easing, onFinished, target.scaleY, value)

            Axes.None -> throw IllegalArgumentException("Cannot scale to none axes.")
        }
    }

    fun scaleTo(x: Float, y: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(SCALE, durationSec, easing, onFinished,
            target.scaleX, x,
            target.scaleY, y
        )
    }


    // Coloring

    fun fadeTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(ALPHA, durationSec, easing, onFinished, target.alpha, value)
    }

    fun fadeIn(durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return fadeTo(1f, durationSec, easing, onFinished)
    }

    fun fadeOut(durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return fadeTo(0f, durationSec, easing, onFinished)
    }

    fun colorTo(red: Float, green: Float, blue: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(COLOR, durationSec, easing, onFinished,
            target.red, red,
            target.green, green,
            target.blue, blue
        )
    }

    fun colorTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(COLOR, durationSec, easing, onFinished,
            target.red, value,
            target.green, value,
            target.blue, value
        )
    }


    // Rotation

    fun rotateTo(value: Float, durationSec: Float = 0f, easing: Easing? = null, onFinished: OnModifierFinished? = null): UniversalModifier {
        return apply(ROTATION, durationSec, easing, onFinished, target.rotation, value)
    }



    private fun apply(type: ModifierType, durationSec: Float, easing: Easing?, onFinished: OnModifierFinished?, vararg values: Float): UniversalModifier {
        return applyModifier {
            it.type = type
            it.values = values
            it.onFinished = onFinished
            it.easeFunction = EasingHelper.asEaseFunction(easing ?: Easing.None)
            it.duration = durationSec
        }
    }

}