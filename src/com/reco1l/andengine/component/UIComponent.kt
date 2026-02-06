package com.reco1l.andengine.component

import android.util.*
import android.view.*
import com.osudroid.*
import com.reco1l.andengine.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.ui.*
import com.rian.osu.math.Precision
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.scene.Scene.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.util.*
import org.anddev.andengine.util.constants.Constants.*
import javax.microedition.khronos.opengles.*
import kotlin.math.max
import kotlin.math.min


/**
 * Entity with extended features.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class UIComponent : Entity(0f, 0f), ITouchArea, IModifierChain {

    //region Size related properties

    /**
     * Whether the component can shrink below its intrinsic size. By default it
     * is true in order to pair default CSS's `flex-shrink` behavior.
     */
    var shrink = true

    /**
     * The minimum width of the entity.
     */
    var minWidth = 0f
        get() = if (shrink) field else max(intrinsicWidth, field)
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The maximum width of the entity.
     */
    var maxWidth = Float.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The width of the entity.
     */
    var width: Float
        get() = when (rawWidth) {
            Size.Auto -> intrinsicWidth
            in Size.relativeSizeRange -> (parent?.innerWidth ?: 0f) * (rawWidth - Size.relativeSizeRange.start)
            else -> rawWidth
        }.coerceAtMost(maxWidth).coerceAtLeast(minWidth)
        set(value) {
            if (!Precision.almostEquals(rawWidth, value)) {
                rawWidth = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * Represents the width specified without any kind of calculation or constraint applied.
     * * If the value is `-1` it means the width is set to [Size.Auto].
     * * If the value is between `-2` and `-3` it means the width is set to a percentage value.
     *
     * @see Size
     */
    var rawWidth = 0f
        private set

    /**
     * The minimum height of the entity.
     */
    var minHeight = 0f
        get() = if (shrink) field else max(intrinsicHeight, field)
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }
    /**
     * The maximum height of the entity.
     */
    var maxHeight = Float.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The height of the entity.
     */
    var height: Float
        get() = when (rawHeight) {
            Size.Auto -> intrinsicHeight
            in Size.relativeSizeRange -> (parent?.innerHeight ?: 0f) * (rawHeight - Size.relativeSizeRange.start)
            else -> rawHeight
        }.coerceAtMost(maxHeight).coerceAtLeast(minHeight)
        set(value) {
            if (!Precision.almostEquals(rawHeight, value)) {
                rawHeight = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * Represents the height specified without any kind of calculation or constraint applied.
     * * If the value is `-1` it means the height is set to [Size.Auto].
     * * If the value is between `-2` and `-3` it means the height is set to a percentage value.
     * @see Size
     */
    var rawHeight = 0f
        private set

    /**
     * The width of the content inside the entity.
     */
    var contentWidth = 0f
        protected set(value) {
            if (!Precision.almostEquals(field, value)) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The height of the content inside the entity.
     */
    var contentHeight = 0f
        protected set(value) {
            if (!Precision.almostEquals(field, value)) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }

    /**
     * The padding of the entity.
     */
    var padding = Vec4.Zero
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content or InvalidationFlag.Size)
            }
        }


    /**
     * The inner width of the component, which is the width minus the horizontal padding.
     * It can be equivalent to [contentWidth] if the component's width is set to [Size.Auto].
     */
    val innerWidth
        get() = max(0f, width - padding.horizontal)

    /**
     * The inner height of the component, which is the height minus the vertical padding.
     * It can be equivalent to [contentHeight] if the component's height is set to [Size.Auto].
     */
    val innerHeight
        get() = max(0f, height - padding.vertical)

    /**
     * The intrinsic width of the entity, which is the content width plus the horizontal padding.
     */
    val intrinsicWidth
        get() = max(contentWidth + padding.horizontal, 0f)

    /**
     * The intrinsic height of the entity, which is the content height plus the vertical padding.
     */
    val intrinsicHeight
        get() = max(contentHeight + padding.vertical, 0f)

    /**
     * The width of this component with transformations applied.
     */
    val transformedWidth
        get() = width * scaleX

    /**
     * The height of this component with transformations applied.
     */
    val transformedHeight
        get() = height * scaleY

    //endregion

    //region Position related properties

    fun setX(value: Float) {
        if (mX != value) {
            mX = value
            invalidate(InvalidationFlag.Position)
        }
    }

    fun setY(value: Float) {
        if (mY != value) {
            mY = value
            invalidate(InvalidationFlag.Position)
        }
    }

    /**
     * Where the entity should be anchored in the parent.
     */
    var anchor = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Position)
            }
        }

    /**
     * Where the entity's origin should be.
     */
    var origin = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                mRotationCenterX = value.x
                mRotationCenterY = value.y
                mScaleCenterX = value.x
                mScaleCenterY = value.y
                invalidate(InvalidationFlag.Position)
            }
        }

    /**
     * The translation in the X axis, translation does not trigger any kind of invalidation
     * nor re-layout of the parent container. It is considered as a transformation.
     */
    var translationX = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Transformations)
            }
        }

    /**
     * The translation in the Y axis, translation does not trigger any kind of invalidation
     * nor re-layout of the parent container. It is considered as a transformation.
     */
    var translationY = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Transformations)
            }
        }

    /**
     * The calculated anchor position for the X axis in the parent's coordinate system.
     * This will be always 0 if the component has no parent.
     */
    val anchorPositionX
        get() = (parent?.innerWidth ?: 0f) * anchor.x

    /**
     * The calculated anchor position for the Y axis in the parent's coordinate system.
     * This will be always 0 if the component has no parent.
     */
    val anchorPositionY
        get() = (parent?.innerHeight ?: 0f) * anchor.y

    /**
     * The calculated origin position for the X axis in the component's coordinate system.
     */
    val originPositionX
        get() = width * origin.x

    /**
     * The calculated origin position for the Y axis in the component's coordinate system.
     */
    val originPositionY
        get() = height * origin.y

    /**
     * The absolute position for the X axis of the entity taking into account the
     * anchor and origin in the parent's coordinate system.
     */
    val absoluteX
        get() = (if (attachmentMode == AttachmentMode.Child) parent.padding.left else 0f) + anchorPositionX - originPositionX + x + translationX

    /**
     * The absolute position for the Y axis of the entity taking into account the
     * anchor and origin in the parent's coordinate system.
     */
    val absoluteY
        get() = (if (attachmentMode == AttachmentMode.Child) parent.padding.top else 0f) + anchorPositionY - originPositionY + y + translationY


    //endregion

    //region Cosmetic properties

    /**
     * The style of this component.
     */
    var style: StyleApplier = {}
        set(value) {
            if (field != value) {
                field = value
                applyStyle()
            }
        }

    /**
     * Applies the current style to the component.
     */
    fun applyStyle() {
        onStyle(Theme.current)
    }

    /**
     * The color of the entity boxed in a [Color4] object.
     */
    var color: Color4
        get() = Color4(mRed, mGreen, mBlue, mAlpha)
        set(value) {
            mRed = value.red
            mGreen = value.green
            mBlue = value.blue
            mAlpha = value.alpha
        }

    /**
     * Whether the entity's color should be multiplied by the color of its ancestor entities.
     */
    var inheritAncestorsColor = true

    /**
     * Whether the entity should clip its children.
     */
    var clipToBounds = false

    /**
     * The background color of this component.
     */
    var backgroundColor
        get() = background?.color ?: Color4.Transparent
        set(value) {
            if (background?.color != value) {
                initializeBackground()
                background!!.color = value
            }
        }

    /**
     * The border color of this component.
     */
    var borderColor
        get() = border?.color ?: Color4.Transparent
        set(value) {
            if (border?.color != value) {
                initializeBorder()
                border!!.color = value
            }
        }

    /**
     * The border width of this component.
     */
    var borderWidth
        get() = border?.lineWidth ?: 0f
        set(value) {
            if (border?.lineWidth != value) {
                initializeBorder()
                border!!.lineWidth = value
            }
        }

    /**
     * The corner radius of this component.
     */
    open var radius = 0f
        set(value) {
            if (field != value) {
                field = value
                background?.radius = value
                border?.radius = value
            }
        }

    /**
     * The box used to draw the background of this component.
     */
    protected var background: UIBox? = null
        private set

    /**
     * The box used to draw the border of this component.
     */
    protected var border: UIBox? = null
        private set

    //endregion

    //region Other properties

    /**
     * Whether the entity should be culled when it is outside the parent's bounds.
     */
    var cullingMode = CullingMode.Disabled

    /**
     * The mode in which the entity is attached to its parent.
     */
    var attachmentMode = AttachmentMode.None
        private set


    private var invalidationFlags = InvalidationFlag.All

    private val inputBindings = arrayOfNulls<UIComponent>(10)

    //endregion

    //region Invalidation

    /**
     * Adds the given flag to the invalidation list. Depending on each flag it will trigger a different action.
     *
     * @see InvalidationFlag
     */
    fun invalidate(flag: Int) {
        invalidationFlags = invalidationFlags or flag
    }

    private fun initializeBackground() {
        if (background == null) {
            background = UIBox().apply {
                setParent(this@UIComponent, AttachmentMode.Decorator)
                radius = this@UIComponent.radius
                color = Color4.Transparent
            }
        }
    }

    private fun initializeBorder() {
        if (border == null) {
            border = UIBox().apply {
                setParent(this@UIComponent, AttachmentMode.Decorator)
                paintStyle = PaintStyle.Outline
                radius = this@UIComponent.radius
                color = Color4.Transparent
            }
        }
    }

    //endregion

    //region Attachment

    /**
     * Called when the content of the entity has changed. This usually is called when a child is added or removed.
     */
    open fun onContentChanged() {}


    override fun detachSelf(): Boolean {

        if (parent == null) {
            return false
        }

        if (attachmentMode == AttachmentMode.Decorator) {
            parent = null
            onDetached()
            return true
        }

        return super.detachSelf()
    }

    fun setParent(entity: IEntity?, mode: AttachmentMode?) {

        when (val parent = parent) {
            is Scene -> parent.unregisterTouchArea(this)
            is UIComponent -> parent.onChildDetached(this)
        }

        super.setParent(entity)

        attachmentMode = if (entity == null) AttachmentMode.None else mode ?: AttachmentMode.Child

        when (entity) {
            is Scene -> entity.registerTouchArea(this)
            is UIComponent -> entity.onChildAttached(this)
        }

        if (attachmentMode == AttachmentMode.Decorator) {
            // Set color-inheritance to false for decorators by default, but allowing to
            // change this after attaching if needed.
            inheritAncestorsColor = false

            if (entity == null) {
                onDetached()
            } else {
                onAttached()
            }
        }
    }

    override fun setParent(parent: IEntity?) {
        setParent(parent, null)
    }

    /**
     * Called when a child is attached to this entity.
     */
    open fun onChildAttached(child: IEntity) {
        invalidate(InvalidationFlag.Content)
    }

    /**
     * Called when a child is detached from this entity.
     */
    open fun onChildDetached(child: IEntity) {
        invalidate(InvalidationFlag.Content)
    }

    override fun onAttached() {
        applyStyle()
    }

    //endregion

    //region Size

    /**
     * Called when the size of this entity changes.
     */
    open fun onSizeChanged() {}

    /**
     * Sets the size of the entity.
     */
    fun setSize(x: Float, y: Float) {
        width = x
        height = y
    }

    //endregion

    //region Position

    /**
     * Whether the entity is outside the camera's or parent's bounds.
     */
    open fun isCulled(camera: Camera): Boolean {

        when (cullingMode) {

            CullingMode.CameraBounds -> {
                val (x1, y1) = convertLocalToSceneCoordinates(0f, 0f)
                val (x2, y2) = convertLocalToSceneCoordinates(width, height)

                return x2 < camera.minX || y2 < camera.minY || x1 > camera.maxX || y1 > camera.maxY
            }

            CullingMode.ParentBounds -> {

                val parent = parent
                if (parent !is UIComponent && parent !is UIScene) {
                    return false
                }

                val x1 = absoluteX
                val y1 = absoluteY
                val x2 = x1 + width
                val y2 = y1 + height

                return x2 < 0f || y2 < 0f || x1 > parent.width || y1 > parent.height
            }

            else -> return false
        }

    }

    /**
     * Called when the position of this entity changes.
     */
    open fun onPositionChanged() {}

    /**
     * Sets the position of the entity.
     */
    override fun setPosition(x: Float, y: Float) {
        if (mX != x || mY != y) {
            mX = x
            mY = y
            invalidate(InvalidationFlag.Position)
        }
    }

    //endregion

    //region Drawing

    override fun onApplyTransformations(gl: GL10, camera: Camera) {
        val x = absoluteX
        val y = absoluteY

        if (x != 0f || y != 0f) {
            gl.glTranslatef(x, y, 0f)
        }

        if (mRotation != 0f) {
            val centerX = width * mRotationCenterX
            val centerY = height * mRotationCenterY

            if (centerX > 0f || centerY > 0f) {
                gl.glTranslatef(centerX, centerY, 0f)
                gl.glRotatef(mRotation, 0f, 0f, 1f)
                gl.glTranslatef(-centerX, -centerY, 0f)
            } else {
                gl.glRotatef(mRotation, 0f, 0f, 1f)
            }
        }

        if (mScaleX != 1f || mScaleY != 1f) {
            val centerX = width * mScaleCenterX
            val centerY = height * mScaleCenterY

            if (centerX > 0f || centerY > 0f) {
                gl.glTranslatef(centerX, centerY, 0f)
                gl.glScalef(mScaleX, mScaleY, 1f)
                gl.glTranslatef(-centerX, -centerY, 0f)
            } else {
                gl.glScalef(mScaleX, mScaleY, 1f)
            }
        }
    }

    fun onApplyColor(gl: GL10) {

        var red = mRed
        var green = mGreen
        var blue = mBlue
        var alpha = mAlpha
        var parent = parent
        var inheritColor = inheritAncestorsColor

        while (parent != null) {

            if (inheritColor) {
                red *= parent.red
                green *= parent.green
                blue *= parent.blue
            }
            alpha *= parent.alpha

            if (red == 0f && green == 0f && blue == 0f || alpha == 0f) {
                break
            }

            if (parent is UIComponent && !parent.inheritAncestorsColor) {
                inheritColor = false
            }

            parent = parent.parent
        }

        GLHelper.setColor(gl, red, green, blue, alpha)
    }

    override fun onDraw(gl: GL10, camera: Camera) {

        val isCulled = isCulled(camera)

        if (!isVisible || isCulled) {
            return
        }

        if (clipToBounds) {
            val wasScissorTestEnabled = GLHelper.isEnableScissorTest()
            GLHelper.enableScissorTest(gl)

            // Entity coordinates in screen's space.
            val (topLeftX, topLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, 0f))
            val (bottomRightX, bottomRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, height))
            val (bottomLeftX, bottomLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, height))

            val minX = minOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val minY = minOf(topLeftY, bottomLeftY, bottomRightY, topRightY)
            val maxX = maxOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val maxY = maxOf(topLeftY, bottomLeftY, bottomRightY, topRightY)

            ScissorStack.pushScissor(minX, minY, maxX - minX, maxY - minY)
            onManagedDraw(gl, camera)
            ScissorStack.pop()

            if (!wasScissorTestEnabled) {
                GLHelper.disableScissorTest(gl)
            }
        } else {
            onManagedDraw(gl, camera)
        }
    }


    /**
     * Called when invalidations needs to be run.
     */
    open fun onHandleInvalidations() {

        val flags = invalidationFlags

        var propagateToChildrenFlags = 0
        var propagateToParentFlags = 0

        if (flags and InvalidationFlag.Content != 0) {
            onContentChanged()
            propagateToChildrenFlags = InvalidationFlag.Content
            propagateToParentFlags = InvalidationFlag.Content
        }

        if (flags and InvalidationFlag.Size != 0) {
            onSizeChanged()
            propagateToChildrenFlags = InvalidationFlag.Content
            propagateToParentFlags = InvalidationFlag.Content
        }

        if (flags and InvalidationFlag.Position != 0) {
            onPositionChanged()
            propagateToParentFlags = InvalidationFlag.Content
        }

        // Transformations have and special case since they are affected by position and size changes as well
        // but not always, as an example scale and rotation do not trigger Position or Size flags but they're
        // still transformations.
        if (flags and InvalidationFlag.Transformations != 0 || flags and InvalidationFlag.Position != 0 || flags and InvalidationFlag.Size != 0) {
            onInvalidateTransformations()
            propagateToChildrenFlags = propagateToChildrenFlags or InvalidationFlag.Transformations
        }

        if (flags and InvalidationFlag.InputBindings != 0) {
            onInvalidateInputBindings()
            propagateToChildrenFlags = propagateToChildrenFlags or InvalidationFlag.InputBindings
        }

        if (propagateToParentFlags != 0) {
            val parent = parent
            if (parent is UIComponent) {
                parent.invalidate(propagateToParentFlags)
            }
        }

        if (propagateToChildrenFlags != 0) {
            forEach { child ->
                if (child is UIComponent) {
                    child.invalidate(propagateToChildrenFlags)
                }
            }
        }

        if (invalidationFlags == flags) {
            invalidationFlags = 0
        }
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {
        onHandleInvalidations()

        gl.glPushMatrix()
        onApplyTransformations(gl, camera)

        background?.setSize(width, height)
        background?.onHandleInvalidations()
        background?.onDraw(gl, camera)

        doDraw(gl, camera)
        onDrawChildren(gl, camera)

        border?.setSize(width, height)
        border?.onHandleInvalidations()
        border?.onDraw(gl, camera)

        if (BuildSettings.SHOW_ENTITY_BOUNDARIES && DEBUG_FOREGROUND != this && attachmentMode != AttachmentMode.Decorator) {
            DEBUG_FOREGROUND.setSize(width, height)
            DEBUG_FOREGROUND.onHandleInvalidations()
            DEBUG_FOREGROUND.onDraw(gl, camera)
        }

        gl.glPopMatrix()
    }

    open fun beginDraw(gl: GL10) {
        // We haven't done any culling implementation so we disable it globally for all buffered entities.
        GLHelper.disableCulling(gl)
        GLHelper.disableTextures(gl)
        GLHelper.disableTexCoordArray(gl)
        onApplyColor(gl)
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        beginDraw(gl)
    }

    //endregion

    //region Update

    override fun onManagedUpdate(deltaTimeSec: Float) {
        background?.onManagedUpdate(deltaTimeSec)
        border?.onManagedUpdate(deltaTimeSec)

        super.onManagedUpdate(deltaTimeSec)
    }

    //endregion

    //region Collision

    override fun contains(x: Float, y: Float): Boolean {

        if (width == 0f || height == 0f) {
            return false
        }

        VERTICES_WRAPPER[0 + VERTEX_INDEX_X] = 0f
        VERTICES_WRAPPER[0 + VERTEX_INDEX_Y] = 0f

        VERTICES_WRAPPER[2 + VERTEX_INDEX_X] = width
        VERTICES_WRAPPER[2 + VERTEX_INDEX_Y] = 0f

        VERTICES_WRAPPER[4 + VERTEX_INDEX_X] = width
        VERTICES_WRAPPER[4 + VERTEX_INDEX_Y] = height

        VERTICES_WRAPPER[6 + VERTEX_INDEX_X] = 0f
        VERTICES_WRAPPER[6 + VERTEX_INDEX_Y] = height

        if (parent is Scene) {
            localToSceneTransformation.transform(VERTICES_WRAPPER)
        } else {
            localToParentTransformation.transform(VERTICES_WRAPPER)
        }

        return ShapeCollisionChecker.checkContains(VERTICES_WRAPPER, VERTICES_WRAPPER.size, x, y)
    }

    //endregion

    //region Transformations

    open fun onInvalidateTransformations() {
        mLocalToParentTransformationDirty = true
        mParentToLocalTransformationDirty = true

        // This recreates and calculates the transformation matrices.
        localToParentTransformation
        parentToLocalTransformation
    }


    override fun setRotation(pRotation: Float) {
        if (mRotation != pRotation) {
            mRotation = pRotation
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setRotationCenterX(pRotationCenterX: Float) = setRotationCenter(pRotationCenterX, mRotationCenterY)
    override fun setRotationCenterY(pRotationCenterY: Float) = setRotationCenter(mRotationCenterX, pRotationCenterY)
    override fun setRotationCenter(pRotationCenterX: Float, pRotationCenterY: Float) {
        if (mRotationCenterX != pRotationCenterX || mRotationCenterY != pRotationCenterY) {
            mRotationCenterX = pRotationCenterX
            mRotationCenterY = pRotationCenterY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setScaleCenterX(pScaleCenterX: Float) = setScaleCenter(pScaleCenterX, mScaleCenterY)
    override fun setScaleCenterY(pScaleCenterY: Float) = setScaleCenter(mScaleCenterX, pScaleCenterY)
    override fun setScaleCenter(pScaleCenterX: Float, pScaleCenterY: Float) {
        if (mScaleCenterX != pScaleCenterX || mScaleCenterY != pScaleCenterY) {
            mScaleCenterX = pScaleCenterX
            mScaleCenterY = pScaleCenterY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun setScaleX(pScaleX: Float) = setScale(pScaleX, mScaleY)
    override fun setScaleY(pScaleY: Float) = setScale(mScaleX, pScaleY)
    override fun setScale(pScale: Float) = setScale(pScale, pScale)
    override fun setScale(pScaleX: Float, pScaleY: Float) {
        if (mScaleX != pScaleX || mScaleY != pScaleY) {
            mScaleX = pScaleX
            mScaleY = pScaleY
            invalidate(InvalidationFlag.Transformations)
        }
    }

    override fun getLocalToParentTransformation(): Transformation {

        if (mLocalToParentTransformation == null) {
            mLocalToParentTransformation = Transformation()
        }

        if (mLocalToParentTransformationDirty) {
            mLocalToParentTransformation.setToIdentity()

            if (mScaleX != 1f || mScaleY != 1f) {
                val centerX = width * mScaleCenterX
                val centerY = height * mScaleCenterY

                mLocalToParentTransformation.postTranslate(-centerX, -centerY)
                mLocalToParentTransformation.postScale(mScaleX, mScaleY)
                mLocalToParentTransformation.postTranslate(centerX, centerY)
            }

            if (rotation != 0f) {
                val centerX = width * mRotationCenterX
                val centerY = height * mRotationCenterY

                mLocalToParentTransformation.postTranslate(-centerX, -centerY)
                mLocalToParentTransformation.postRotate(mRotation)
                mLocalToParentTransformation.postTranslate(centerX, centerY)
            }

            mLocalToParentTransformation.postTranslate(absoluteX, absoluteY)
            mLocalToParentTransformationDirty = false
        }

        return mLocalToParentTransformation
    }

    override fun getParentToLocalTransformation(): Transformation {

        if (mParentToLocalTransformation == null) {
            mParentToLocalTransformation = Transformation()
        }

        if (mParentToLocalTransformationDirty) {
            mParentToLocalTransformation.setToIdentity()
            mParentToLocalTransformation.postTranslate(-absoluteX, -absoluteY)

            if (mRotation != 0f) {
                val centerX = width * mRotationCenterX
                val centerY = height * mRotationCenterY

                mParentToLocalTransformation.postTranslate(-centerX, -centerY)
                mParentToLocalTransformation.postRotate(-mRotation)
                mParentToLocalTransformation.postTranslate(centerX, centerY)
            }

            if (mScaleX != 1f || mScaleY != 1f) {
                val centerX = width * mScaleCenterX
                val centerY = height * mScaleCenterY

                mParentToLocalTransformation.postTranslate(-centerX, -centerY)
                mParentToLocalTransformation.postScale(1 / mScaleX, 1 / mScaleY)
                mParentToLocalTransformation.postTranslate(centerX, centerY)
            }

            mParentToLocalTransformationDirty = false
        }

        return mParentToLocalTransformation
    }

    //endregion

    //region Modifiers

    fun clearModifiers(vararg type: ModifierType) {
        unregisterEntityModifiers { it is UniversalModifier && it.type in type }
    }

    override fun appendModifier(block: UniversalModifier.() -> Unit): UniversalModifier {

        val modifier = UniversalModifier.GlobalPool.acquire() ?: UniversalModifier()
        modifier.setToDefault()
        modifier.parent = this
        modifier.block()

        registerEntityModifier(modifier)
        return modifier
    }

    //endregion

    //region Input

    /**
     * Propagates a touch event to the entity.
     */
    protected fun propagateTouchEvent(action: Int, pointerIndex: Int = 0, localX: Float = 0f, localY: Float = 0f): Boolean {

        val motionEvent = MotionEvent.obtain(0, 0, action, 0f, 0f, 0)
        val touchEvent = TouchEvent.obtain(0f, 0f, action, pointerIndex, motionEvent)

        val result = onAreaTouched(touchEvent, localX, localY)

        touchEvent.recycle()
        motionEvent.recycle()
        return result
    }

    /**
     * Called when input bindings are invalidated and needs to be removed.
     */
    open fun onInvalidateInputBindings() {
        inputBindings.fastForEachIndexed { index, binding ->
            if (binding != null) {
                propagateTouchEvent(MotionEvent.ACTION_CANCEL, index)
            }
        }
        inputBindings.fill(null)
    }

    /**
     * Called when a key is pressed.
     */
    open fun onKeyPress(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        val inputBinding = inputBindings.getOrNull(event.pointerID)

        if (inputBinding is IEntity && inputBinding.parent == this) {
            inputBinding.onAreaTouched(event, localX - inputBinding.absoluteX, localY - inputBinding.absoluteY)

            if (event.isActionUp) {
                inputBindings[event.pointerID] = null
            }
            return true
        } else {
            inputBindings[event.pointerID] = null
        }

        try {
            for (i in childCount - 1 downTo 0) {
                val child = getChild(i)
                if (child is UIComponent && child.contains(localX, localY)) {
                    if (child.onAreaTouched(event, localX - child.absoluteX, localY - child.absoluteY)) {
                        inputBindings[event.pointerID] = child
                        return true
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("UIComponent", "A child entity was removed during touch event propagation.", e)
        }

        return false
    }

    //endregion

    //region Cosmetic functions

    open fun onStyle(theme: Theme) {
        background?.onStyle(theme)
        border?.onStyle(theme)

        style(theme)
    }

    //endregion


    companion object {

        private val DEBUG_FOREGROUND by lazy {
            UIBox().apply {
                paintStyle = PaintStyle.Outline
                color = Color4.White
                lineWidth = 2f
            }
        }

        private val VERTICES_WRAPPER = FloatArray(8)
    }

}


