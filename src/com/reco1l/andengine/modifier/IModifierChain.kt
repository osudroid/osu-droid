package com.reco1l.andengine.modifier

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
    fun beginSequenceChain(builder: IModifierChainBlock): UniversalModifier {
        return applyModifier {
            it.type = Sequence
            builder.apply { it.build() }
        }
    }

    /**
     * Begins a parallel chain of modifiers.
     */
    fun beginParallelChain(builder: IModifierChainBlock): UniversalModifier {
        return applyModifier {
            it.type = Parallel
            builder.apply { it.build() }
        }
    }


    // Delay

    /**
     * Delays the execution of the next modifier in the chain.
     */
    fun delay(durationSec: Float): UniversalModifier {
        return applyModifier {
            it.type = Delay
            it.duration = durationSec
            it.finalValue = 0f
        }
    }


    // Translate

    fun translateTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return beginParallelChain {
            translateToX(value, durationSec)
            translateToY(value, durationSec)
        }
    }

    fun translateToX(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = TranslateX
            it.duration = durationSec
            it.finalValue = value
        }
    }

    fun translateToY(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = TranslateY
            it.duration = durationSec
            it.finalValue = value
        }
    }


    // Move

    fun moveTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return beginParallelChain {
            moveToX(value, durationSec)
            moveToY(value, durationSec)
        }
    }

    fun moveTo(valueX: Float, valueY: Float, durationSec: Float = 0f): UniversalModifier {
        return beginParallelChain {
            moveToX(valueX, durationSec)
            moveToY(valueY, durationSec)
        }
    }

    fun moveToX(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = MoveX
            it.duration = durationSec
            it.finalValue = value
        }
    }

    fun moveToY(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = MoveY
            it.duration = durationSec
            it.finalValue = value
        }
    }


    // Scale

    fun scaleTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return beginParallelChain {
            scaleToX(value, durationSec)
            scaleToY(value, durationSec)
        }
    }

    fun scaleToX(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = ScaleX
            it.duration = durationSec
            it.finalValue = value
        }
    }

    fun scaleToY(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = ScaleY
            it.duration = durationSec
            it.finalValue = value
        }
    }


    // Coloring

    fun fadeTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = Alpha
            it.duration = durationSec
            it.finalValue = value
        }
    }

    fun fadeIn(durationSec: Float = 0f): UniversalModifier {
        return fadeTo(1f, durationSec)
    }

    fun fadeOut(durationSec: Float = 0f): UniversalModifier {
        return fadeTo(0f, durationSec)
    }

    fun colorTo(red: Float, green: Float, blue: Float, durationSec: Float = 0f): UniversalModifier {
        return beginParallelChain {
            colorToRed(red, durationSec)
            colorToGreen(green, durationSec)
            colorToBlue(blue, durationSec)
        }
    }

    fun colorToRed(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = ColorRed
            it.duration = durationSec
            it.finalValue = value
        }
    }

    fun colorToGreen(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = ColorGreen
            it.duration = durationSec
            it.finalValue = value
        }
    }

    fun colorToBlue(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = ColorBlue
            it.duration = durationSec
            it.finalValue = value
        }
    }


    // Rotation

    fun rotateTo(value: Float, durationSec: Float = 0f): UniversalModifier {
        return applyModifier {
            it.type = Rotation
            it.duration = durationSec
            it.finalValue = value
        }
    }

}