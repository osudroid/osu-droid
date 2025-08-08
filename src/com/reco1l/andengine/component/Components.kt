package com.reco1l.andengine.component

import androidx.annotation.*
import com.reco1l.andengine.component.AttachmentMode.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.Vec2
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.CameraScene
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.IShape
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.StringTable

//region Size related properties

fun IEntity?.getWidth() = when (this) {
    is UIComponent -> width
    is CameraScene -> camera?.widthRaw ?: 0f
    is IShape -> width
    is Scene -> Config.getRES_WIDTH().toFloat()
    else -> 0f
}

fun IEntity?.getHeight() = when (this) {
    is UIComponent -> height
    is CameraScene -> camera?.heightRaw ?: 0f
    is IShape -> height
    is Scene -> Config.getRES_HEIGHT().toFloat()
    else -> 0f
}


/**
 * The size of the entity.
 */
var UIComponent.size: Vec2
    get() = Vec2(width, height)
    set(value) = setSize(value.x, value.y)

/**
 * The content size of the entity.
 */
val UIComponent.contentSize: Vec2
    get() = Vec2(contentWidth, contentHeight)

/**
 * The position of the content in the x-axis of the entity.
 */
val IEntity?.contentX: Float
    get() = if (this is UIComponent) padding.left else 0f

/**
 * The position of the content in the y-axis of the entity.
 */
val IEntity?.contentY: Float
    get() = if (this is UIComponent) padding.top else 0f

/**
 * The size with transformations applied.
 */
val UIComponent.transformedSize: Vec2
    get() = Vec2(transformedWidth, transformedHeight)

/**
 * The width with transformations applied.
 */
val UIComponent.transformedWidth: Float
    get() = width * scaleX

/**
 * The height with transformations applied.
 */
val UIComponent.transformedHeight: Float
    get() = height * scaleY

/**
 * The size minus padding of the entity.
 */
val UIComponent.innerSize: Vec2
    get() = Vec2(innerWidth, innerHeight)

/**
 * The width minus padding of the entity.
 */
val IEntity?.innerWidth: Float
    get() = if (this is UIComponent) width - padding.horizontal else getWidth()

/**
 * The height minus padding of the entity.
 */
val IEntity?.innerHeight: Float
    get() = if (this is UIComponent) height - padding.vertical else getHeight()

//endregion

//region Position related properties

/**
 * The absolute position of the entity in the parent coordinate system.
 * This takes into account the anchor and origin but not transformations.
 */
val UIComponent.absolutePosition: Vec2
    get() = Vec2(absoluteX, absoluteY)

/**
 * The absolute position of the X axis of the entity in the parent coordinate system.
 * This takes into account the anchor and origin.
 */
val IEntity.absoluteX: Float
    get() = if (this is UIComponent) (if (attachmentMode == Child) parent.contentX else 0f) + anchorPositionX - originPositionX + x + translationX else x

/**
 * The absolute position of the Y axis of the entity in the parent coordinate system.
 * This takes into account the anchor and origin but not transformations.
 */
val IEntity.absoluteY: Float
    get() = if (this is UIComponent) (if (attachmentMode == Child) parent.contentY else 0f) + anchorPositionY - originPositionY + y + translationY else y

/**
 * The position of the entity. This does not take into account the anchor and origin.
 */
var UIComponent.position
    get() = Vec2(x, y)
    set(value) = setPosition(value.x, value.y)

/**
 * The anchor position of the entity.
 */
val UIComponent.anchorPosition: Vec2
    get() = Vec2(anchorPositionX, anchorPositionY)

/**
 * The anchor position of the entity in the X axis.
 */
val UIComponent.anchorPositionX: Float
    get() = parent.innerWidth * anchor.x

/**
 * The anchor position of the entity in the Y axis.
 */
val UIComponent.anchorPositionY: Float
    get() = parent.innerHeight * anchor.y


/**
 * The origin position of the entity.
 */
val UIComponent.originPosition: Vec2
    get() = Vec2(originPositionX, originPositionY)

/**
 * The origin position of the entity in the X axis.
 */
val UIComponent.originPositionX: Float
    get() = width * origin.x

/**
 * The origin position of the entity in the Y axis.
 */
val UIComponent.originPositionY: Float
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

var IEntity.color4
    get() = Color4(red, green, blue, alpha)
    set(value) {
        setColor(value.red, value.green, value.blue, value.alpha)
    }

//endregion


//region Utilities

/**
 * Traverses the parent hierarchy of the entity to find the first scene it belongs to.
 */
fun IEntity.getParentScene(): Scene? {
    return if (this is Scene) this else parent?.getParentScene()
}

inline fun IEntity.forEach(block: (IEntity) -> Unit) {
    for (i in 0 until childCount) {
        block(getChild(i))
    }
}

inline fun IEntity.forEachIndexed(block: (Int, IEntity) -> Unit) {
    for (i in 0 until childCount) {
        block(i, getChild(i))
    }
}

fun UIText.setText(@StringRes resourceId: Int) {
    text = StringTable.get(resourceId)
}

fun UITextButton.setText(@StringRes resourceId: Int) {
    text = StringTable.get(resourceId)
}

fun UIBadge.setText(@StringRes resourceId: Int) {
    text = StringTable.get(resourceId)
}

//endregion