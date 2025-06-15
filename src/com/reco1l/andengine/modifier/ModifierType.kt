package com.reco1l.andengine.modifier

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import org.anddev.andengine.entity.*


/**
 * The type of the modifier.
 */
enum class ModifierType {

    /**
     * Modifies the entity's X scale value.
     */
    ScaleX,

    /**
     * Modifies the entity's Y scale value.
     */
    ScaleY,

    /**
     * Modifies the entity's X and Y scale values.
     */
    ScaleXY,

    /**
     * Modifies the entity's alpha value.
     */
    Alpha,

    /**
     * Modifies the entity's color value.
     */
    Color,

    /**
     * Modifies the entity's X position.
     */
    MoveX,

    /**
     * Modifies the entity's Y position.
     */
    MoveY,

    /**
     * Modifies the entity's X and Y position.
     */
    MoveXY,

    /**
     * Modifies the entity's X translation.
     *
     * Note: This is only available for [UIComponent] instances.
     */
    TranslateX,

    /**
     * Modifies the entity's Y translation.
     *
     * Note: This is only available for [UIComponent] instances.
     */
    TranslateY,

    /**
     * Modifies the entity's X and Y translation.
     *
     * Note: This is only available for [UIComponent] instances.
     */
    TranslateXY,

    /**
     * Modifies the entity's rotation.
     */
    Rotation,

    /**
     * Modifies the entity's with inner modifiers in sequence.
     */
    Sequence,

    /**
     * Modifies the entity's with inner modifiers in parallel.
     */
    Parallel,

    SizeX,

    SizeY,

    SizeXY,

    /**
     * Does nothing, used as a delay modifier.
     */
    Delay;


    /**
     * Whether this modifier type uses nested modifiers.
     */
    val isCompoundModifier
        get() = this == Sequence || this == Parallel


    fun getInitialValues(entity: IEntity): FloatArray? = when (this) {

        ScaleX -> floatArrayOf(entity.scaleX)
        ScaleY -> floatArrayOf(entity.scaleY)
        ScaleXY -> floatArrayOf(entity.scaleX, entity.scaleY)

        Alpha -> floatArrayOf(entity.alpha)
        Color -> floatArrayOf(entity.red, entity.green, entity.blue)

        MoveX -> floatArrayOf(entity.x)
        MoveY -> floatArrayOf(entity.y)
        MoveXY -> floatArrayOf(entity.x, entity.y)

        SizeX -> {
            entity as? UIComponent ?: throw IllegalArgumentException("SizeX is only available for ExtendedEntity instances.")
            floatArrayOf(entity.width)
        }

        SizeY -> {
            entity as? UIComponent ?: throw IllegalArgumentException("SizeY is only available for ExtendedEntity instances.")
            floatArrayOf(entity.height)
        }

        SizeXY -> {
            entity as? UIComponent ?: throw IllegalArgumentException("SizeXY is only available for ExtendedEntity instances.")
            floatArrayOf(entity.width, entity.height)
        }

        TranslateX -> {
            entity as? UIComponent ?: throw IllegalArgumentException("TranslateX is only available for ExtendedEntity instances.")
            floatArrayOf(entity.translationX)
        }

        TranslateY -> {
            entity as? UIComponent ?: throw IllegalArgumentException("TranslateX is only available for ExtendedEntity instances.")
            floatArrayOf(entity.translationY)
        }

        TranslateXY -> {
            entity as? UIComponent ?: throw IllegalArgumentException("TranslateX is only available for ExtendedEntity instances.")
            floatArrayOf(entity.translationX, entity.translationY)
        }

        Rotation -> floatArrayOf(entity.rotation)

        else -> null
    }

    fun setValues(entity: IEntity, initialValues: FloatArray, finalValues: FloatArray, percentage: Float) {

        fun valueAt(index: Int): Float {
            var i = index

            // This is a workaround for the case when the value count doesn't fit the type requirements.
            // `initialValues` shouldn't be settable as they should be calculated from the entity but since
            // we're supporting `Modifiers.kt` yet we need to handle this case.
            i %= initialValues.size
            i %= finalValues.size

            return initialValues[i] + percentage * (finalValues[i] - initialValues[i])
        }

        when (this) {

            ScaleX -> entity.scaleX = valueAt(0)
            ScaleY -> entity.scaleY = valueAt(0)
            ScaleXY -> entity.setScale(valueAt(0), valueAt(1))

            Alpha -> entity.alpha = valueAt(0)
            Color -> entity.setColor(valueAt(0), valueAt(1), valueAt(2))

            MoveX -> entity.setPosition(valueAt(0), entity.y)
            MoveY -> entity.setPosition(entity.x, valueAt(0))
            MoveXY -> entity.setPosition(valueAt(0), valueAt(1))

            TranslateX -> {
                entity as? UIComponent ?: throw IllegalArgumentException("TranslateX is only available for ExtendedEntity instances.")
                entity.translationX = valueAt(0)
            }

            TranslateY -> {
                entity as? UIComponent ?: throw IllegalArgumentException("TranslateY is only available for ExtendedEntity instances.")
                entity.translationY = valueAt(0)
            }

            TranslateXY -> {
                entity as? UIComponent ?: throw IllegalArgumentException("TranslateXY is only available for ExtendedEntity instances.")
                entity.translationX = valueAt(0)
                entity.translationY = valueAt(1)
            }

            SizeX -> {
                entity as? UIComponent ?: throw IllegalArgumentException("SizeX is only available for ExtendedEntity instances.")
                entity.width = valueAt(0)
            }

            SizeY -> {
                entity as? UIComponent ?: throw IllegalArgumentException("SizeY is only available for ExtendedEntity instances.")
                entity.height = valueAt(0)
            }

            SizeXY -> {
                entity as? UIComponent ?: throw IllegalArgumentException("SizeXY is only available for ExtendedEntity instances.")
                entity.setSize(valueAt(0), valueAt(1))
            }

            Rotation -> entity.rotation = valueAt(0)

            else -> Unit
        }
    }


}