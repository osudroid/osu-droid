package com.reco1l.andengine.modifier

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.modifier.ModifierType.*
import com.reco1l.framework.*


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
     *
     * @param block The block of code to execute on the newly created modifier.
     */
    fun appendModifier(block: UniversalModifier.() -> Unit): UniversalModifier


    // Multiple

    /**
     * Begins a parallel block of modifiers.
     */
    fun beginParallel(block: UniversalModifier.() -> Unit): UniversalModifier {
        return appendModifier {
            type = Parallel
            block()
        }
    }

    /**
     * Begins a sequential block of modifiers.
     */
    fun beginSequence(block: UniversalModifier.() -> Unit): UniversalModifier {
        return appendModifier {
            type = Sequence
            block()
        }
    }


    // Delay

    /**
     * Delays the execution of the next modifier in the chain.
     */
    fun delay(durationSec: Float): UniversalModifier {
        return appendModifier {
            type = Delay
            duration = durationSec
        }
    }


    // Translate

    fun translateTo(valueX: Float, valueY: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = TranslateXY
            duration = durationSec
            finalValues = floatArrayOf(valueX, valueY)
            eased(easing)
        }
    }

    fun translateToX(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = TranslateX
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

    fun translateToY(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = TranslateY
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }


    // Move

    fun moveTo(valueX: Float, valueY: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = MoveXY
            duration = durationSec
            finalValues = floatArrayOf(valueX, valueY)
            eased(easing)
        }
    }

    fun moveToX(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = MoveX
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

    fun moveToY(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = MoveY
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }


    // Scale

    fun scaleTo(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = ScaleXY
            duration = durationSec
            finalValues = floatArrayOf(value, value)
            eased(easing)
        }
    }

    fun scaleToX(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = ScaleX
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

    fun scaleToY(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = ScaleY
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }


    // Coloring

    fun fadeTo(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = Alpha
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }

    fun fadeIn(durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return fadeTo(1f, durationSec, easing)
    }

    fun fadeInFromZero(durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = Alpha
            duration = durationSec
            initialValues = floatArrayOf(0f)
            finalValues = floatArrayOf(1f)
            eased(easing)
        }
    }

    fun fadeOut(durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return fadeTo(0f, durationSec, easing)
    }


    fun colorTo(color: Long, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return colorTo(Color4(color), durationSec, easing)
    }

    fun colorTo(color: Color4, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return colorTo(color.red, color.green, color.blue, durationSec, easing)
    }

    fun colorTo(red: Float, green: Float, blue: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = Color
            duration = durationSec
            finalValues = floatArrayOf(red, green, blue)
            eased(easing)
        }
    }


    // Rotation

    fun rotateTo(value: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = Rotation
            duration = durationSec
            finalValues = floatArrayOf(value)
            eased(easing)
        }
    }


    // Size
    fun sizeTo(width: Float, height: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = SizeXY
            duration = durationSec
            finalValues = floatArrayOf(width, height)
            eased(easing)
        }
    }

    fun sizeToX(width: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = SizeX
            duration = durationSec
            finalValues = floatArrayOf(width)
            eased(easing)
        }
    }

    fun sizeToY(height: Float, durationSec: Float = 0f, easing: Easing = Easing.None): UniversalModifier {
        return appendModifier {
            type = SizeY
            duration = durationSec
            finalValues = floatArrayOf(height)
            eased(easing)
        }
    }

}