package com.reco1l.andengine.modifier

import com.reco1l.andengine.*
import org.anddev.andengine.entity.IEntity


/**
 * The type of the modifier.
 * @see Modifiers
 */
enum class ModifierType(val onApply: ((entity: IEntity, values: SpanArray, percentage: Float) -> Unit)? = null) {

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
        entity.scaleX = values[0, percentage]
        entity.scaleY = values[1, percentage]
    }),

    /**
     * Modifies the entity's color values.
     */
    RGB({ entity, values, percentage ->
        entity.setColor(
            values[0, percentage],
            values[1, percentage],
            values[2, percentage]
        )
    }),

    /**
     * Modifies the entity's position in both axis.
     */
    MOVE({ entity, values, percentage ->
        entity.setPosition(
            values[0, percentage],
            values[1, percentage]
        )
    }),

    /**
     * Modifies the entity's translation in both axis.
     *
     * Note: This is only available for [ExtendedEntity] instances.
     */
    TRANSLATE({ entity, values, percentage ->
        if (entity is ExtendedEntity) {
            entity.translationX = values[0, percentage]
            entity.translationY = values[1, percentage]
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