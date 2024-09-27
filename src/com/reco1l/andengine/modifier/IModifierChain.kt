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
     * Begins a delayed chain.
     * In a [ExtendedEntity] this should be called instead of [delay].
     */
    fun beginDelayChain(durationSec: Float, builder: IModifierChainBlock): UniversalModifier {
        return applyModifier {
            it.type = NONE
            it.duration = durationSec
            builder.apply { it.build() }
        }
    }

    /**
     * Begins a sequential chain of modifiers.
     */
    fun beginSequenceChain(builder: IModifierChainBlock): UniversalModifier {
        return applyModifier {
            it.type = SEQUENCE
            builder.apply { it.build() }
        }
    }

    /**
     * Begins a parallel chain of modifiers.
     */
    fun beginParallelChain(builder: IModifierChainBlock): UniversalModifier {
        return applyModifier {
            it.type = PARALLEL
            builder.apply { it.build() }
        }
    }


    // Delay

    fun delay(durationSec: Float): UniversalModifier {
        return apply(NONE, durationSec)
    }


    // Translate

    fun translateTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(TRANSLATE, durationSec,
            target.translationX, value,
            target.translationY, value
        )
    }

    fun translateTo(axes: Axes = Axes.Both, value: Float, durationSec: Float = 0f): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(TRANSLATE, durationSec,
                target.translationX, value,
                target.translationY, value
            )

            Axes.X -> apply(TRANSLATE_X, durationSec, target.translationX, value)
            Axes.Y -> apply(TRANSLATE_Y, durationSec, target.translationY, value)

            Axes.None -> throw IllegalArgumentException("Cannot translate to none axes.")
        }
    }

    fun translateTo(x: Float, y: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(TRANSLATE, durationSec,
            target.translationX, x,
            target.translationY, y
        )
    }


    // Move

    fun moveTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(MOVE, durationSec,
            target.x, value,
            target.y, value
        )
    }

    fun moveTo(axes: Axes = Axes.Both, x: Float, y: Float, durationSec: Float = 0f): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(MOVE, durationSec,
                target.x, x,
                target.y, y
            )

            Axes.X -> apply(MOVE_X, durationSec, target.x, x)
            Axes.Y -> apply(MOVE_Y, durationSec, target.y, y)

            Axes.None -> throw IllegalArgumentException("Cannot move to none axes.")
        }
    }

    fun moveTo(x: Float, y: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(MOVE, durationSec,
            target.x, x,
            target.y, y
        )
    }


    // Scale

    fun scaleTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(SCALE, durationSec,
            target.scaleX, value,
            target.scaleY, value
        )
    }

    fun scaleTo(axes: Axes = Axes.Both, value: Float, durationSec: Float = 0f): UniversalModifier {
        return when (axes) {

            Axes.Both -> apply(SCALE, durationSec,
                target.scaleX, value,
                target.scaleY, value
            )

            Axes.X -> apply(SCALE_X, durationSec, target.scaleX, value)
            Axes.Y -> apply(SCALE_Y, durationSec, target.scaleY, value)

            Axes.None -> throw IllegalArgumentException("Cannot scale to none axes.")
        }
    }

    fun scaleTo(x: Float, y: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(SCALE, durationSec,
            target.scaleX, x,
            target.scaleY, y
        )
    }


    // Coloring

    fun fadeTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(ALPHA, durationSec, target.alpha, value)
    }

    fun fadeIn(durationSec: Float = 0f): UniversalModifier {
        return fadeTo(1f, durationSec)
    }

    fun fadeOut(durationSec: Float = 0f): UniversalModifier {
        return fadeTo(0f, durationSec)
    }

    fun colorTo(red: Float, green: Float, blue: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(COLOR, durationSec,
            target.red, red,
            target.green, green,
            target.blue, blue
        )
    }

    fun colorTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(COLOR, durationSec,
            target.red, value,
            target.green, value,
            target.blue, value
        )
    }


    // Rotation

    fun rotateTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return apply(ROTATION, durationSec, target.rotation, value)
    }


    // Private

    private fun apply(type: ModifierType, durationSec: Float, vararg values: Float): UniversalModifier {
        return applyModifier {
            it.type = type
            it.values = values
            it.duration = durationSec
        }
    }

}