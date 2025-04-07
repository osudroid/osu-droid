package com.reco1l.andengine

import android.util.*
import com.reco1l.andengine.modifier.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.entity.scene.*
import org.anddev.andengine.entity.scene.Scene.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.util.*
import javax.microedition.khronos.opengles.*


/**
 * Entity with extended features.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class ExtendedEntity : Entity(0f, 0f), ITouchArea, IModifierChain {

    //region Axes properties

    /**
     * Determines which axes for the size of the entity are relative w.r.t the parent.
     *
     * * If the value is [Axes.None], the unit for both [width] and [height] will be absolute.
     * * If the value is [Axes.X], the unit for [width] will be relative meanwhile [height] will remain as absolute.
     * * If the value is [Axes.Y], the unit for [height] will be relative meanwhile [width] will remain as absolute.
     * * If the value is [Axes.Both], both [width] and [height] will be relative.
     *
     * Relative values are calculated as a percentage of the parent's size minus its padding, that is, values passed
     * to [width] or [height] will be treated as a percentage (values from 0 to 1).
     */
    open var relativeSizeAxes = Axes.None

    /**
     * Determines which axes for the position of the entity are relative w.r.t the parent.
     *
     * * If the value is [Axes.None], the unit for both [x][setX] and [y][setY] will be absolute.
     * * If the value is [Axes.X], the unit for [x][setX] will be relative meanwhile [y][setY] will remain as absolute.
     * * If the value is [Axes.Y], the unit for [y][setY] will be relative meanwhile [x][setX] will remain as absolute.
     * * If the value is [Axes.Both], both [x][setX] and [y][setY] will be relative.
     *
     * Relative values are calculated as a percentage of the parent's size minus its padding, that is, values passed
     * to [x][setX] or [y][setY] will be treated as a percentage (values from 0 to 1).
     */
    open var relativePositionAxes = Axes.None

    //endregion

    //region Size related properties

    /**
     * The width of the entity.
     */
    var width: Float = 0f
        get() = when (field) {
            FitContent -> contentWidth + padding.horizontal
            FitParent -> parent.innerWidth
            else -> if (relativeSizeAxes.isHorizontal) {
                field * parent.innerWidth
            } else {
                field + padding.horizontal
            }
        }
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    /**
     * The height of the entity.
     */
    var height = 0f
        get() = when (field) {
            FitContent -> contentHeight + padding.vertical
            FitParent -> parent.innerHeight
            else -> if (relativeSizeAxes.isVertical) {
                field * parent.innerHeight
            } else {
                field + padding.vertical
            }
        }
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Size)
            }
        }

    /**
     * The width of the content inside the entity.
     */
    open var contentWidth = 0f
        protected set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.ContentSize)
            }
        }

    /**
     * The height of the content inside the entity.
     */
    open var contentHeight = 0f
        protected set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.ContentSize)
            }
        }

    /**
     * The padding of the entity.
     */
    open var padding = Vec4.Zero
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.ContentSize)
            }
        }

    //endregion

    //region Position related properties

    override fun getX(): Float {
        if (relativePositionAxes.isHorizontal) {
            return mX * parent.innerWidth
        }
        return mX
    }
    fun setX(value: Float) {
        if (mX != value) {
            mX = value
            invalidate(InvalidationFlag.Position)
        }
    }

    override fun getY(): Float {
        if (relativePositionAxes.isVertical) {
            return mY * parent.innerHeight
        }
        return mY
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
    open var anchor = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Position)
            }
        }

    /**
     * Where the entity's origin should be.
     */
    open var origin = Anchor.TopLeft
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
     * The translation in the X axis.
     */
    open var translationX = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Position)
            }
        }

    /**
     * The translation in the Y axis.
     */
    open var translationY = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Position)
            }
        }

    //endregion

    //region Cosmetic properties

    /**
     * The background entity. This entity will be drawn before the entity children and will not be
     * affected by padding.
     */
    open var background: ExtendedEntity? = null
        set(value) {
            if (field != value) {
                if (value?.parent != null) {
                    Log.e("ExtendedEntity", "The background entity is already attached to another entity.")
                    return
                }
                field = value
            }
        }

    /**
     * The foreground entity. This entity will be drawn after the entity children and will not be
     * affected by padding.
     */
    open var foreground: ExtendedEntity? = null
        set(value) {
            if (field != value) {
                if (value?.parent != null) {
                    Log.e("ExtendedEntity", "The foreground entity is already attached to another entity.")
                    return
                }
                field = value
            }
        }

    /**
     * The color of the entity boxed in a [ColorARGB] object.
     */
    open var color: ColorARGB
        get() = ColorARGB(mRed, mGreen, mBlue, mAlpha)
        set(value) {
            mRed = value.red
            mGreen = value.green
            mBlue = value.blue
            mAlpha = value.alpha
        }

    /**
     * Whether the entity should clip its children.
     */
    open var clipChildren = false

    //endregion

    //region State properties

    /**
     * The current invalidation flags. Indicates which properties were updated and need to be handled.
     *
     * @see InvalidationFlag
     */
    protected var invalidationFlags = InvalidationFlag.Position or InvalidationFlag.Size

    /**
     * The input bindings of the entity. This is used to handle touch events.
     */
    protected val inputBindings = arrayOfNulls<ExtendedEntity>(10)

    //endregion

    //region Invalidation

    /**
     * Adds the given flag to the invalidation list. Depending on each flag it will trigger a different action.
     *
     * @see InvalidationFlag
     */
    fun invalidate(flag: Int) {
        invalidationFlags = invalidationFlags or flag

        var recursiveInvalidationFlags = 0

        if (flag and InvalidationFlag.Position != 0
            || flag and InvalidationFlag.Size != 0
            || flag and InvalidationFlag.ContentSize != 0) {

            invalidationFlags = invalidationFlags or InvalidationFlag.Transformations
            recursiveInvalidationFlags = InvalidationFlag.Transformations
        }

        if (flag and InvalidationFlag.InputBindings != 0) {
            recursiveInvalidationFlags = recursiveInvalidationFlags or InvalidationFlag.InputBindings
        }

        if (recursiveInvalidationFlags != 0) {
            runSafe {
                mChildren?.fastForEach {
                    (it as? ExtendedEntity)?.invalidate(recursiveInvalidationFlags)
                }
            }
        }
    }

    //endregion

    //region Attachment

    override fun setParent(newParent: IEntity?) {
        when (val parent = parent) {
            is Scene -> parent.unregisterTouchArea(this)
            is ExtendedEntity -> parent.onChildDetached(this)
        }
        super.setParent(newParent)
        when (newParent) {
            is ExtendedScene -> newParent.registerTouchArea(this)
            is ExtendedEntity -> newParent.onChildAttached(this)
        }
    }

    /**
     * Called when a child is attached to this entity.
     */
    open fun onChildAttached(child: IEntity) {}

    /**
     * Called when a child is detached from this entity.
     */
    open fun onChildDetached(child: IEntity) {}

    //endregion

    //region Size

    /**
     * Called when the size of a child entity changes.
     */
    open fun onChildSizeChanged(child: IEntity) {}

    /**
     * Called when the size of this entity changes.
     */
    open fun onSizeChanged() {
        (parent as? ExtendedEntity)?.onChildSizeChanged(this)
    }

    /**
     * Sets the size of the entity.
     */
    open fun setSize(x: Float, y: Float) {
        width = x
        height = y
    }

    @Deprecated("Keeping this for the current usages.", ReplaceWith("transformedWidth"))
    fun getWidthScaled(): Float {
        return width * scaleX
    }

    @Deprecated("Keeping this for the current usages.", ReplaceWith("transformedHeight"))
    fun getHeightScaled(): Float {
        return height * scaleY
    }

    //endregion

    //region Position

    /**
     * Called when the position of a child entity changes.
     */
    open fun onChildPositionChanged(child: IEntity) {}

    /**
     * Called when the position of this entity changes.
     */
    open fun onPositionChanged() {
        (parent as? ExtendedEntity)?.onChildPositionChanged(this)
    }

    /**
     * Sets the position of the entity.
     */
    override fun setPosition(x: Float, y: Float) {
        setX(x)
        setY(y)
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

    open fun onApplyColor(gl: GL10) {
        var red = mRed
        var green = mGreen
        var blue = mBlue
        var alpha = mAlpha
        var parent = parent

        while (parent != null) {

            red *= parent.red
            green *= parent.green
            blue *= parent.blue
            alpha *= parent.alpha

            // We'll assume at this point there's no need to keep multiplying.
            if (red == 0f && green == 0f && blue == 0f && alpha == 0f) {
                break
            }

            parent = parent.parent
        }

        GLHelper.setColor(gl, red, green, blue, alpha)
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {

        if (mChildren == null || !mChildrenVisible) {
            return
        }

        if (clipChildren) {
            GLHelper.enableScissorTest(gl)

            val (topLeftX, topLeftY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(width, 0f))
            val (bottomRightX, bottomRightY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(width, height))
            val (bottomLeftX, bottomLeftY) = camera.getScreenSpaceCoordinates(convertLocalToSceneCoordinates(0f, height))

            val minClippingX = minOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val minClippingY = minOf(topLeftY, bottomLeftY, bottomRightY, topRightY)

            val maxClippingX = maxOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val maxClippingY = maxOf(topLeftY, bottomLeftY, bottomRightY, topRightY)

            gl.glScissor(
                minClippingX.toInt(),
                minClippingY.toInt(),
                (maxClippingX - minClippingX).toInt(),
                (maxClippingY - minClippingY).toInt()
            )
        }

        super.onDrawChildren(gl, camera)

        if (clipChildren) {
            GLHelper.disableScissorTest(gl)
        }
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        val invalidationFlags = invalidationFlags

        if (invalidationFlags != 0) {

            if (invalidationFlags and InvalidationFlag.Position != 0) {
                onPositionChanged()
            }

            if (invalidationFlags and InvalidationFlag.Size != 0 || invalidationFlags and InvalidationFlag.ContentSize != 0) {
                onSizeChanged()
            }

            if (invalidationFlags and InvalidationFlag.Transformations != 0) {
                onInvalidateTransformations()
            }

            if (invalidationFlags and InvalidationFlag.InputBindings != 0) {
                onInvalidateInputBindings()
            }

            // During the invalidation process the flags could be changed.
            if (this.invalidationFlags == invalidationFlags) {
                this.invalidationFlags = 0
            }
        }

        gl.glPushMatrix()

        onApplyTransformations(gl, camera)

        background?.setSize(width, height)
        background?.onDraw(gl, camera)

        val offsetLeft = padding.left * (1f - anchor.x)
        val offsetTop = padding.top * (1f - anchor.y)

        val hasOffset = offsetLeft > 0f || offsetTop > 0f
        if (hasOffset) {
            gl.glTranslatef(offsetLeft, offsetTop, 0f)
        }

        doDraw(gl, camera)
        onDrawChildren(gl, camera)

        if (hasOffset) {
            gl.glTranslatef(-offsetLeft, -offsetTop, 0f)
        }

        foreground?.setSize(width, height)
        foreground?.onDraw(gl, camera)

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
        foreground?.onManagedUpdate(deltaTimeSec)

        super.onManagedUpdate(deltaTimeSec)
    }

    //endregion

    //region Collision

    override fun contains(x: Float, y: Float): Boolean {

        if (width == 0f || height == 0f) {
            return false
        }

        return EntityCollisionChecker.contains(this, x, y, parent is Scene)
    }

    //endregion

    //region Transformations

    open fun onInvalidateTransformations() {
        mLocalToParentTransformationDirty = true
        localToParentTransformation

        mParentToLocalTransformationDirty = true
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

        val modifier = UniversalModifier.GlobalPool.obtain()
        modifier.setToDefault()
        modifier.block()

        registerEntityModifier(modifier)
        return modifier
    }

    //endregion

    //region Input

    /**
     * Called when input bindings are invalidated and needs to be removed.
     */
    open fun onInvalidateInputBindings() {
        inputBindings.fill(null)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        val inputBinding = inputBindings.getOrNull(event.pointerID)

        if (inputBinding != null && inputBinding.parent == this) {

            val childLocalX = localX - inputBinding.absoluteX - padding.left
            val childLocalY = localY - inputBinding.absoluteY - padding.top

            if (!inputBinding.onAreaTouched(event, childLocalX, childLocalY) || event.isActionUp) {
                inputBindings[event.pointerID] = null
                return false
            }
            return true
        } else {
            inputBindings[event.pointerID] = null
        }

        try {
            for (i in childCount - 1 downTo 0) {
                val child = getChild(i)
                if (child is ExtendedEntity && child.contains(localX, localY)) {

                    val childLocalX = localX - child.absoluteX - padding.left
                    val childLocalY = localY - child.absoluteY - padding.top

                    if (child.onAreaTouched(event, childLocalX, childLocalY)) {
                        inputBindings[event.pointerID] = child
                        return true
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("ExtendedEntity", "A child entity was removed during touch event propagation.", e)
        }

        return false
    }

    //endregion


    @Suppress("ConstPropertyName")
    companion object {

        /**
         * The width and height of the entity will be set to the content size.
         */
        const val FitContent = -1f

        /**
         * The width and height of the entity will be set to the parent's size.
         */
        const val FitParent = -2f

    }

}


