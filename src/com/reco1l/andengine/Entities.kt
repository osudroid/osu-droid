package com.reco1l.andengine

import com.reco1l.framework.math.Vec4
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.shape.IShape


fun IEntity?.getPadding() = when (this) {
    is ExtendedEntity -> padding
    else -> Vec4.Zero
}

fun IEntity?.getPaddedWidth() = when (this) {
    is ExtendedEntity -> drawWidth - padding.horizontal
    is IShape -> width
    else -> 0f
}

fun IEntity?.getPaddedHeight() = when (this) {
    is ExtendedEntity -> drawHeight - padding.vertical
    is IShape -> height
    else -> 0f
}


/**
 * The total offset applied to the X axis.
 */
val ExtendedEntity.totalOffsetX
    get() = originOffsetX + anchorOffsetX + translationX

/**
 * The total offset applied to the Y axis.
 */
val ExtendedEntity.totalOffsetY
    get() = originOffsetY + anchorOffsetY + translationY

/**
 * The offset applied to the X axis according to the anchor factor.
 */
val ExtendedEntity.anchorOffsetX: Float
    get() = parent.getPaddedWidth() * anchor.x

/**
 * The offset applied to the Y axis according to the anchor factor.
 */
val ExtendedEntity.anchorOffsetY: Float
    get() = parent.getPaddedHeight() * anchor.y

/**
 * The offset applied to the X axis according to the origin factor.
 */
val ExtendedEntity.originOffsetX: Float
    get() = -(drawWidth * origin.x)

/**
 * The offset applied to the Y axis according to the origin factor.
 */
val ExtendedEntity.originOffsetY: Float
    get() = -(drawHeight * origin.y)


/**
 * Returns the draw width of the entity.
 */
fun IEntity?.getDrawWidth(): Float = when (this) {
    is ExtendedEntity -> drawWidth
    is IShape -> width
    else -> 0f
}

/**
 * Returns the draw height of the entity.
 */
fun IEntity?.getDrawHeight(): Float = when (this) {
    is ExtendedEntity -> drawHeight
    is IShape -> height
    else -> 0f
}

/**
 * Returns the draw X position of the entity.
 */
fun IEntity?.getDrawX(): Float = when (this) {
    is ExtendedEntity -> drawX
    is IShape -> x
    else -> 0f
}

/**
 * Returns the draw Y position of the entity.
 */
fun IEntity?.getDrawY(): Float = when (this) {
    is ExtendedEntity -> drawY
    is IShape -> y
    else -> 0f
}

/**
 * Attaches the entity to a parent.
 */
infix fun <T : IEntity> T.attachTo(parent: IEntity): T {
    parent.attachChild(this)
    return this
}