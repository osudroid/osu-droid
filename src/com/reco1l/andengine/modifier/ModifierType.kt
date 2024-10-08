package com.reco1l.andengine.modifier

import android.graphics.Color.*
import com.reco1l.andengine.*
import com.reco1l.framework.*
import org.anddev.andengine.entity.*


/**
 * The type of the modifier.
 */
enum class ModifierType(val getCurrentValue: IEntity.() -> Float = { 0f }, val setValue: IEntity.(Float) -> Unit = {}) {

    /**
     * Modifies the entity's X scale value.
     */
    ScaleX(IEntity::getScaleX, IEntity::setScaleX),

    /**
     * Modifies the entity's Y scale value.
     */
    ScaleY(IEntity::getScaleY, IEntity::setScaleY),

    /**
     * Modifies the entity's alpha value.
     */
    Alpha(IEntity::getAlpha, IEntity::setAlpha),

    /**
     * Modifies the entity's red color value.
     */
    ColorRed(IEntity::getRed, { setColor(it, green, blue) }),

    /**
     * Modifies the entity's green color value.
     */
    ColorGreen(IEntity::getGreen, { setColor(red, it, blue) }),

    /**
     * Modifies the entity's blue color value.
     */
    ColorBlue(IEntity::getBlue, { setColor(red, green, it) }),

    /**
     * Modifies the entity's color value.
     */
    ColorRGB({ Colors.toPackedFloat(red, green, blue) }, {
        val hex = it.toRawBits()
        setColor(red(hex) / 255f, green(hex) / 255f, blue(hex) / 255f)
    }),

    /**
     * Modifies the entity's X position.
     */
    MoveX(IEntity::getX, { setPosition(it, y) }),

    /**
     * Modifies the entity's Y position.
     */
    MoveY(IEntity::getY, { setPosition(x, it) }),

    /**
     * Modifies the entity's X translation.
     *
     * Note: This is only available for [ExtendedEntity] instances.
     */
    TranslateX({ (this as ExtendedEntity).translationX }, { (this as ExtendedEntity).translationX = it }),

    /**
     * Modifies the entity's Y translation.
     *
     * Note: This is only available for [ExtendedEntity] instances.
     */
    TranslateY({ (this as ExtendedEntity).translationY }, { (this as ExtendedEntity).translationY = it }),

    /**
     * Modifies the entity's rotation.
     */
    Rotation(IEntity::getRotation, IEntity::setRotation),

    /**
     * Modifies the entity's with inner modifiers in sequence.
     */
    Sequence,

    /**
     * Modifies the entity's with inner modifiers in parallel.
     */
    Parallel,

    /**
     * Does nothing, used as a delay modifier.
     */
    Delay;


    /**
     * Whether this modifier type uses nested modifiers.
     */
    val usesNestedModifiers
        get() = this == Sequence || this == Parallel

}