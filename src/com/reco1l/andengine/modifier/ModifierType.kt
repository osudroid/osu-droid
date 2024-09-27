package com.reco1l.andengine.modifier

import com.reco1l.andengine.*
import org.anddev.andengine.entity.IEntity


private operator fun FloatArray.get(index: Int, percentage: Float): Float {
    val from = this[2 * index]
    val to = this[2 * index + 1]
    return from + (to - from) * percentage
}

private val FloatArray.spanCount
    get() = size / 2


/**
 * The type of the modifier.
 */
enum class ModifierType(val onApply: ((entity: IEntity, values: FloatArray, percentage: Float) -> Unit)? = null) {

    /**
     * Modifies the entity's alpha value.
     */
    ALPHA({ entity, values, percentage ->
        entity.alpha = values[0, percentage]
    }),

    /**
     * Modifies the entity's scale values for both axis.
     */
    SCALE({ entity, values, percentage ->
        if (values.spanCount == 1) {
            entity.scaleX = values[0, percentage]
            entity.scaleY = values[0, percentage]
        } else {
            entity.scaleX = values[0, percentage]
            entity.scaleY = values[1, percentage]
        }
    }),

    /**
     * Modifies the entity's X scale value.
     */
    SCALE_X({ entity, values, percentage ->
        entity.scaleX = values[0, percentage]
    }),

    /**
     * Modifies the entity's Y scale value.
     */
    SCALE_Y({ entity, values, percentage ->
        entity.scaleY = values[0, percentage]
    }),

    /**
     * Modifies the entity's color values.
     */
    COLOR({ entity, values, percentage ->
        when (values.spanCount) {
            1 -> entity.setColor(values[0, percentage], values[0, percentage], values[0, percentage])
            3 -> entity.setColor(values[0, percentage], values[1, percentage], values[2, percentage])

            else -> throw IllegalArgumentException("Color modifier must have 1 or 3 values.")
        }
    }),

    /**
     * Modifies the entity's position in both axis.
     */
    MOVE({ entity, values, percentage ->
        if (values.spanCount == 1) {
            entity.setPosition(values[0, percentage], values[0, percentage])
        } else {
            entity.setPosition(values[0, percentage], values[1, percentage])
        }
    }),

    /**
     * Modifies the entity's X position.
     */
    MOVE_X({ entity, values, percentage ->
        entity.setPosition(values[0, percentage], entity.y)
    }),

    /**
     * Modifies the entity's Y position.
     */
    MOVE_Y({ entity, values, percentage ->
        entity.setPosition(entity.x, values[0, percentage])
    }),

    /**
     * Modifies the entity's translation in both axis.
     *
     * Note: This is only available for [ExtendedEntity] instances.
     */
    TRANSLATE({ entity, values, percentage ->
        if (entity is ExtendedEntity) {
            if (values.spanCount == 1) {
                entity.translationX = values[0, percentage]
                entity.translationY = values[0, percentage]
            } else {
                entity.translationX = values[0, percentage]
                entity.translationY = values[1, percentage]
            }
        }
    }),

    /**
     * Modifies the entity's X translation.
     *
     * Note: This is only available for [ExtendedEntity] instances.
     */
    TRANSLATE_X({ entity, values, percentage ->
        if (entity is ExtendedEntity) {
            entity.translationX = values[0, percentage]
        }
    }),

    /**
     * Modifies the entity's Y translation.
     *
     * Note: This is only available for [ExtendedEntity] instances.
     */
    TRANSLATE_Y({ entity, values, percentage ->
        if (entity is ExtendedEntity) {
            entity.translationY = values[0, percentage]
        }
    }),

    /**
     * Modifies the entity's rotation.
     */
    ROTATION({ entity, values, percentage ->
        entity.rotation = values[0, percentage]
    }),

    /**
     * Modifies the entity's with inner modifiers in sequence.
     */
    SEQUENCE,

    /**
     * Modifies the entity's with inner modifiers in parallel.
     */
    PARALLEL,

    /**
     * Does nothing, used as a delay modifier.
     */
    NONE

}