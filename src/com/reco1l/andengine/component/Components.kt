package com.reco1l.andengine.component

import androidx.annotation.*
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.Vec2
import com.reco1l.framework.math.Vec4
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.CameraScene
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.IShape
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.StringTable

//region Size related properties

/**
 * @see UIComponent.width
 */
val IEntity.width
    get() = when (this) {
        is UIComponent -> width
        is CameraScene -> camera?.widthRaw ?: 0f
        is IShape -> width
        is Scene -> Config.getRES_WIDTH().toFloat()
        else -> 0f
    }

/**
 * @see UIComponent.height
 */
val IEntity.height
    get() = when (this) {
        is UIComponent -> height
        is CameraScene -> camera?.heightRaw ?: 0f
        is IShape -> height
        is Scene -> Config.getRES_HEIGHT().toFloat()
        else -> 0f
    }


/**
 * Wraps [width] and [height] into a [Vec2].
 */
var UIComponent.size: Vec2
    get() = Vec2(width, height)
    set(value) = setSize(value.x, value.y)

/**
 * @see UIComponent.padding
 */
val IEntity?.padding: Vec4
    get() = if (this is UIComponent) padding else Vec4.Zero

/**
 * The width minus padding of the entity.
 */
val IEntity.innerWidth: Float
    get() = if (this is UIComponent) innerWidth else width - padding.horizontal

/**
 * The height minus padding of the entity.
 */
val IEntity.innerHeight: Float
    get() = if (this is UIComponent) innerHeight else height - padding.vertical

//endregion

//region Position related properties

/**
 * Wraps [absoluteX] and [absoluteY] into a [Vec2].
 */
val IEntity.absolutePosition: Vec2
    get() = Vec2(absoluteX, absoluteY)

/** @see UIComponent.absoluteX */
val IEntity.absoluteX: Float
    get() = if (this is UIComponent) absoluteX else x

/**
 * @see UIComponent.absoluteY
 */
val IEntity.absoluteY: Float
    get() = if (this is UIComponent) absoluteY else y

/**
 * Wraps [IEntity.getX] and [IEntity.getY] into a [Vec2].
 */
var IEntity.position
    get() = Vec2(x, y)
    set(value) = setPosition(value.x, value.y)

/**
 * The anchor position of the entity.
 */
val UIComponent.anchorPosition: Vec2
    get() = Vec2(anchorPositionX, anchorPositionY)


//endregion

//region Padding

var UIComponent.paddingLeft
    get() = padding.left
    set(value) {
        if (padding.left != value) {
            padding = padding.copy(x = value)
        }
    }

var UIComponent.paddingTop
    get() = padding.top
    set(value) {
        if (padding.top != value) {
            padding = padding.copy(y = value)
        }
    }

var UIComponent.paddingRight
    get() = padding.right
    set(value) {
        if (padding.right != value) {
            padding = padding.copy(z = value)
        }
    }

var UIComponent.paddingBottom
    get() = padding.bottom
    set(value) {
        if (padding.bottom != value) {
            padding = padding.copy(w = value)
        }
    }

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

inline fun IEntity.forEach(block: (IEntity) -> Unit) {
    for (i in 0 until childCount) {
        block(getChild(i))
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