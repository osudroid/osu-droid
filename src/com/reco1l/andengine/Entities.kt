package com.reco1l.andengine

import com.reco1l.framework.math.Vec2
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.CameraScene
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.IShape
import ru.nsu.ccfit.zuev.osu.Config

//region Size related properties

fun IEntity?.getWidth() = when (this) {
    is ExtendedEntity -> width
    is CameraScene -> camera.widthRaw
    is IShape -> width
    is Scene -> Config.getRES_WIDTH().toFloat()
    else -> 0f
}

fun IEntity?.getHeight() = when (this) {
    is ExtendedEntity -> height
    is CameraScene -> camera.heightRaw
    is IShape -> height
    is Scene -> Config.getRES_HEIGHT().toFloat()
    else -> 0f
}


/**
 * The size of the entity.
 */
var ExtendedEntity.size: Vec2
    get() = Vec2(width, height)
    set(value) = setSize(value.x, value.y)

/**
 * The content size of the entity.
 */
val ExtendedEntity.contentSize: Vec2
    get() = Vec2(contentWidth, contentHeight)

/**
 * The size with transformations applied.
 */
val ExtendedEntity.transformedSize: Vec2
    get() = Vec2(transformedWidth, transformedHeight)

/**
 * The width with transformations applied.
 */
val ExtendedEntity.transformedWidth: Float
    get() = width * scaleX

/**
 * The height with transformations applied.
 */
val ExtendedEntity.transformedHeight: Float
    get() = height * scaleY

/**
 * The size minus padding of the entity.
 */
val ExtendedEntity.innerSize: Vec2
    get() = Vec2(innerWidth, innerHeight)

/**
 * The width minus padding of the entity.
 */
val IEntity?.innerWidth: Float
    get() = if (this is ExtendedEntity) width - padding.horizontal else getWidth()

/**
 * The height minus padding of the entity.
 */
val IEntity?.innerHeight: Float
    get() = if (this is ExtendedEntity) height - padding.vertical else getHeight()

//endregion

//region Position related properties

/**
 * The absolute position of the entity in the parent coordinate system.
 * This takes into account the anchor and origin but not transformations.
 */
val ExtendedEntity.absolutePosition: Vec2
    get() = Vec2(absoluteX, absoluteY)

/**
 * The absolute position of the X axis of the entity in the parent coordinate system.
 * This takes into account the anchor and origin.
 */
val IEntity.absoluteX: Float
    get() = if (this is ExtendedEntity) anchorPositionX - originPositionX + x else x

/**
 * The absolute position of the Y axis of the entity in the parent coordinate system.
 * This takes into account the anchor and origin but not transformations.
 */
val IEntity.absoluteY: Float
    get() = if (this is ExtendedEntity) anchorPositionY - originPositionY + y else y

/**
 * The position of the entity. This does not take into account the anchor and origin.
 */
var ExtendedEntity.position
    get() = Vec2(x, y)
    set(value) = setPosition(value.x, value.y)

/**
 * The anchor position of the entity.
 */
val ExtendedEntity.anchorPosition: Vec2
    get() = Vec2(anchorPositionX, anchorPositionY)

/**
 * The anchor position of the entity in the X axis.
 */
val ExtendedEntity.anchorPositionX: Float
    get() = parent.innerWidth * anchor.x

/**
 * The anchor position of the entity in the Y axis.
 */
val ExtendedEntity.anchorPositionY: Float
    get() = parent.innerHeight * anchor.y


/**
 * The origin position of the entity.
 */
val ExtendedEntity.originPosition: Vec2
    get() = Vec2(originPositionX, originPositionY)

/**
 * The origin position of the entity in the X axis.
 */
val ExtendedEntity.originPositionX: Float
    get() = width * origin.x

/**
 * The origin position of the entity in the Y axis.
 */
val ExtendedEntity.originPositionY: Float
    get() = height * origin.y

//endregion

//region Transformation properties

/**
 * The scale of the entity.
 */
var IEntity.scale
    get() = Vec2(scaleX, scaleY)
    set(value) = setScale(value.x, value.y)

/**
 * The center where the entity will scale from.
 */
var IEntity.scaleCenter
    get() = Vec2(scaleCenterX, scaleCenterY)
    set(value) = setScaleCenter(value.x, value.y)

/**
 * The center where the entity will rotate from.
 */
var IEntity.rotationCenter
    get() = Vec2(rotationCenterX, rotationCenterY)
    set(value) = setRotationCenter(value.x, value.y)

//endregion

