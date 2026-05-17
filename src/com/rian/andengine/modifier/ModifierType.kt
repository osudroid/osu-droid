package com.rian.andengine.modifier

import com.reco1l.andengine.component.*

/**
 * The type of the [UniversalModifier].
 *
 * @author Reco1I, Rian8337
 */
enum class ModifierType(
    /**
     * The target member of this [ModifierType].
     *
     * This is used to group [ModifierType]s that operate on the same property (or properties) of a [UIComponent]. It is
     * assumed that [UniversalModifier]s with different [targetMember]s are independent of each other in that they
     * affect different properties, and therefore they can be applied independently in any order without affecting the
     * end result.
     */
    val targetMember: String?
) {
    None(null),

    /**
     * Modifies the [UIComponent]'s X scale value.
     */
    ScaleX("scale"),

    /**
     * Modifies the [UIComponent]'s Y scale value.
     */
    ScaleY("scale"),

    /**
     * Modifies the [UIComponent]'s X and Y scale values.
     */
    ScaleXY("scale"),

    /**
     * Modifies the [UIComponent]'s alpha value.
     */
    Alpha("alpha"),

    /**
     * Modifies the [UIComponent]'s color value.
     */
    Color("color"),

    /**
     * Modifies the [UIComponent]'s X position.
     */
    MoveX("move"),

    /**
     * Modifies the [UIComponent]'s Y position.
     */
    MoveY("move"),

    /**
     * Modifies the [UIComponent]'s X and Y position.
     */
    MoveXY("move"),

    /**
     * Modifies the [UIComponent]'s X translation.
     */
    TranslateX("translate"),

    /**
     * Modifies the [UIComponent]'s Y translation.
     */
    TranslateY("translate"),

    /**
     * Modifies the [UIComponent]'s X and Y translation.
     */
    TranslateXY("translate"),

    /**
     * Modifies the [UIComponent]'s rotation.
     */
    Rotation("rotation"),

    /**
     * Modifies the [UIComponent]'s width.
     */
    Width("size"),

    /**
     * Modifies the [UIComponent]'s height.
     */
    Height("size"),

    /**
     * Modifies the [UIComponent]'s width and height.
     */
    Size("size");

    fun getInitialValues(entity: UIComponent, reusableArray: FloatArray) = when (this) {
        ScaleX -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.scaleX
            array
        }

        ScaleY -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.scaleY
            array
        }

        ScaleXY -> {
            val array = reusableArray.ensureSize(2)

            array[0] = entity.scaleX
            array[1] = entity.scaleY
            array
        }

        Alpha -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.alpha
            array
        }

        Color -> {
            val array = reusableArray.ensureSize(3)

            array[0] = entity.red
            array[1] = entity.green
            array[2] = entity.blue

            array
        }

        MoveX -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.x
            array
        }

        MoveY -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.y
            array
        }

        MoveXY -> {
            val array = reusableArray.ensureSize(2)

            array[0] = entity.x
            array[1] = entity.y

            array
        }

        Width -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.width
            array
        }

        Height -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.height
            array
        }

        Size -> {
            val array = reusableArray.ensureSize(2)

            array[0] = entity.width
            array[1] = entity.height

            array
        }

        TranslateX -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.translationX
            array
        }

        TranslateY -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.translationY
            array
        }

        TranslateXY -> {
            val array = reusableArray.ensureSize(2)

            array[0] = entity.translationX
            array[1] = entity.translationY
            array
        }

        Rotation -> {
            val array = reusableArray.ensureSize(1)

            array[0] = entity.rotation
            array
        }

        else -> reusableArray
    }

    fun setValues(entity: UIComponent, initialValues: FloatArray, finalValues: FloatArray, percentage: Float) {
        fun valueAt(index: Int) = when {
            percentage >= 1f -> finalValues[index]
            percentage <= 0f -> initialValues[index]
            else -> initialValues[index] + percentage * (finalValues[index] - initialValues[index])
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

            TranslateX -> entity.translationX = valueAt(0)
            TranslateY -> entity.translationY = valueAt(0)

            TranslateXY -> {
                entity.translationX = valueAt(0)
                entity.translationY = valueAt(1)
            }

            Width -> entity.width = valueAt(0)
            Height -> entity.height = valueAt(0)
            Size -> entity.setSize(valueAt(0), valueAt(1))

            Rotation -> entity.rotation = valueAt(0)

            None -> Unit
        }
    }

    private fun FloatArray.ensureSize(size: Int) = this.takeIf { it.size >= size } ?: FloatArray(size)
}